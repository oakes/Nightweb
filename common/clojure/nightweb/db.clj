(ns nightweb.db
  (:use [nightweb.jdbc :only [with-connection
                              transaction
                              drop-table
                              create-table-if-not-exists
                              update-or-insert-values
                              with-query-results]]
        [nightweb.formats :only [base32-decode
                                 b-decode
                                 b-decode-string
                                 b-decode-long]]
        [nightweb.constants :only [db-file my-hash-bytes]]))

(def spec nil)

(defn run-query
  ([f params] (run-query f params (fn [results] results)))
  ([f params callback]
   (with-connection
     spec
     (transaction (f params callback)))))

; initialization

(defn drop-tables
  [params callback]
  (try
    (do
      (drop-table :user)
      (drop-table :post)
      (drop-table :file)
      (drop-table :prev)
      (drop-table :fav))
    (catch java.lang.Exception e (println "Tables don't exist"))))

(defn create-tables
  [params callback]
  (create-table-if-not-exists
    :user
    [:userhash "BINARY"]
    [:title "VARCHAR"]
    [:body "VARCHAR"])
  (create-table-if-not-exists
    :post
    [:posthash "BINARY"]
    [:userhash "BINARY"]
    [:body "VARCHAR"]
    [:time "BIGINT"])
  (create-table-if-not-exists
    :file
    [:filehash "BINARY"]
    [:userhash "BINARY"]
    [:posthash "BINARY"]
    [:title "VARCHAR"]
    [:ext "VARCHAR"]
    [:bytes "BIGINT"])
  (create-table-if-not-exists
    :prev
    [:prevhash "BINARY"]
    [:userhash "BINARY"]
    [:ptrhash "BINARY"])
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
    ;(run-query drop-tables nil)
    (run-query create-tables nil)))

; insertion

(defn insert-profile
  [user-hash args]
  (with-connection
    spec
    (update-or-insert-values
      :user
      ["userhash = ?" user-hash]
      {:userhash user-hash 
       :title (b-decode-string (get args "title"))
       :body (b-decode-string (get args "body"))})))

(defn insert-post
  [user-hash post-hash args]
  (if-let [time-long (b-decode-long (get args "time"))]
    (with-connection
      spec
      (update-or-insert-values
        :post
        ["userhash = ? AND time = ?" user-hash time-long]
        {:posthash post-hash
         :userhash user-hash
         :body (b-decode-string (get args "body"))
         :time time-long}))))

(defn insert-fav
  [user-hash fav-hash]
  (with-connection
    spec
    (update-or-insert-values
      :fav
      ["favhash = ? AND userhash = ?" fav-hash user-hash]
      {:favhash fav-hash
       :userhash user-hash})))

(defn insert-data
  [data-map]
  (case (get data-map :dir-name)
    "post" (insert-post (get data-map :user-hash)
                        (base32-decode (get data-map :file-name))
                        (get data-map :contents))
    nil (case (get data-map :file-name)
          "user.profile" (insert-profile (get data-map :user-hash)
                                         (get data-map :contents)))))

; retrieval

(defn get-user-data
  [params callback]
  (let [user-hash (get params :userhash)
        is-me? (java.util.Arrays/equals user-hash my-hash-bytes)]
    (with-query-results
      rs
      ["SELECT * FROM user WHERE userhash = ?" user-hash]
      (if-let [user (first rs)]
        (callback (assoc user :is-me? is-me?))
        (callback (assoc params :is-me? is-me?))))))

(defn get-single-post-data
  [params callback]
  (let [user-hash (get params :userhash)
        unix-time (get params :time)]
    (with-query-results
      rs
      ["SELECT * FROM post WHERE userhash = ? AND time = ?" user-hash unix-time]
      (if-let [post (first rs)]
        (callback post)
        (callback params)))))

(defn get-post-data
  [params callback]
  (let [user-hash (get params :userhash)]
    (with-query-results
      rs
      ["SELECT * FROM post WHERE userhash = ? ORDER BY time DESC" user-hash]
      (callback (doall
                  (for [row rs]
                    (assoc row :type :post)))))))

(defn get-category-data
  [params callback]
  (let [data-type (get params :type)
        user-hash (get params :userhash)
        statement (case data-type
                    :user ["SELECT * FROM user"]
                    :post ["SELECT * FROM post ORDER BY time DESC"]
                    :user-fav
                    [(str "SELECT * FROM user "
                          "INNER JOIN fav ON user.userhash = fav.favhash "
                          "WHERE fav.userhash = ?")
                     user-hash]
                    :post-fav
                    [(str "SELECT * FROM post "
                          "INNER JOIN fav ON post.userhash = fav.favhash "
                          "WHERE fav.userhash = ? "
                          "ORDER BY time DESC")
                     user-hash]
                    :all-tran ["SELECT * FROM file"]
                    :photos-tran ["SELECT * FROM file"]
                    :videos-tran ["SELECT * FROM file"]
                    :audio-tran ["SELECT * FROM file"])]
    (with-query-results
      rs
      statement
      (callback (doall
                  (for [row rs]
                    (assoc row :type data-type)))))))
