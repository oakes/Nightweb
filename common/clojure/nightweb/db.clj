(ns nightweb.db
  (:use [nightweb.jdbc :only [with-connection
                              transaction
                              drop-table
                              create-table-if-not-exists
                              update-or-insert-values
                              with-query-results
                              delete-rows]]
        [nightweb.formats :only [base32-decode
                                 b-encode
                                 b-decode
                                 b-decode-bytes
                                 b-decode-string
                                 b-decode-long
                                 b-decode-byte-list]]
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
    [:pics "BINARY"])
  (create-table-if-not-exists
    :post
    [:posthash "BINARY"]
    [:userhash "BINARY"]
    [:body "VARCHAR"]
    [:time "BIGINT"]
    [:pics "BINARY"])
  (create-table-if-not-exists
    :fav
    [:favhash "BINARY"]
    [:userhash "BINARY"]))

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

(defn insert-profile
  [user-hash args]
  (run-query
    (update-or-insert-values
      :user
      ["userhash = ?" user-hash]
      {:userhash user-hash 
       :title (b-decode-string (get args "title"))
       :body (b-decode-string (get args "body"))
       :pics (b-encode (b-decode-byte-list (get args "pics")))})))

(defn insert-post
  [user-hash post-hash args]
  (if-let [time-long (b-decode-long (get args "time"))]
    (run-query
      (update-or-insert-values
        :post
        ["userhash = ? AND time = ?" user-hash time-long]
        {:posthash post-hash
         :userhash user-hash
         :body (b-decode-string (get args "body"))
         :time time-long
         :pics (b-encode (b-decode-byte-list (get args "pics")))}))))

(defn insert-meta-data
  [user-hash data-map]
  (case (get data-map :dir-name)
    "post" (insert-post user-hash
                        (base32-decode (get data-map :file-name))
                        (get data-map :contents))
    nil (case (get data-map :file-name)
          "user.profile" (insert-profile user-hash (get data-map :contents)))
    nil))

; retrieval

(defn get-user-data
  [params]
  (let [user-hash (get params :userhash)
        is-me? (java.util.Arrays/equals user-hash my-hash-bytes)]
    (run-query
      (with-query-results
        rs
        [(str "SELECT * FROM user WHERE userhash = ?") user-hash]
        (if-let [user (first rs)]
          (assoc user
                 :is-me? is-me?
                 :pics (b-decode-byte-list (b-decode (get user :pics))))
          (assoc params :is-me? is-me?))))))

(defn get-single-post-data
  [params]
  (let [user-hash (get params :userhash)
        unix-time (get params :time)]
    (run-query
      (with-query-results
        rs
        [(str "SELECT * FROM post "
              "WHERE userhash = ? AND time = ?") user-hash unix-time]
        (if-let [post (first rs)]
          (assoc post :pics (b-decode-byte-list (b-decode (get post :pics))))
          params)))))

(defn get-post-data
  [params]
  (let [user-hash (get params :userhash)]
    (run-query
      (with-query-results
        rs
        [(str "SELECT * FROM post WHERE userhash = ? "
              "ORDER BY time DESC") user-hash]
        (doall
          (for [row rs]
            (assoc row
                   :type :post
                   :pics (b-decode-byte-list (b-decode (get row :pics))))))))))

(defn get-category-data
  [params]
  (let [data-type (get params :type)
        user-hash (get params :userhash)
        statement (case data-type
                    :user [(str "SELECT * FROM user")]
                    :post [(str "SELECT * FROM post "
                                "ORDER BY time DESC")]
                    :user-fav
                    [(str "SELECT * FROM user "
                          "INNER JOIN fav ON user.userhash = fav.favhash "
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
            (assoc row
                   :type data-type
                   :pics (b-decode-byte-list (b-decode (get row :pics))))))))))
