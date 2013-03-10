(ns nightweb.constants)

(def base-dir nil)
(defn set-base-dir
  [dir]
  (def base-dir dir))

(def my-hash-bytes nil)
(defn set-my-hash-bytes
  [hash-bytes]
  (def my-hash-bytes hash-bytes))

(def my-hash-str nil)
(defn set-my-hash-str
  [hash-str]
  (def my-hash-str hash-str))

(defn is-me?
  [user-hash]
  (java.util.Arrays/equals user-hash my-hash-bytes))

(def slash java.io.File/separator)
(def nw-dir (str slash "nwapp"))
(def meta-dir (str slash "meta"))
(def post-dir (str slash "post"))
(def pic-dir (str slash "pic"))

(def priv-key "private.key")
(def pub-key "public.key")
(def profile "user.profile")

(def torrent-ext ".torrent")
(def link-ext ".link")

(def priv-key-file (str nw-dir slash priv-key))
(def pub-key-file (str nw-dir slash pub-key))
(def priv-node-key-file (str nw-dir slash "private.node.key"))
(def pub-node-key-file (str nw-dir slash "public.node.key"))
(def db-file (str nw-dir slash "main"))
(def user-dir (str nw-dir slash "user"))

(defn get-user-dir
  ([] (str base-dir user-dir))
  ([user-hash] (str base-dir user-dir slash user-hash)))

(defn get-user-priv-file
  ([] (str base-dir priv-key-file))
  ([user-hash] (str (get-user-dir user-hash) slash priv-key)))

(defn get-user-pub-file
  ([] (str base-dir pub-key-file))
  ([user-hash] (str (get-user-dir user-hash) slash pub-key)))

(defn get-meta-dir
  [user-hash]
  (str (get-user-dir user-hash) meta-dir))

(defn get-meta-link-file
  [user-hash]
  (str (get-meta-dir user-hash) link-ext))

(defn get-post-dir
  [user-hash]
  (str (get-meta-dir user-hash) post-dir))

(defn get-pic-dir
  [user-hash]
  (str (get-meta-dir user-hash) pic-dir))
