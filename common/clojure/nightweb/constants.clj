(ns nightweb.constants)

(def sep java.io.File/separator)

(def nw-dir (str sep "nwapp"))
(def priv-nkey-file (str nw-dir sep "private.nightkey"))
(def db-file (str nw-dir sep "main"))
(def users-dir (str nw-dir sep "users"))
(def files-dir (str nw-dir sep "files"))
