(ns nightweb.constants)

(def slash java.io.File/separator)
(def nw-dir (str slash "nwapp"))

(def priv-key "private.key")
(def pub-key "public.key")

(def torrent-ext ".torrent")
(def post-ext ".post")
(def link-ext ".link")

(def priv-key-file (str nw-dir slash priv-key))
(def pub-key-file (str nw-dir slash pub-key))
(def profile-file (str nw-dir slash "user.profile"))
(def db-file (str nw-dir slash "main"))
(def users-dir (str nw-dir slash "users"))

(defn get-user-dir
  [user-hash]
  (str users-dir slash user-hash))

(defn get-meta-dir
  [user-hash]
  (str (get-user-dir user-hash) slash "meta"))

(defn get-posts-dir
  [user-hash]
  (str (get-meta-dir user-hash) slash "posts"))

(defn get-files-dir
  [user-hash]
  (str (get-meta-dir user-hash) slash "files"))
