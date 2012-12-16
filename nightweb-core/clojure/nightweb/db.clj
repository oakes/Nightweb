(ns nightweb.db
  (:use [nightweb.jdbc :only [create-table-if-not-exists
                              with-connection
                              transaction]]))

(defn create-tables
  []
  (create-table-if-not-exists
    :users
    [:hash "BINARY"]
    [:name "VARCHAR"]
    [:about "CLOB"]
    [:time "TIMESTAMP"])
  (create-table-if-not-exists
    :posts
    [:hash "BINARY"]
    [:user "BINARY"]
    [:text "CLOB"]
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
    :previews
    [:hash "BINARY"]
    [:pointer "BINARY"]))

(defn get-spec
  [path password]
  {:classname "org.h2.Driver"
   :subprotocol "h2"
   :subname (str path "/main")
   :cipher "AES"
   :password (str (if password password "password") " ")})

(def spec nil)

(defn run-query
  [f]
  (with-connection
    spec
    (transaction (f))))

(defn create-database
  [path password]
  (def spec (get-spec path password))
  (run-query create-tables))
