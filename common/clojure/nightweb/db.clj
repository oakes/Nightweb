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
                                 b-decode-byte-list
                                 tags-decode]]
        [nightweb.constants :only [db-file my-hash-bytes]]))

(def spec (atom nil))
(def ^:const limit 24)
(def ^:const max-length-small 20)
(def ^:const max-length-large 10000)

(defn paginate
  [page statement]
  (format (str statement " LIMIT %d OFFSET %d")
          (+ limit 1)
          (* limit (if page (- page 1) 0))))

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
       @spec
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
      @spec
      (create-generic-table :user)
      (create-index "USER" ["ID" "TITLE" "BODY"])))
  (when-not (check-table :post)
    (with-connection
      @spec
      (create-generic-table :post)
      (create-index "POST" ["ID" "TITLE" "BODY"])))
  (when-not (check-table :pic)
    (with-connection
      @spec
      (create-generic-table :pic)))
  (when-not (check-table :fav)
    (with-connection
      @spec
      (create-generic-table :fav)))
  (when-not (check-table :tag)
    (with-connection
      @spec
      (create-generic-table :tag))))

(defn init-db
  [base-dir]
  (when (nil? @spec)
    (reset! spec
            {:classname "org.h2.Driver"
             :subprotocol "h2"
             :subname (str base-dir db-file)})
    (create-tables)))

; retrieval

(defn get-single-user-data
  [params]
  (let [user-hash (:userhash params)]
    (with-connection
      @spec
      (with-query-results
        rs
        ["SELECT * FROM user WHERE userhash = ?" user-hash]
        (if-let [user (first (prepare-results rs :user))]
          (dissoc user :time)
          {:userhash user-hash :type :user})))))

(defn get-single-post-data
  [params]
  (let [user-hash (:userhash params)
        create-time (:time params)]
    (with-connection
      @spec
      (with-query-results
        rs
        ["SELECT * FROM post WHERE userhash = ? AND time = ? AND status = 1"
         user-hash create-time]
        (if-let [post (first (prepare-results rs :post))]
          post
          {:userhash user-hash :time create-time :type :post})))))

(defn get-post-data
  [params]
  (let [user-hash (:userhash params)
        page (:page params)]
    (with-connection
      @spec
      (with-query-results
        rs
        [(paginate page
                   "SELECT * FROM post 
                   WHERE userhash = ? AND status = 1 ORDER BY time DESC")
         user-hash]
        (prepare-results rs :post)))))

(defn get-single-fav-data
  [params]
  (let [ptr-hash (:userhash params)
        ptr-time (:time params)]
    (with-connection
      @spec
      (with-query-results
        rs
        ["SELECT * FROM fav WHERE userhash = ? AND ptrhash = ? AND ptrtime IS ?"
         @my-hash-bytes ptr-hash ptr-time]
        (first (prepare-results rs :fav))))))

(defn get-fav-data
  [params]
  (let [ptr-hash (:ptrhash params)]
    (with-connection
      @spec
      (with-query-results
        rs
        ["SELECT * FROM fav WHERE ptrhash = ? 
         AND status = 1 
         AND (userhash IN 
         (SELECT ptrhash FROM fav WHERE userhash = ? AND status = 1) 
         OR userhash = ?)"
         ptr-hash @my-hash-bytes @my-hash-bytes]
        (prepare-results rs :fav)))))

(defn get-category-data
  [params]
  (let [data-type (:type params)
        sub-type (:subtype params)
        statement (case data-type
                    :user (if-let [tag (:tag params)]
                            ["SELECT user.* FROM user 
                             INNER JOIN tag 
                             ON user.userhash = tag.userhash 
                             WHERE tag.title = ? 
                             AND tag.ptrtime IS NULL 
                             ORDER BY user.time DESC" tag]
                            ["SELECT * FROM user ORDER BY time DESC"])
                    :post (if-let [tag (:tag params)]
                            ["SELECT post.*, user.title AS subtitle FROM post 
                             INNER JOIN tag
                             ON post.userhash = tag.userhash
                             AND post.time = tag.ptrtime 
                             LEFT JOIN user 
                             ON post.userhash = user.userhash 
                             WHERE post.status = 1 
                             AND tag.title = ? 
                             ORDER BY post.time DESC" tag]
                            ["SELECT post.*, user.title AS subtitle FROM post 
                             LEFT JOIN user 
                             ON post.userhash = user.userhash 
                             WHERE post.status = 1 
                             ORDER BY post.time DESC"])
                    :fav (case sub-type
                           :user ["SELECT fav.ptrhash AS userhash, user.* 
                                  FROM fav 
                                  LEFT JOIN user 
                                  ON fav.ptrhash = user.userhash 
                                  WHERE fav.userhash = ? 
                                  AND fav.status = 1 
                                  AND fav.ptrtime IS NULL 
                                  ORDER BY fav.mtime DESC"
                                  (:userhash params)]
                           :post ["SELECT fav.ptrhash AS userhash, post.*, 
                                  user.title AS subtitle 
                                  FROM fav 
                                  LEFT JOIN post 
                                  ON fav.ptrhash = post.userhash 
                                  AND fav.ptrtime = post.time 
                                  LEFT JOIN user 
                                  ON post.userhash = user.userhash 
                                  WHERE fav.userhash = ? 
                                  AND fav.status = 1 
                                  AND post.status = 1 
                                  ORDER BY fav.mtime DESC"
                                  (:userhash params)]
                           nil)
                    :search (case sub-type
                              :user ["SELECT user.* 
                                     FROM FT_SEARCH_DATA(?, 0, 0) ft, user 
                                     WHERE ft.TABLE = 'USER' 
                                     AND user.id = ft.KEYS[0] 
                                     ORDER BY user.time DESC"
                                     (:query params)]
                              :post ["SELECT post.*, user.title AS subtitle 
                                     FROM FT_SEARCH_DATA(?, 0, 0) ft, post 
                                     LEFT JOIN user 
                                     ON post.userhash = user.userhash 
                                     WHERE ft.TABLE='POST' 
                                     AND post.id = ft.KEYS[0] 
                                     AND post.status = 1 
                                     ORDER BY post.time DESC"
                                     (:query params)]
                              nil)
                    :tag (case sub-type
                           :user ["SELECT title AS tag, 
                                  COUNT(*) AS count 
                                  FROM tag 
                                  WHERE ptrtime IS NULL 
                                  GROUP BY title 
                                  ORDER BY count DESC"]
                           :post ["SELECT title AS tag, 
                                  COUNT(*) AS count 
                                  FROM tag 
                                  WHERE ptrtime IS NOT NULL 
                                  GROUP BY title 
                                  ORDER BY count DESC"]
                           nil))]
    (when statement
      (with-connection
        @spec
        (with-query-results
          rs
          (vec (concat [(paginate (:page params) (first statement))]
                       (rest statement)))
          (prepare-results rs (or sub-type data-type)))))))

(defn get-single-tag-data
  [params]
  (let [tag (:tag params)
        statement (case (:type params)
                    :user ["SELECT * FROM tag 
                           WHERE title = ? 
                           AND pichash IS NOT NULL 
                           AND ptrtime IS NULL 
                           ORDER BY mtime DESC 
                           LIMIT 1" tag]
                    :post ["SELECT * FROM tag 
                           WHERE title = ? 
                           AND pichash IS NOT NULL 
                           AND ptrtime IS NOT NULL 
                           ORDER BY mtime DESC 
                           LIMIT 1" tag]
                    nil)]
    (when statement
      (with-connection
        @spec
        (with-query-results
          rs
          statement
          (first (prepare-results rs :tag)))))))

(defn get-pic-data
  ([params]
   (let [user-hash (:userhash params)
         pic-hash (:pichash params)]
     (with-connection
       @spec
       (with-query-results
         rs
         ["SELECT * FROM pic WHERE userhash = ? AND pichash = ?"
          user-hash pic-hash]
         (prepare-results rs :pic)))))
  ([params ptr-time paginate?]
   (let [user-hash (:userhash params)
         page (:page params)]
     (with-connection
       @spec
       (with-query-results
         rs
         [(let [sql "SELECT * FROM pic WHERE userhash = ? AND ptrtime IS ?"]
            (if paginate? (paginate page sql) sql))
          user-hash ptr-time]
         (prepare-results rs :pic))))))

; insertion / removal

(defn insert-tag-list
  [user-hash ptr-time edit-time args]
  (let [tags (tags-decode (b-decode-string (get args "body")))
        pics (b-decode-list (get args "pics"))
        pic-hash (b-decode-bytes (get pics 0))]
    (with-connection
      @spec
      (delete-rows
        :tag
        ["userhash = ? AND ptrtime IS ? AND mtime < ?"
         user-hash ptr-time edit-time])
      (doseq [tag tags]
        (update-or-insert-values
          :tag
          ["title = ? AND userhash = ? AND ptrtime IS ?"
           tag user-hash ptr-time]
          {:realuserhash user-hash
           :userhash user-hash
           :title tag
           :mtime edit-time
           :ptrtime ptr-time
           :pichash pic-hash})))
    tags))

(defn insert-pic-list
  [user-hash ptr-time edit-time args]
  (let [pics (b-decode-list (get args "pics"))]
    (with-connection
      @spec
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
        pics (insert-pic-list user-hash nil edit-time args)
        tags (insert-tag-list user-hash nil edit-time args)]
    (when (and edit-time
               (<= edit-time (.getTime (java.util.Date.))))
      (with-connection
        @spec
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
        pics (insert-pic-list user-hash post-time edit-time args)
        tags (insert-tag-list user-hash post-time edit-time args)]
    (when (and post-time
               (<= post-time (.getTime (java.util.Date.)))
               edit-time
               (<= edit-time (.getTime (java.util.Date.))))
      (with-connection
        @spec
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
           :ptrhash (b-decode-bytes (get args "ptrhash"))
           :ptrtime (b-decode-long (get args "ptrtime"))
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
        @spec
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
  (case (:dir-name data-map)
    "post" (insert-post user-hash
                        (long-decode (:file-name data-map))
                        (:contents data-map))
    "fav" (insert-fav user-hash
                      (long-decode (:file-name data-map))
                      (:contents data-map))
    "meta" (case (:file-name data-map)
             "user.profile" (insert-profile user-hash (:contents data-map))
             nil)
    nil))

(defn delete-user
  [user-hash]
  (with-connection
    @spec
    (delete-rows :user ["userhash = ?" user-hash])
    (delete-rows :post ["userhash = ?" user-hash])
    (delete-rows :pic ["userhash = ?" user-hash])
    (delete-rows :fav ["userhash = ?" user-hash])
    (delete-rows :tag ["userhash = ?" user-hash])))
