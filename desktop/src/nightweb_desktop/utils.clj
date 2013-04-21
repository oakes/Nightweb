(ns nightweb-desktop.utils
  (:use [clojure.java.io :only [resource]]
        [clojure.xml :only [parse]]))

(def strings (-> (resource "strings.xml")
                 (.getFile)
                 (parse)
                 (get :content)))

(defn get-string
  [res-name]
  (if (keyword? res-name)
    (-> (filter #(= (get-in % [:attrs :name]) (name res-name))
                strings)
        (first)
        (get :content)
        (first))
    res-name))

