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
     :subname (str path "/main")
     :cipher "AES"
     :password (str (if password password "password") " ")}))

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
      (drop-table :prevs))
    (catch java.lang.Exception e (println "Tables don't exist"))))

(defn create-tables
  [params callback]
  (create-table-if-not-exists
    :users
    [:hash "BINARY"]
    [:name "VARCHAR"]
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
    [:pointer "BINARY"]))

(defn insert-test-data
  [params callback]
  (insert-records
    :users
    {:hash (byte-array (map byte [0])) :name "oskar" :about "Hello, World!"}))

(defn get-profile-data
  [params callback]
  (let [user-hash (get params :hash)]
    (with-query-results
      rs
      ["SELECT * FROM users WHERE hash=?" user-hash]
      (callback (first rs)))))
