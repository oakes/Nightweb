(ns nightweb.db
  (:use [nightweb.jdbc :only [with-connection
                              transaction
                              drop-table
                              create-table-if-not-exists
                              insert-records
                              with-query-results]]))

(def spec nil)

(defn defspec
  [path password]
  (def spec
    {:classname "org.h2.Driver"
     :subprotocol "h2"
     :subname (str path "/main")}))

(defn run-query
  [f params callback]
  (with-connection
    spec
    (transaction (f params callback))))

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
    [:time "TIMESTAMP"])
  (create-table-if-not-exists
    :posts
    [:hash "BINARY"]
    [:user "BINARY"]
    [:text "VARCHAR"]
    [:time "TIMESTAMP"])
  (create-table-if-not-exists
    :files
    [:hash "BINARY"]
    [:post "BINARY"]
    [:name "VARCHAR"]
    [:ext "VARCHAR"]
    [:bytes "BIGINT"]
    [:time "TIMESTAMP"])
  (create-table-if-not-exists
    :prevs
    [:hash "BINARY"]
    [:pointer "BINARY"])
  (create-table-if-not-exists
    :favs
    [:hash "BINARY"]
    [:user "BINARY"]
    [:pointer "BINARY"]))

(defn insert-test-data
  [params callback]
  (let [oskar (byte-array (map byte [0]))
        papa (byte-array (map byte [1]))
        quebec (byte-array (map byte [2]))
        post1 (byte-array (map byte [3]))
        post2 (byte-array (map byte [4]))
        post3 (byte-array (map byte [5]))
        fav1 (byte-array (map byte [6]))
        fav2 (byte-array (map byte [7]))
        fav3 (byte-array (map byte [8]))]
    (insert-records
      :users
      {:hash oskar :text "oskar" :about "Hello, World!"}
      {:hash papa :text "papa" :about "Hello, World!"}
      {:hash quebec :text "quebec" :about "Hello, World!"})
    (insert-records
      :posts
      {:hash post1 :user oskar :text "First post!"}
      {:hash post2 :user papa :text "From papa!"}
      {:hash post3 :user quebec :text "From quebec!"})
    (insert-records
      :favs
      {:hash fav1 :user oskar :pointer papa}
      {:hash fav2 :user papa :pointer oskar}
      {:hash fav3 :user quebec :pointer papa})))

(defn get-user-data
  [params callback]
  (let [user-hash (get params :hash)]
    (with-query-results
      rs
      ["SELECT * FROM users WHERE hash = ?" user-hash]
      (if-let [user (first rs)]
        (callback (assoc user :is-me? (java.util.Arrays/equals
                                        user-hash
                                        (byte-array (map byte [0])))))))))

(defn get-post-data
  [params callback]
  (let [user-hash (get params :hash)]
    (with-query-results
      rs
      ["SELECT * FROM posts WHERE user = ?" user-hash]
      (callback (doall
                  (for [row rs]
                    (assoc row :type :posts)))))))

(defn get-category-data
  [params callback]
  (let [data-type (get params :type)
        user-hash (get params :hash)
        statement (case data-type
                    :users ["SELECT * FROM users"]
                    :posts ["SELECT * FROM posts"]
                    :users-favorites
                    [(str "SELECT * FROM users "
                          "INNER JOIN favs ON users.hash = favs.pointer "
                          "WHERE favs.user = ?")
                     user-hash]
                    :posts-favorites
                    [(str "SELECT * FROM posts "
                          "INNER JOIN favs ON posts.hash = favs.pointer "
                          "WHERE favs.user = ?")
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
