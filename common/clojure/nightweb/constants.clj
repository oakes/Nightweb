(ns nightweb.constants)

(def slash java.io.File/separator)

(def nw-dir (str slash "nwapp"))

(def priv-key-file (str slash "private.key"))
(def pub-key-file (str slash "public.key"))
(def db-file (str slash "main"))
(def users-dir (str slash "users"))
(def files-dir (str slash "files"))
