(ns nightweb.db
  (:use [nightweb.jdbc :only [with-connection
                              transaction
                              drop-table
                              create-table-if-not-exists
                              update-or-insert-values
                              with-query-results
                              delete-rows]]
        [nightweb.formats :only [base32-decode
                                 b-decode
                                 b-decode-bytes
                                 b-decode-string
                                 b-decode-long
                                 b-decode-list]]
        [nightweb.constants :only [db-file my-hash-bytes]]))

(def spec nil)

(defmacro run-query
  [& body]
  `(with-connection
     spec
     (transaction ~@body)))

; initialization

(defn drop-tables
  [params]
  (try
    (do
      (drop-table :user)
      (drop-table :post)
      (drop-table :file)
      (drop-table :pic)
      (drop-table :fav))
    (catch java.lang.Exception e (println "Tables don't exist"))))

(defn create-tables
  [params]
  (create-table-if-not-exists
    :user
    [:userhash "BINARY"]
    [:title "VARCHAR"]
    [:body "VARCHAR"]
    [:lastupdated "BIGINT"])
  (create-table-if-not-exists
    :post
    [:posthash "BINARY"]
    [:userhash "BINARY"]
    [:body "VARCHAR"]
    [:time "BIGINT"]
    [:lastupdated "BIGINT"])
  (create-table-if-not-exists
    :file
    [:filehash "BINARY"]
    [:userhash "BINARY"]
    [:posthash "BINARY"]
    [:title "VARCHAR"]
    [:ext "VARCHAR"]
    [:bytes "BIGINT"]
    [:lastupdated "BIGINT"])
  (create-table-if-not-exists
    :pic
    [:pichash "BINARY"]
    [:userhash "BINARY"]
    [:ptrhash "BINARY"]
    [:lastupdated "BIGINT"])
  (create-table-if-not-exists
    :fav
    [:favhash "BINARY"]
    [:userhash "BINARY"]
    [:lastupdated "BIGINT"]))

(defn init-db
  [base-dir]
  (when (nil? spec)
    (def spec
      {:classname "org.h2.Driver"
       :subprotocol "h2"
       :subname (str base-dir db-file)})
    ;(run-query (drop-tables nil))
    (run-query (create-tables nil))))

; insertion

(defn insert-pic-list
  [user-hash ptr-hash args last-updated]
  (run-query
    (doseq [pic (b-decode-list (get args "pics"))]
      (if-let [pic-hash (b-decode-bytes pic)]
        (update-or-insert-values
          :pic
          ["pichash = ? AND userhash = ? AND ptrhash = ?"
           pic-hash user-hash ptr-hash]
          {:pichash pic-hash
           :userhash user-hash
           :ptrhash ptr-hash
           :lastupdated last-updated})))))

(defn insert-profile
  [user-hash args last-updated]
  (run-query
    (update-or-insert-values
      :user
      ["userhash = ?" user-hash]
      {:userhash user-hash 
       :title (b-decode-string (get args "title"))
       :body (b-decode-string (get args "body"))
       :lastupdated last-updated}))
  (insert-pic-list user-hash user-hash args last-updated))

(defn insert-post
  [user-hash post-hash args last-updated]
  (if-let [time-long (b-decode-long (get args "time"))]
    (run-query
      (update-or-insert-values
        :post
        ["userhash = ? AND time = ?" user-hash time-long]
        {:posthash post-hash
         :userhash user-hash
         :body (b-decode-string (get args "body"))
         :time time-long
         :lastupdated last-updated})))
  (insert-pic-list user-hash post-hash args last-updated))

(defn insert-meta-data
  [user-hash data-map last-updated]
  (case (get data-map :dir-name)
    "post" (insert-post user-hash
                        (base32-decode (get data-map :file-name))
                        (get data-map :contents)
                        last-updated)
    nil (case (get data-map :file-name)
          "user.profile" (insert-profile user-hash
                                         (get data-map :contents)
                                         last-updated))
    nil))

; retrieval

(defn get-user-data
  [params]
  (let [user-hash (get params :userhash)
        is-me? (java.util.Arrays/equals user-hash my-hash-bytes)]
    (run-query
      (with-query-results
        rs
        [(str "SELECT * FROM user LEFT JOIN pic "
              "ON user.userhash = pic.userhash "
              "AND user.userhash = pic.ptrhash "
              "WHERE user.userhash = ?") user-hash]
        (if-let [user (first rs)]
          (assoc user :is-me? is-me?)
          (assoc params :is-me? is-me?))))))

(defn get-single-post-data
  [params]
  (let [user-hash (get params :userhash)
        unix-time (get params :time)]
    (run-query
      (with-query-results
        rs
        [(str "SELECT * FROM post LEFT JOIN pic "
              "ON post.userhash = pic.userhash "
              "AND post.posthash = pic.ptrhash "
              "WHERE post.userhash = ? AND post.time = ?") user-hash unix-time]
        (if-let [post (first rs)]
          post
          params)))))

(defn get-pic-data
  [params]
  (let [user-hash (get params :userhash)
        ptr-hash (get params :ptrhash)]
    (run-query
      (with-query-results
        rs
        ["SELECT * FROM pic WHERE userhash = ? AND ptrhash = ?"
         user-hash ptr-hash]
        (doall rs)))))

(defn get-post-data
  [params]
  (let [user-hash (get params :userhash)]
    (run-query
      (with-query-results
        rs
        [(str "SELECT * FROM post LEFT JOIN pic "
              "ON post.userhash = pic.userhash "
              "AND post.posthash = pic.ptrhash "
              "WHERE post.userhash = ? ORDER BY post.time DESC") user-hash]
        (doall
          (for [row rs]
            (assoc row :type :post)))))))

(defn get-category-data
  [params]
  (let [data-type (get params :type)
        user-hash (get params :userhash)
        statement (case data-type
                    :user [(str "SELECT * FROM user LEFT JOIN pic "
                                "ON user.userhash = pic.userhash "
                                "AND user.userhash = pic.ptrhash")]
                    :post [(str "SELECT * FROM post LEFT JOIN pic "
                                "ON post.userhash = pic.userhash "
                                "AND post.posthash = pic.ptrhash "
                                "ORDER BY post.time DESC")]
                    :user-fav
                    [(str "SELECT * FROM user "
                          "INNER JOIN fav ON user.userhash = fav.favhash "
                          "LEFT JOIN pic ON user.userhash = pic.userhash "
                          "AND user.userhash = pic.ptrhash "
                          "WHERE fav.userhash = ?")
                     user-hash]
                    :post-fav
                    [(str "SELECT * FROM post "
                          "INNER JOIN fav ON post.userhash = fav.favhash "
                          "WHERE fav.userhash = ? "
                          "ORDER BY post.time DESC")
                     user-hash]
                    :all-tran ["SELECT * FROM file"]
                    :photos-tran ["SELECT * FROM file"]
                    :videos-tran ["SELECT * FROM file"]
                    :audio-tran ["SELECT * FROM file"])]
    (run-query
      (with-query-results
        rs
        statement
        (doall
          (for [row rs]
            (assoc row :type data-type)))))))

(defn get-old-pic-data
  [params]
  (let [user-hash (get params :userhash)
        last-updated (get params :lastupdated)]
    (run-query
      (with-query-results
        rs
        ["SELECT * FROM pic WHERE userhash = ? AND lastupdated <> ?"
         user-hash last-updated]
        (doall rs)))))

(defn get-old-post-data
  [params]
  (let [user-hash (get params :userhash)
        last-updated (get params :lastupdated)]
    (run-query
      (with-query-results
        rs
        ["SELECT * FROM post WHERE userhash = ? AND lastupdated <> ?"
         user-hash last-updated]
        (doall rs)))))

; deletion

(defn delete-old-pic-data
  [params]
  (let [user-hash (get params :userhash)
        last-updated (get params :lastupdated)]
    (run-query
      (delete-rows
        :pic
        ["userhash = ? AND lastupdated <> ?" user-hash last-updated]))))

(defn delete-old-post-data
  [params]
  (let [user-hash (get params :userhash)
        last-updated (get params :lastupdated)]
    (run-query
      (delete-rows
        :post
        ["userhash = ? AND lastupdated <> ?" user-hash last-updated]))))
