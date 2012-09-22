(ns nightweb.db
  (:require [clojure.java.jdbc :as sql]))

(def dbspec
  {:classname "org.h2.Driver"
   :subprotocol "h2"
   :subname "main"})

(defn invoke-with-connection
  [f]
  (sql/with-connection
    dbspec
    (sql/transaction
      (f))))

(defn create-tables
  []
  (sql/create-table
    "websites"
    [:id "IDENTITY" "NOT NULL" "PRIMARY KEY"]))

(defn create-database
  []
  (invoke-with-connection create-tables))