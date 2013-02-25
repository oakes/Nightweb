(ns nightweb.db
  (:use [nightweb.jdbc :only [with-connection
                              transaction
                              drop-table
                              create-table-if-not-exists
                              update-or-insert-values
                              with-query-results]]
        [nightweb.constants :only [db-file my-hash-bytes]]))

(def spec nil)

(defn run-query
  ([f params] (run-query f params (fn [results] results)))
  ([f params callback]
   (with-connection
     spec
     (transaction (f params callback)))))

(defn drop-tables
  [params callback]
  (try
    (do
      (drop-table :users)
      (drop-table :posts)
      (drop-table :files)
      (drop-table :prevs)
      (drop-table :favs))
    (catch java.lang.Exception e (println "Tables don't exist"))))

(defn create-tables
  [params callback]
  (create-table-if-not-exists
    :users
    [:hash "BINARY"]
    [:text "VARCHAR"]
    [:about "VARCHAR"]
    [:time "BIGINT"])
  (create-table-if-not-exists
    :posts
    [:hash "BINARY"]
    [:userhash "BINARY"]
    [:text "VARCHAR"]
    [:time "BIGINT"])
  (create-table-if-not-exists
    :files
    [:hash "BINARY"]
    [:posthash "BINARY"]
    [:name "VARCHAR"]
    [:ext "VARCHAR"]
    [:bytes "BIGINT"])
  (create-table-if-not-exists
    :prevs
    [:hash "BINARY"]
    [:pointer "BINARY"])
  (create-table-if-not-exists
    :favs
    [:userhash "BINARY"]
    [:ptrhash "BINARY"]))

(defn init-db
  [base-dir]
  (when (nil? spec)
    (def spec
      {:classname "org.h2.Driver"
       :subprotocol "h2"
       :subname (str base-dir db-file)})
    ;(run-query drop-tables nil)
    (run-query create-tables nil)))

(defn insert-profile
  [user-hash args]
  (with-connection
    spec
    (update-or-insert-values
      :users
      ["hash = ?" user-hash]
      {:hash user-hash 
       :text (get args "name")
       :about (get args "about")
       :time (get args "time")})))

(defn insert-post
  [user-hash post-hash args]
  (with-connection
    spec
    (update-or-insert-values
      :posts
      ["hash = ? AND userhash = ?" user-hash post-hash]
      {:hash post-hash
       :userhash user-hash
       :text (get args "text")
       :time (get args "time")})))

(defn insert-fav
  [user-hash args]
  (if-let [ptr-hash (get args "ptrhash")]
    (with-connection
      spec
      (update-or-insert-values
        :favs
        ["userhash = ? AND ptrhash = ?" user-hash ptr-hash]
        {:userhash user-hash :ptrhash ptr-hash}))))

(defn get-user-data
  [params callback]
  (let [user-hash (get params :hash)
        is-me? (java.util.Arrays/equals user-hash my-hash-bytes)]
    (with-query-results
      rs
      ["SELECT * FROM users WHERE hash = ?" user-hash]
      (if-let [user (first rs)]
        (callback (assoc user :is-me? is-me?))
        (callback (assoc params :is-me? is-me?))))))

(defn get-post-data
  [params callback]
  (let [user-hash (get params :hash)]
    (with-query-results
      rs
      ["SELECT * FROM posts WHERE userhash = ? ORDER BY time DESC" user-hash]
      (callback (doall
                  (for [row rs]
                    (assoc row :type :posts)))))))

(defn get-category-data
  [params callback]
  (let [data-type (get params :type)
        user-hash (get params :hash)
        statement (case data-type
                    :users ["SELECT * FROM users"]
                    :posts ["SELECT * FROM posts ORDER BY time DESC"]
                    :users-favorites
                    [(str "SELECT * FROM users "
                          "INNER JOIN favs ON users.hash = favs.pointer "
                          "WHERE favs.user = ?")
                     user-hash]
                    :posts-favorites
                    [(str "SELECT * FROM posts "
                          "INNER JOIN favs ON posts.userhash = favs.ptrhash "
                          "WHERE favs.userhash = ? "
                          "ORDER BY time DESC")
                     user-hash]
                    :all-transfers ["SELECT * FROM files"]
                    :photos-transfers ["SELECT * FROM files"]
                    :videos-transfers ["SELECT * FROM files"]
                    :audio-transfers ["SELECT * FROM files"])]
    (with-query-results
      rs
      statement
      (callback (doall
                  (for [row rs]
                    (assoc row :type data-type)))))))
