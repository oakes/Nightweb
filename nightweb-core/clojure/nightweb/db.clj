(ns nightweb.db
  (:require [clojure.string :as string]
            [clojure.java.jdbc :as sql]))

(defn create-table-if-not-exists
  [name & specs]
  (sql/do-commands (string/replace
                     (apply sql/create-table-ddl name specs)
                     "CREATE TABLE"
                     "CREATE TABLE IF NOT EXISTS")))

(defn create-tables
  []
  (create-table-if-not-exists
    "websites"
    [:id "IDENTITY" "NOT NULL" "PRIMARY KEY"]))

(defn get-spec
  ([] (get-spec "default-password"))
  ([password]
    {:classname "org.h2.Driver"
     :subprotocol "h2"
     :subname "main"
     :cipher "AES"
     :password (str password " ")}))

(defn invoke-with-connection
  [f]
  (sql/with-connection
    (get-spec)
    (sql/transaction
      (f))))

(defn create-database
  []
  (invoke-with-connection create-tables))