(ns nightweb.router
  (:require [clojure.java.io :as java.io]
            [nightweb.constants :as c]
            [nightweb.crypto :as crypto]
            [nightweb.db :as db]
            [nightweb.io :as io]
            [nightweb.formats :as f]
            [nightweb.torrents :as t]
            [nightweb.torrents-dht :as dht]))

(def ^:const enable-router? true) ; if false, I2P won't boot
(def is-first-boot? (atom false))

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
  (let [user-dir (c/get-user-dir their-hash-str)
        pub-path (c/get-user-pub-file their-hash-str)
        pub-torrent-path (str pub-path c/torrent-ext)
        meta-path (c/get-meta-dir their-hash-str)
        meta-torrent-path (str meta-path c/torrent-ext)
        meta-link-path (str meta-path c/link-ext)
        link-map (when (io/file-exists? meta-link-path)
                   (-> (io/read-file meta-link-path)
                       (f/b-decode)
                       (f/b-decode-map)
                       (dht/parse-meta-link)))]
    ; add user torrent
    (if (or (= @c/my-hash-str their-hash-str)
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
  (let [user-list (remove #(java.util.Arrays/equals user-hash-bytes %)
                          @c/my-hash-list)]
    (io/write-user-list-file (if (= 0 (count user-list))
                             (cons (create-user) user-list)
                             user-list))
    (load-user nil)
    (future (dht/remove-user-hash user-hash-bytes))))

(defn create-imported-user
  "Replaces current user with imported user."
  [user-hash-str-list]
  (let [imported-user-str (first user-hash-str-list)
        imported-user (f/base32-decode imported-user-str)
        is-valid? (and imported-user-str
                       imported-user
                       (not (c/is-me? imported-user true)))]
    (when is-valid?
      (io/write-user-list-file (cons imported-user @c/my-hash-list))
      (load-user imported-user)
      (doseq [f (file-seq (java.io/file (c/get-meta-dir imported-user-str)))]
        (when (.isFile f)
          (dht/on-recv-meta-file imported-user
                                 (io/read-meta-file (.getAbsolutePath f)))))
      (add-user-and-meta-torrents imported-user-str))
    is-valid?))

(defn create-meta-torrent
  "Creates a new meta torrent."
  []
  (let [path (c/get-meta-dir @c/my-hash-str)]
    (t/remove-torrent (str path c/torrent-ext))
    (io/write-link-file (t/add-torrent path false dht/on-recv-meta))
    (dht/send-meta-link)))

(defn get-router
  "Returns the router object if it exists."
  []
  (when-let [contexts (net.i2p.router.RouterContext/listContexts)]
    (when-not (.isEmpty contexts)
      (when-let [context (.get contexts 0)]
        (.router context)))))

(defn start-router
  "Starts the I2P router, I2PSnark manager, and the user and meta torrents."
  [dir hide?]
  ; set main dir and initialize the database
  (reset! c/base-dir dir)
  (db/init-db dir)
  ; start i2psnark
  (t/start-torrent-manager)
  (dht/init-dht)
  ; create or load user
  (when (= 0 (count (io/read-user-list-file)))
    (reset! is-first-boot? true)
    (create-user))
  (load-user nil)
  ; run the rest of the initialization in a separate thread
  (future
    ; start i2p router
    (when enable-router?
      (System/setProperty "i2p.dir.base" dir)
      (System/setProperty "i2p.dir.config" dir)
      (System/setProperty "wrapper.logfile" (str dir c/slash "wrapper.log"))
      (net.i2p.router.RouterLaunch/main nil)
      (when-let [router (get-router)]
        (.saveConfig router "router.hiddenMode" (if hide? "true" "false")))
      (Thread/sleep 10000))
    ; add all user and meta torrents
    (io/iterate-dir (c/get-user-dir) add-user-and-meta-torrents)
    ; add default fav user
    (when @is-first-boot?
      (let [user-hash-bytes
            (f/base32-decode "zc3bf63ca7p756p5lffnypyzbo53qtzb")]
        (io/write-fav-file (.getTime (java.util.Date.))
                           (f/fav-encode user-hash-bytes nil 1))
        (create-meta-torrent)))))

(defn stop-router
  "Shuts down the I2P router."
  []
  (when-let [router (get-router)]
    (.shutdown router net.i2p.router.Router/EXIT_HARD)))
