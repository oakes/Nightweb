(ns nightweb.db
  (:require [clojure.java.jdbc :as jdbc]
            [nightweb.constants :as c]
            [nightweb.formats :as f]))

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
         (into {} (assoc row
                         :type table
                         :title (f/escape-html (:title row))
                         :body (f/escape-html (:body row)))))
       (doall)
       (vec)))

; initialization

(defn check-table
  ([table-name] (check-table table-name "*"))
  ([table-name column-name]
   (try
     (jdbc/with-connection
       @spec
       (jdbc/with-query-results
         rs
         [(str "SELECT COUNT(" (name column-name) ") FROM " (name table-name))]
         rs))
     (catch Exception e nil))))

(defn create-index
  [table-name columns]
  (jdbc/do-commands
    "CREATE ALIAS IF NOT EXISTS FT_INIT FOR \"org.h2.fulltext.FullText.init\";"
    "CALL FT_INIT();"
    (format "CALL FT_CREATE_INDEX('PUBLIC', '%s', '%s');"
            table-name (clojure.string/join "," columns))))

(defn create-generic-table
  [table-name]
  (jdbc/create-table
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
    (jdbc/with-connection
      @spec
      (create-generic-table :user)
      (create-index "USER" ["ID" "TITLE" "BODY"])))
  (when-not (check-table :post)
    (jdbc/with-connection
      @spec
      (create-generic-table :post)
      (create-index "POST" ["ID" "TITLE" "BODY"])))
  (when-not (check-table :pic)
    (jdbc/with-connection
      @spec
      (create-generic-table :pic)))
  (when-not (check-table :fav)
    (jdbc/with-connection
      @spec
      (create-generic-table :fav)))
  (when-not (check-table :tag)
    (jdbc/with-connection
      @spec
      (create-generic-table :tag))))

(defn init-db
  [base-dir]
  (when (nil? @spec)
    (reset! spec
            {:classname "org.h2.Driver"
             :subprotocol "h2"
             :subname (str base-dir c/db-file)})
    (create-tables)))

; retrieval

(defn get-single-user-data
  [params]
  (let [user-hash (:userhash params)]
    (jdbc/with-connection
      @spec
      (jdbc/with-query-results
        rs
        ["SELECT * FROM user WHERE userhash = ?" user-hash]
        (if-let [user (first (prepare-results rs :user))]
          (dissoc user :time)
          {:userhash user-hash :type :user})))))

(defn get-single-post-data
  [params]
  (let [user-hash (:userhash params)
        create-time (:time params)]
    (jdbc/with-connection
      @spec
      (jdbc/with-query-results
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
    (jdbc/with-connection
      @spec
      (jdbc/with-query-results
        rs
        [(paginate page
                   "SELECT * FROM post 
                   WHERE userhash = ? AND status = 1 ORDER BY time DESC")
         user-hash]
        (prepare-results rs :post)))))

(defn get-single-fav-data
  ([params] (get-single-fav-data params @c/my-hash-bytes))
  ([params my-user-hash]
   (let [ptr-hash (:userhash params)
         ptr-time (:time params)]
     (jdbc/with-connection
       @spec
       (jdbc/with-query-results
         rs
         ["SELECT * FROM fav WHERE userhash = ? AND ptrhash = ? AND ptrtime IS ?"
          my-user-hash ptr-hash ptr-time]
         (first (prepare-results rs :fav)))))))

(defn get-fav-data
  ([params] (get-fav-data params @c/my-hash-bytes))
  ([params my-user-hash]
   (let [ptr-hash (:ptrhash params)]
     (jdbc/with-connection
       @spec
       (jdbc/with-query-results
         rs
         ["SELECT * FROM fav WHERE ptrhash = ? 
          AND status = 1 
          AND (userhash IN 
          (SELECT ptrhash FROM fav WHERE userhash = ? AND status = 1) 
          OR userhash = ?)"
          ptr-hash my-user-hash my-user-hash]
         (prepare-results rs :fav))))))

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
      (jdbc/with-connection
        @spec
        (jdbc/with-query-results
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
      (jdbc/with-connection
        @spec
        (jdbc/with-query-results
          rs
          statement
          (first (prepare-results rs :tag)))))))

(defn get-pic-data
  ([params]
   (let [user-hash (:userhash params)
         pic-hash (:pichash params)]
     (jdbc/with-connection
       @spec
       (jdbc/with-query-results
         rs
         ["SELECT * FROM pic WHERE userhash = ? AND pichash = ?"
          user-hash pic-hash]
         (prepare-results rs :pic)))))
  ([params ptr-time paginate?]
   (let [user-hash (:userhash params)
         page (:page params)]
     (jdbc/with-connection
       @spec
       (jdbc/with-query-results
         rs
         [(let [sql "SELECT * FROM pic WHERE userhash = ? AND ptrtime IS ?"]
            (if paginate? (paginate page sql) sql))
          user-hash ptr-time]
         (prepare-results rs :pic))))))

; insertion / removal

(defn insert-tag-list
  [user-hash ptr-time edit-time args]
  (let [tags (f/tags-decode (f/b-decode-string (get args "body")))
        pics (f/b-decode-list (get args "pics"))
        pic-hash (f/b-decode-bytes (get pics 0))]
    (jdbc/with-connection
      @spec
      (jdbc/delete-rows
        :tag
        ["userhash = ? AND ptrtime IS ? AND mtime < ?"
         user-hash ptr-time edit-time])
      (doseq [tag tags]
        (jdbc/update-or-insert-values
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
  (let [pics (f/b-decode-list (get args "pics"))]
    (jdbc/with-connection
      @spec
      (jdbc/delete-rows
        :pic
        ["userhash = ? AND ptrtime IS ? AND mtime < ?"
         user-hash ptr-time edit-time])
      (doseq [pic pics]
        (when-let [pic-hash (f/b-decode-bytes pic)]
          (jdbc/update-or-insert-values
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
  (let [edit-time (f/b-decode-long (get args "mtime"))
        pics (insert-pic-list user-hash nil edit-time args)
        tags (insert-tag-list user-hash nil edit-time args)]
    (when (and edit-time
               (<= edit-time (.getTime (java.util.Date.))))
      (jdbc/with-connection
        @spec
        (jdbc/update-or-insert-values
          :user
          ["userhash = ?" user-hash]
          {:realuserhash user-hash 
           :userhash user-hash 
           :title (f/b-decode-string (get args "title"))
           :body (f/b-decode-string (get args "body"))
           :mtime edit-time
           :pichash (f/b-decode-bytes (get pics 0))
           :status (f/b-decode-long (get args "status"))})
        (jdbc/update-values
          :user
          ["userhash = ? AND time IS NULL" user-hash]
          {:time (.getTime (java.util.Date.))})))))

(defn insert-post
  [user-hash post-time args]
  (let [edit-time (f/b-decode-long (get args "mtime"))
        pics (insert-pic-list user-hash post-time edit-time args)
        tags (insert-tag-list user-hash post-time edit-time args)]
    (when (and post-time
               (<= post-time (.getTime (java.util.Date.)))
               edit-time
               (<= edit-time (.getTime (java.util.Date.))))
      (jdbc/with-connection
        @spec
        (jdbc/update-or-insert-values
          :post
          ["userhash = ? AND time = ?" user-hash post-time]
          {:realuserhash user-hash 
           :userhash user-hash
           :body (f/b-decode-string (get args "body"))
           :time post-time
           :mtime edit-time
           :pichash (f/b-decode-bytes (get pics 0))
           :count (count pics)
           :ptrhash (f/b-decode-bytes (get args "ptrhash"))
           :ptrtime (f/b-decode-long (get args "ptrtime"))
           :status (f/b-decode-long (get args "status"))})))))

(defn insert-fav
  [user-hash fav-time args]
  (let [edit-time (f/b-decode-long (get args "mtime"))
        ptr-hash (f/b-decode-bytes (get args "ptrhash"))
        ptr-time (f/b-decode-long (get args "ptrtime"))]
    (when (and ptr-hash
               fav-time
               (<= fav-time (.getTime (java.util.Date.)))
               edit-time
               (<= edit-time (.getTime (java.util.Date.))))
      (jdbc/with-connection
        @spec
        (jdbc/update-or-insert-values
          :fav
          ["userhash = ? AND ptrhash = ? AND ptrtime IS ?"
           user-hash ptr-hash ptr-time]
          {:realuserhash user-hash 
           :userhash user-hash
           :time fav-time
           :mtime edit-time
           :ptrhash ptr-hash
           :ptrtime ptr-time
           :status (f/b-decode-long (get args "status"))})))))

(defn insert-meta-data
  [user-hash data-map]
  (case (:dir-name data-map)
    "post" (insert-post user-hash
                        (f/long-decode (:file-name data-map))
                        (:contents data-map))
    "fav" (insert-fav user-hash
                      (f/long-decode (:file-name data-map))
                      (:contents data-map))
    "meta" (case (:file-name data-map)
             "user.profile" (insert-profile user-hash (:contents data-map))
             nil)
    nil))

(defn delete-user
  [user-hash]
  (jdbc/with-connection
    @spec
    (jdbc/delete-rows :user ["userhash = ?" user-hash])
    (jdbc/delete-rows :post ["userhash = ?" user-hash])
    (jdbc/delete-rows :pic ["userhash = ?" user-hash])
    (jdbc/delete-rows :fav ["userhash = ?" user-hash])
    (jdbc/delete-rows :tag ["userhash = ?" user-hash])))
