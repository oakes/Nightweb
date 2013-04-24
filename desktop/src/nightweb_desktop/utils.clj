(ns nightweb-desktop.utils
  (:use [clojure.java.io :only [resource]]
        [clojure.xml :only [parse]]))

(def strings (-> (resource "strings.xml")
                 (.toString)
                 (parse)
                 (get :content)))

(defn get-string
  "Returns the localized string for the given keyword."
  [res-name]
  (if (keyword? res-name)
    (-> (filter #(= (get-in % [:attrs :name]) (name res-name))
                strings)
        (first)
        (get :content)
        (first))
    res-name))

(defn get-version
  "Returns version number from project.clj."
  []
  (let [project-clj (-> (resource "project.clj")
                        (slurp)
                        (read-string))]
    (if (= (name (nth project-clj 1)) "nightweb-desktop")
      (nth project-clj 2)
      nil)))
