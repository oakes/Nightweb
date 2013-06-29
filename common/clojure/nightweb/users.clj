(ns nightweb.users
  (:require [clojure.java.io :as java.io]
            [nightweb.constants :as c]
            [nightweb.crypto :as crypto]
            [nightweb.io :as io]
            [nightweb.formats :as f]
            [nightweb.torrents :as t]
            [nightweb.torrents-dht :as dht])
  (:import [java.io File]
           [java.util Arrays]))

(defn user-exists?
  "Checks if we are following this user."
  [user-hash-bytes]
  (let [user-hash-str (f/base32-encode user-hash-bytes)]
    (or (io/file-exists? (c/get-user-dir user-hash-str))
        (c/is-me? user-hash-bytes true))))

(defn user-has-content?
  "Checks if we've received anything from this user."
  [user-hash-bytes]
  (let [user-hash-str (f/base32-encode user-hash-bytes)
        meta-torrent-file (c/get-meta-torrent-file user-hash-str)
        meta-dir (c/get-meta-dir user-hash-str)]
    (or (io/file-exists? meta-torrent-file)
        (io/file-exists? meta-dir)
        (c/is-me? user-hash-bytes true))))

(defn add-user-and-meta-torrents
  "Starts the user and meta torrent for this user."
  [their-hash-str]
  (let [their-hash-bytes (f/base32-decode their-hash-str)
        user-dir (c/get-user-dir their-hash-str)
        pub-path (c/get-user-pub-file their-hash-str)
        pub-torrent-path (c/get-user-pub-torrent-file their-hash-str)
        meta-path (c/get-meta-dir their-hash-str)
        meta-torrent-path (c/get-meta-torrent-file their-hash-str)
        meta-link-path (c/get-meta-link-file their-hash-str)
        link-map (when (io/file-exists? meta-link-path)
                   (-> (io/read-file meta-link-path)
                       (f/b-decode)
                       (f/b-decode-map)
                       (dht/parse-meta-link)))]
    ; add user torrent
    (if (or (c/is-me? their-hash-bytes true)
            (io/file-exists? pub-torrent-path))
      (t/add-torrent pub-path true dht/send-meta-link)
      (t/add-hash user-dir their-hash-str true dht/send-meta-link))
    ; add meta torrent
    (if (io/file-exists? meta-torrent-path)
      (t/add-torrent meta-path false dht/on-recv-meta)
      (when-let [new-link-str (:link-hash-str link-map)]
        (t/add-hash user-dir new-link-str false dht/on-recv-meta)))))

(defn create-user
  "Creates a new user."
  []
  (crypto/load-user-keys nil)
  ; temporarily write pub key to the root dir
  (io/write-key-file (c/get-user-pub-file nil) @crypto/pub-key)
  (let [info-hash (t/get-info-hash (c/get-user-pub-file nil))
        info-hash-str (f/base32-encode info-hash)]
    ; delete pub key from root, save keys in user dir, and save user list
    (io/delete-file (c/get-user-pub-file nil))
    (io/write-key-file (c/get-user-priv-file info-hash-str) @crypto/priv-key)
    (io/write-key-file (c/get-user-pub-file info-hash-str) @crypto/pub-key)
    (io/write-user-list-file (cons info-hash @c/my-hash-list))
    (add-user-and-meta-torrents info-hash-str)
    info-hash))

(defn load-user
  "Loads user into memory."
  [user-hash-bytes]
  (let [user-list (io/read-user-list-file)
        user-hash (or user-hash-bytes (get user-list 0))
        user-hash-str (f/base32-encode user-hash)
        priv-key-path (c/get-user-priv-file user-hash-str)
        pub-key-path (c/get-user-pub-file user-hash-str)
        priv-key-bytes (io/read-key-file priv-key-path)]
    (crypto/load-user-keys priv-key-bytes)
    (reset! c/my-hash-bytes user-hash)
    (reset! c/my-hash-str user-hash-str)
    (reset! c/my-hash-list user-list))
  nil)

(defn delete-user
  "Removes user permanently."
  [user-hash-bytes]
  (let [user-list (remove #(Arrays/equals ^bytes user-hash-bytes ^bytes %)
                          @c/my-hash-list)]
    (io/write-user-list-file (if (= 0 (count user-list))
                             (cons (create-user) user-list)
                             user-list))
    (load-user nil)
    (future (dht/remove-user-hash user-hash-bytes))))

(defn create-imported-user
  "Replaces current user with imported user."
  [imported-user-str]
  (let [imported-user (f/base32-decode imported-user-str)
        is-valid? (and imported-user-str
                       imported-user
                       (not (c/is-me? imported-user true)))]
    (when is-valid?
      (io/write-user-list-file (cons imported-user @c/my-hash-list))
      (load-user imported-user)
      (doseq [^File f (-> (c/get-meta-dir imported-user-str)
                          java.io/file
                          file-seq)]
        (when (.isFile f)
          (dht/on-recv-meta-file imported-user
                                 (io/read-meta-file (.getCanonicalPath f)))))
      (add-user-and-meta-torrents imported-user-str))
    is-valid?))
