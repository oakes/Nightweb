(ns nightweb.constants
  (:require [clojure.java.io :as java.io]))

(def base-dir (atom nil))
(def my-hash-bytes (atom nil))
(def my-hash-str (atom nil))
(def my-hash-list (atom []))

(def ^:const nw-dir "nwapp")
(def ^:const meta-dir "meta")
(def ^:const post-dir "post")
(def ^:const pic-dir "pic")
(def ^:const fav-dir "fav")

(def ^:const priv-key "private.key")
(def ^:const pub-key "public.key")
(def ^:const profile "user.profile")
(def ^:const user-zip-file "nightweb_user.zip")

(def ^:const torrent-ext ".torrent")
(def ^:const link-ext ".link")

(def ^:const user-list-file "user.list")
(def ^:const priv-node-key-file "private.node.key")
(def ^:const pub-node-key-file "public.node.key")
(def ^:const db-file "main")
(def ^:const user-dir "user")

(defn is-me?
  ([user-hash]
   (is-me? user-hash false))
  ([user-hash all-my-users?]
   (if all-my-users?
     (-> (filter #(java.util.Arrays/equals ^bytes user-hash ^bytes %)
                 @my-hash-list)
         (count)
         (> 0))
     (java.util.Arrays/equals ^bytes user-hash ^bytes @my-hash-bytes))))

(defn get-user-list-file
  []
  (.getCanonicalPath (java.io/file @base-dir nw-dir user-list-file)))

(defn get-user-dir
  ([]
   (.getCanonicalPath (java.io/file @base-dir nw-dir user-dir)))
  ([user-hash]
   (.getCanonicalPath (java.io/file @base-dir nw-dir user-dir user-hash))))

(defn get-user-priv-file
  [user-hash]
  (.getCanonicalPath (java.io/file (get-user-dir user-hash) priv-key)))

(defn get-user-pub-file
  [user-hash]
  (if user-hash
    (.getCanonicalPath (java.io/file (get-user-dir user-hash) pub-key))
    (.getCanonicalPath (java.io/file @base-dir nw-dir pub-key))))

(defn get-user-pub-torrent-file
  [user-hash]
  (.getCanonicalPath
    (java.io/file (get-user-dir user-hash) (str pub-key torrent-ext))))

(defn get-meta-dir
  [user-hash]
  (.getCanonicalPath (java.io/file (get-user-dir user-hash) meta-dir)))

(defn get-meta-torrent-file
  [user-hash]
  (.getCanonicalPath
    (java.io/file (get-user-dir user-hash) (str meta-dir torrent-ext))))

(defn get-meta-link-file
  [user-hash]
  (.getCanonicalPath
    (java.io/file (get-user-dir user-hash) (str meta-dir link-ext))))

(defn get-post-dir
  [user-hash]
  (.getCanonicalPath (java.io/file (get-meta-dir user-hash) post-dir)))

(defn get-pic-dir
  [user-hash]
  (.getCanonicalPath (java.io/file (get-meta-dir user-hash) pic-dir)))

(defn get-fav-dir
  [user-hash]
  (.getCanonicalPath (java.io/file (get-meta-dir user-hash) fav-dir)))
