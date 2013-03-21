(ns nightweb.db
  (:use [clojure.java.jdbc :only [with-connection
                                  create-table
                                  drop-table
                                  update-or-insert-values
                                  update-values
                                  with-query-results
                                  delete-rows
                                  do-commands]]
        [nightweb.formats :only [base32-decode
                                 long-decode
                                 b-encode
                                 b-decode
                                 b-decode-bytes
                                 b-decode-string
                                 b-decode-long
                                 b-decode-list
                                 b-decode-byte-list]]
        [nightweb.constants :only [db-file my-hash-bytes]]))

(def spec nil)
(def limit 25)

(defmacro paginate
  [page & body]
  `(format (str ~@body " LIMIT %d OFFSET %d")
           (+ ~limit 1)
           (* ~limit (if ~page (- ~page 1) 0))))

(defn prepare-results
  [rs table]
  (->> (for [row rs]
         (into {} (assoc row :type table)))
       (doall)
       (vec)))

; initialization

(defn check-table
  ([table-name] (check-table table-name "*"))
  ([table-name column-name]
   (try
     (with-connection
       spec
       (with-query-results
         rs
         [(str "SELECT COUNT(" (name column-name) ") FROM " (name table-name))]
         rs))
     (catch java.lang.Exception e nil))))

(defn create-index
  [table-name columns]
  (do-commands
    "CREATE ALIAS IF NOT EXISTS FT_INIT FOR \"org.h2.fulltext.FullText.init\";"
    "CALL FT_INIT();"
    (format "CALL FT_CREATE_INDEX('PUBLIC', '%s', '%s');"
            table-name (clojure.string/join "," columns))))

(defn create-generic-table
  [table-name]
  (create-table
    table-name
    [:id "BIGINT" "PRIMARY KEY AUTO_INCREMENT"]
    [:realuserhash "BINARY"]
    [:userhash "BINARY"]
    [:title "VARCHAR"]
    [:body "VARCHAR"]
    [:time "BIGINT"]
    [:mtime "BIGINT"]
    [:count "BIGINT"]
    [:pichash "BINARY"]
    [:ptrhash "BINARY"]
    [:ptrtime "BIGINT"]
    [:status "BIGINT"]))

(defn create-tables
  []
  (when-not (check-table :user)
    (with-connection
      spec
      (create-generic-table :user)
      (create-index "USER" ["ID" "TITLE" "BODY"])))
  (when-not (check-table :post)
    (with-connection
      spec
      (create-generic-table :post)
      (create-index "POST" ["ID" "TITLE" "BODY"])))
  (when-not (check-table :pic)
    (with-connection
      spec
      (create-generic-table :pic)))
  (when-not (check-table :fav)
    (with-connection
      spec
      (create-generic-table :fav))))

(defn drop-tables
  []
  (try
    (with-connection
      spec
      (drop-table :user)
      (drop-table :post)
      (drop-table :pic)
      (drop-table :fav))
    (catch java.lang.Exception e (println "Tables don't exist"))))

(defn init-db
  [base-dir]
  (when (nil? spec)
    (def spec
      {:classname "org.h2.Driver"
       :subprotocol "h2"
       :subname (str base-dir db-file)})
    ;(drop-tables)
    (create-tables)))

; retrieval

(defn get-user-data
  [params]
  (let [user-hash (get params :userhash)]
    (with-connection
      spec
      (with-query-results
        rs
        ["SELECT * FROM user WHERE userhash = ?" user-hash]
        (if-let [user (first (prepare-results rs :user))]
          (dissoc user :time)
          {:userhash user-hash :type :user})))))

(defn get-single-post-data
  [params]
  (let [user-hash (get params :userhash)
        create-time (get params :time)]
    (with-connection
      spec
      (with-query-results
        rs
        ["SELECT * FROM post WHERE userhash = ? AND time = ? AND status = 1"
         user-hash create-time]
        (if-let [post (first (prepare-results rs :post))]
          post
          {:userhash user-hash :time create-time :type :post})))))

(defn get-post-data
  [params]
  (let [user-hash (get params :userhash)
        page (get params :page)]
    (with-connection
      spec
      (with-query-results
        rs
        [(paginate page
                   "SELECT * FROM post "
                   "WHERE userhash = ? AND status = 1 ORDER BY time DESC")
         user-hash]
        (prepare-results rs :post)))))

(defn get-single-fav-data
  [params]
  (let [ptr-hash (get params :userhash)
        ptr-time (get params :time)]
    (with-connection
      spec
      (with-query-results
        rs
        ["SELECT * FROM fav WHERE userhash = ? AND ptrhash = ? AND ptrtime IS ?"
         my-hash-bytes ptr-hash ptr-time]
        (first (prepare-results rs :fav))))))

(defn get-fav-data
  [params]
  (let [ptr-hash (get params :ptrhash)]
    (with-connection
      spec
      (with-query-results
        rs
        ["SELECT * FROM fav WHERE ptrhash = ? 
         AND status = 1 
         AND (userhash IN (SELECT ptrhash FROM fav WHERE userhash = ?) 
         OR userhash = ?)"
         ptr-hash my-hash-bytes my-hash-bytes]
        (prepare-results rs :fav)))))

(defn get-category-data
  [params]
  (let [data-type (get params :type)
        sub-type (get params :subtype)
        statement (case data-type
                    :user ["SELECT * FROM user ORDER BY time DESC"]
                    :post ["SELECT * FROM post 
                           WHERE status = 1 ORDER BY time DESC"]
                    :fav (case sub-type
                           :user ["SELECT fav.ptrhash AS userhash, user.* 
                                  FROM fav LEFT JOIN user 
                                  ON fav.ptrhash = user.userhash 
                                  WHERE fav.userhash = ? 
                                  AND fav.status = 1 
                                  ORDER BY fav.mtime DESC"
                                  (get params :userhash)]
                           :post ["SELECT fav.ptrhash AS userhash, post.* 
                                  FROM fav LEFT JOIN post 
                                  ON fav.ptrhash = post.userhash 
                                  AND fav.ptrtime = post.time 
                                  WHERE fav.userhash = ? 
                                  AND fav.status = 1 
                                  AND post.status = 1 
                                  ORDER BY fav.mtime DESC"
                                  (get params :userhash)]
                           nil)
                    :search (case sub-type
                              :user ["SELECT user.* FROM 
                                     FT_SEARCH_DATA(?, 0, 0) ft, user 
                                     WHERE ft.TABLE = 'USER' 
                                     AND user.id = ft.KEYS[0] 
                                     ORDER BY user.time DESC"
                                     (get params :query)]
                              :post ["SELECT post.* FROM 
                                     FT_SEARCH_DATA(?, 0, 0) ft, post 
                                     WHERE ft.TABLE='POST' 
                                     AND post.id = ft.KEYS[0] 
                                     AND post.status = 1 
                                     ORDER BY post.time DESC"
                                     (get params :query)]
                              nil))]
    (when statement
      (with-connection
        spec
        (with-query-results
          rs
          (vec (concat [(paginate (get params :page) (first statement))]
                       (rest statement)))
          (prepare-results rs (or sub-type data-type)))))))

(defn get-pic-data
  ([params]
   (let [user-hash (get params :userhash)
         pic-hash (get params :pichash)]
     (with-connection
       spec
       (with-query-results
         rs
         ["SELECT * FROM pic WHERE userhash = ? AND pichash = ?"
          user-hash pic-hash]
         (prepare-results rs :pic)))))
  ([params ptr-time]
   (let [user-hash (get params :userhash)
         page (get params :page)]
     (with-connection
       spec
       (with-query-results
         rs
         [(paginate page
                    "SELECT * FROM pic WHERE userhash = ? AND ptrtime IS ?")
          user-hash ptr-time]
         (prepare-results rs :pic))))))

; insertion / removal

(defn insert-pic-list
  [user-hash ptr-time edit-time args]
  (let [pics (b-decode-list (get args "pics"))]
    (with-connection
      spec
      (delete-rows
        :pic
        ["userhash = ? AND ptrtime IS ? AND mtime < ?"
         user-hash ptr-time edit-time])
      (doseq [pic pics]
        (when-let [pic-hash (b-decode-bytes pic)]
          (update-or-insert-values
            :pic
            ["pichash = ? AND userhash = ? AND ptrtime IS ?"
             pic-hash user-hash ptr-time]
            {:realuserhash user-hash
             :userhash user-hash
             :pichash pic-hash
             :mtime edit-time
             :ptrtime ptr-time}))))
    pics))

(defn insert-profile
  [user-hash args]
  (let [edit-time (b-decode-long (get args "mtime"))
        pics (insert-pic-list user-hash nil edit-time args)]
    (when (and edit-time
               (<= edit-time (.getTime (java.util.Date.))))
      (with-connection
        spec
        (update-or-insert-values
          :user
          ["userhash = ?" user-hash]
          {:realuserhash user-hash 
           :userhash user-hash 
           :title (b-decode-string (get args "title"))
           :body (b-decode-string (get args "body"))
           :mtime edit-time
           :pichash (b-decode-bytes (get pics 0))
           :status (b-decode-long (get args "status"))})
        (update-values
          :user
          ["userhash = ? AND time IS NULL" user-hash]
          {:time (.getTime (java.util.Date.))})))))

(defn insert-post
  [user-hash post-time args]
  (let [edit-time (b-decode-long (get args "mtime"))
        pics (insert-pic-list user-hash post-time edit-time args)]
    (when (and post-time
               (<= post-time (.getTime (java.util.Date.)))
               edit-time
               (<= edit-time (.getTime (java.util.Date.))))
      (with-connection
        spec
        (update-or-insert-values
          :post
          ["userhash = ? AND time = ?" user-hash post-time]
          {:realuserhash user-hash 
           :userhash user-hash
           :body (b-decode-string (get args "body"))
           :time post-time
           :mtime edit-time
           :pichash (b-decode-bytes (get pics 0))
           :count (count pics)
           :ptrhash (b-decode-bytes (get args "userptrhash"))
           :ptrtime (b-decode-bytes (get args "ptrtime"))
           :status (b-decode-long (get args "status"))})))))

(defn insert-fav
  [user-hash fav-time args]
  (let [edit-time (b-decode-long (get args "mtime"))
        ptr-hash (b-decode-bytes (get args "ptrhash"))
        ptr-time (b-decode-long (get args "ptrtime"))]
    (when (and ptr-hash
               fav-time
               (<= fav-time (.getTime (java.util.Date.)))
               edit-time
               (<= edit-time (.getTime (java.util.Date.))))
      (with-connection
        spec
        (update-or-insert-values
          :fav
          ["userhash = ? AND ptrhash = ? AND ptrtime IS ?"
           user-hash ptr-hash ptr-time]
          {:realuserhash user-hash 
           :userhash user-hash
           :time fav-time
           :mtime edit-time
           :ptrhash ptr-hash
           :ptrtime ptr-time
           :status (b-decode-long (get args "status"))})))))

(defn insert-meta-data
  [user-hash data-map]
  (case (get data-map :dir-name)
    "post" (insert-post user-hash
                        (long-decode (get data-map :file-name))
                        (get data-map :contents))
    "fav" (insert-fav user-hash
                      (long-decode (get data-map :file-name))
                      (get data-map :contents))
    nil (case (get data-map :file-name)
          "user.profile" (insert-profile user-hash (get data-map :contents)))
    nil))

(defn delete-user
  [user-hash]
  (with-connection
    spec
    (delete-rows :user ["userhash = ?" user-hash])
    (delete-rows :post ["userhash = ?" user-hash])
    (delete-rows :pic ["userhash = ?" user-hash])
    (delete-rows :fav ["userhash = ?" user-hash])))
