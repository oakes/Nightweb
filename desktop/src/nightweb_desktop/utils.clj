(ns nightweb-desktop.utils
  (:use [ring.util.codec :only [base64-encode
                                base64-decode
                                form-decode-str]]
        [clojure.java.io :only [resource]]
        [clojure.xml :only [parse]]
        [nightweb.io :only [read-file]]
        [nightweb.formats :only [base32-encode]]
        [nightweb.constants :only [my-hash-bytes
                                   slash
                                   get-pic-dir]]))

(def strings (-> (resource "strings.xml")
                 (.toString)
                 (parse)
                 (:content)))

(defn get-string
  "Returns the localized string for the given keyword."
  [res-name]
  (if (keyword? res-name)
    (-> (filter #(= (get-in % [:attrs :name]) (name res-name))
                strings)
        (first)
        (:content)
        (first)
        (or "")
        (clojure.string/replace "\\" ""))
    res-name))

(defn get-pic
  "Returns the path to the pic"
  ([pic-hash] (get-pic @my-hash-bytes pic-hash))
  ([user-hash pic-hash]
   (when (and user-hash pic-hash)
     (str (get-pic-dir (base32-encode user-hash))
          slash
          (base32-encode pic-hash)
          ; add a non-existent file-extension so WebPJS works
          ".webp"))))

(defn get-version
  "Returns version number from project.clj."
  []
  (let [project-clj (-> (resource "project.clj")
                        (slurp)
                        (read-string))]
    (if (= (name (nth project-clj 1)) "nightweb-desktop")
      (nth project-clj 2)
      nil)))

(defn pic-to-data-uri
  "Converts the pic from the given pic hash to a data url."
  ([pic-hash] (pic-to-data-uri @my-hash-bytes pic-hash))
  ([user-hash pic-hash]
   (when (and user-hash pic-hash)
     (let [pic-dir (str (get-pic-dir (base32-encode user-hash))
                        slash
                        (base32-encode pic-hash))]
       (when-let [pic (read-file pic-dir)]
         (str "image/webp;base64," (base64-encode pic)))))))

(defn decode-values
  "Decodes the values of the given map."
  [m]
  (into {} (for [[k v] m]
             [k (form-decode-str v)])))

(defn decode-data-uri
  [uri-str]
  (when uri-str
    (->> (+ 1 (.indexOf uri-str ","))
         (subs uri-str)
         base64-decode)))
