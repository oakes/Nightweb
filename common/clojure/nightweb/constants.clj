(ns nightweb.constants)

(def base-dir (atom nil))
(def my-hash-bytes (atom nil))
(def my-hash-str (atom nil))

(defn is-me?
  [user-hash]
  (java.util.Arrays/equals user-hash @my-hash-bytes))

(def ^:const slash java.io.File/separator)
(def ^:const nw-dir (str slash "nwapp"))
(def ^:const meta-dir (str slash "meta"))
(def ^:const post-dir (str slash "post"))
(def ^:const pic-dir (str slash "pic"))
(def ^:const fav-dir (str slash "fav"))

(def ^:const priv-key "private.key")
(def ^:const pub-key "public.key")
(def ^:const profile "user.profile")
(def ^:const user-zip-file "nightweb_user.zip")

(def ^:const torrent-ext ".torrent")
(def ^:const link-ext ".link")

(def ^:const user-list-file (str nw-dir slash "user.list"))
(def ^:const priv-node-key-file (str nw-dir slash "private.node.key"))
(def ^:const pub-node-key-file (str nw-dir slash "public.node.key"))
(def ^:const db-file (str nw-dir slash "main"))
(def ^:const user-dir (str nw-dir slash "user"))

(defn get-user-list-file
  []
  (str @base-dir user-list-file))

(defn get-user-dir
  ([] (str @base-dir user-dir))
  ([user-hash] (str @base-dir user-dir slash user-hash)))

(defn get-user-priv-file
  [user-hash]
  (str (get-user-dir user-hash) slash priv-key))

(defn get-user-pub-file
  [user-hash]
  (if user-hash
    (str (get-user-dir user-hash) slash pub-key)
    (str @base-dir nw-dir slash pub-key)))

(defn get-meta-dir
  [user-hash]
  (str (get-user-dir user-hash) meta-dir))

(defn get-meta-torrent-file
  [user-hash]
  (str (get-meta-dir user-hash) torrent-ext))

(defn get-meta-link-file
  [user-hash]
  (str (get-meta-dir user-hash) link-ext))

(defn get-post-dir
  [user-hash]
  (str (get-meta-dir user-hash) post-dir))

(defn get-pic-dir
  [user-hash]
  (str (get-meta-dir user-hash) pic-dir))

(defn get-fav-dir
  [user-hash]
  (str (get-meta-dir user-hash) fav-dir))
