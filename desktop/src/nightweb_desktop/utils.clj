(ns nightweb-desktop.utils
  (:require [ring.util.codec :as codec]
            [clojure.java.io :as java.io]
            [clojure.xml :as xml]
            [nightweb.constants :as c]
            [nightweb.formats :as f]
            [nightweb.io :as io]))

(def strings (-> (java.io/resource "strings.xml")
                 (.toString)
                 (xml/parse)
                 (:content)))

(defn get-string
  "Returns the localized string for the given keyword."
  [res-name]
  (if (keyword? res-name)
    (-> (filter #(= (get-in % [:attrs :name]) (name res-name))
                strings)
        first
        :content
        first
        (or "")
        (clojure.string/replace "\\" ""))
    res-name))

(defn get-pic
  "Returns the path to the pic"
  ([pic-hash] (get-pic @c/my-hash-bytes pic-hash))
  ([user-hash pic-hash]
   (when (and user-hash pic-hash)
     (str (c/get-pic-dir (f/base32-encode user-hash))
          c/slash
          (f/base32-encode pic-hash)
          ; add a non-existent file-extension so WebPJS works
          ".webp"))))

(defn get-version
  "Returns version number from project.clj."
  []
  (let [project-clj (-> (java.io/resource "project.clj")
                        (slurp)
                        (read-string))]
    (if (= (name (nth project-clj 1)) "nightweb-desktop")
      (nth project-clj 2)
      nil)))

(defn pic-to-data-uri
  "Converts the pic from the given pic hash to a data url."
  ([pic-hash] (pic-to-data-uri @c/my-hash-bytes pic-hash))
  ([user-hash pic-hash]
   (when (and user-hash pic-hash)
     (let [pic-dir (str (c/get-pic-dir (f/base32-encode user-hash))
                        c/slash
                        (f/base32-encode pic-hash))]
       (when-let [pic (io/read-file pic-dir)]
         (str "image/webp;base64," (codec/base64-encode pic)))))))

(defn decode-values
  "Decodes the values of the given map."
  [m]
  (into {} (for [[k v] m]
             [k (codec/form-decode-str v)])))

(defn decode-data-uri
  [uri-str]
  (when uri-str
    (->> (+ 1 (.indexOf uri-str ","))
         (subs uri-str)
         codec/base64-decode)))
