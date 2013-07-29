(ns nightweb-desktop.utils
  (:require [clojure.java.io :as java.io]
            [clojure.xml :as xml]
            [nightweb.constants :as c]
            [nightweb.formats :as f]
            [nightweb.io :as io]
            [ring.util.codec :as codec])
  (:import [java.util Locale]))

; preferences

(def prefs (.node (java.util.prefs.Preferences/userRoot) "nightweb"))

(defn write-pref
  [k v]
  (.put prefs (name k) (pr-str v)))

(defn read-pref
  [k]
  (when-let [string (.get prefs (name k) nil)]
    (read-string string)))

(def update? (atom (read-pref :update)))
(def remote? (atom (read-pref :remote)))

(when (nil? @update?)
  (write-pref :update true)
  (reset! update? true))

; language

(def lang-files {"en" "values/strings.xml"
                 "de" "values-de/strings.xml"
                 "fr" "values-fr/strings.xml"
                 "ja" "values-ja/strings.xml"})
(def lang-strings (-> (get lang-files (.getLanguage (Locale/getDefault)))
                      (or (get lang-files "en"))
                      java.io/resource
                      .toString
                      xml/parse
                      :content))

(defn get-string
  "Returns the localized string for the given keyword."
  [res-name]
  (if (keyword? res-name)
    (-> (filter #(= (get-in % [:attrs :name]) (name res-name))
                lang-strings)
        first
        :content
        first
        (or "")
        (clojure.string/replace "\\" ""))
    res-name))

; paths and encodings

(defn get-relative-path
  "Gets the path of a child relative to its parent."
  [parent-path child-path]
  (-> (.toURI (java.io/file parent-path))
      (.relativize (.toURI (java.io/file child-path)))
      (.getPath)))

(defn get-pic
  "Returns the path to the pic"
  ([pic-hash] (get-pic @c/my-hash-bytes pic-hash))
  ([user-hash pic-hash]
   (when (and user-hash pic-hash)
     (str (->> (c/get-pic-dir (f/base32-encode user-hash))
               (get-relative-path @c/base-dir))
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
      "beta")))

(defn pic-to-data-uri
  "Converts the pic from the given pic hash to a data url."
  ([pic-hash] (pic-to-data-uri @c/my-hash-bytes pic-hash))
  ([user-hash pic-hash]
   (when (and user-hash pic-hash)
     (let [pic-dir (-> (c/get-pic-dir (f/base32-encode user-hash))
                       (java.io/file (f/base32-encode pic-hash))
                       .getCanonicalPath)]
       (when-let [pic (io/read-file pic-dir)]
         (str "image/webp;base64," (codec/base64-encode pic)))))))

(defn decode-values
  "Decodes the values of the given map."
  [m]
  (into {} (for [[k v] m]
             [k (codec/form-decode-str v)])))

(defn decode-data-uri
  "Gets the binary equivalent of a base64-encoded data URI."
  [uri-str]
  (when uri-str
    (->> (+ 1 (.indexOf uri-str ","))
         (subs uri-str)
         codec/base64-decode)))
