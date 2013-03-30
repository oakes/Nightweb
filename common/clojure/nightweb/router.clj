(ns nightweb.router
  (:use [clojure.java.io :only [file]]
        [nightweb.crypto :only [priv-key
                                pub-key
                                load-user-keys]]
        [nightweb.io :only [file-exists?
                            read-file
                            delete-file
                            iterate-dir
                            write-key-file
                            read-key-file
                            write-link-file
                            read-user-list-file
                            write-user-list-file
                            write-fav-file
                            read-meta-file]]
        [nightweb.formats :only [base32-encode
                                 base32-decode
                                 b-decode
                                 b-decode-map
                                 b-decode-bytes
                                 fav-encode]]
        [nightweb.constants :only [is-me?
                                   set-base-dir
                                   my-hash-bytes
                                   my-hash-str
                                   set-my-hash-bytes
                                   set-my-hash-str
                                   slash
                                   torrent-ext
                                   link-ext
                                   get-user-dir
                                   get-user-priv-file
                                   get-user-pub-file
                                   get-meta-dir
                                   get-meta-torrent-file]]
        [nightweb.db :only [init-db]]
        [nightweb.torrents :only [start-torrent-manager
                                  get-torrent-by-path
                                  get-info-hash
                                  add-hash
                                  add-torrent
                                  remove-torrent]]
        [nightweb.torrents-dht :only [add-user-hash
                                      remove-user-hash
                                      on-recv-meta-file
                                      on-recv-meta
                                      send-meta-link
                                      parse-meta-link
                                      init-dht]]))

(def enable-router? false) ; if false, I2P won't boot (useful for testing)
(def is-first-boot? false)

(defn user-exists?
  "Checks if we are following this user."
  [user-hash-bytes]
  (-> (base32-encode user-hash-bytes)
      (get-user-dir)
      (file-exists?)
      (or (is-me? user-hash-bytes))))

(defn user-has-content?
  "Checks if we've received anything from this user."
  [user-hash-bytes]
  (-> (base32-encode user-hash-bytes)
      (get-meta-torrent-file)
      (file-exists?)
      (or (is-me? user-hash-bytes))))

(defn add-user-and-meta-torrents
  "Starts the user and meta torrent for this user."
  [their-hash-str]
  (let [user-dir (get-user-dir their-hash-str)
        pub-path (get-user-pub-file their-hash-str)
        pub-torrent-path (str pub-path torrent-ext)
        meta-path (get-meta-dir their-hash-str)
        meta-torrent-path (str meta-path torrent-ext)
        meta-link-path (str meta-path link-ext)
        link-map (when (file-exists? meta-link-path)
                   (-> (read-file meta-link-path)
                       (b-decode)
                       (b-decode-map)
                       (parse-meta-link)))]
    ; add user torrent
    (if (or (= my-hash-str their-hash-str)
            (file-exists? pub-torrent-path))
      (add-torrent pub-path true send-meta-link)
      (add-hash user-dir their-hash-str true send-meta-link))
    ; add meta torrent
    (if (file-exists? meta-torrent-path)
      (add-torrent meta-path false on-recv-meta)
      (when-let [new-link-str (get link-map :link-hash-str)]
        (add-hash user-dir new-link-str false on-recv-meta)))))

(defn create-new-user
  "Creates a new user."
  []
  (load-user-keys nil)
  ; temporarily write pub key to the root dir
  (write-key-file (get-user-pub-file nil) pub-key)
  (let [info-hash (get-info-hash (get-user-pub-file nil))
        info-hash-str (base32-encode info-hash)]
    ; delete pub key from root, save keys in user dir, and save user list
    (delete-file (get-user-pub-file nil))
    (write-key-file (get-user-priv-file info-hash-str) priv-key)
    (write-key-file (get-user-pub-file info-hash-str) pub-key)
    (write-user-list-file [info-hash])
    info-hash))

(defn create-or-load-user
  "Loads user into memory (creating new user if necessary)."
  []
  ; create keys if necessary
  (when (nil? (get (read-user-list-file) 0))
    (def is-first-boot? true)
    (create-new-user))
  ; load keys based on the first hash stored in the user list
  (let [user-hash (get (read-user-list-file) 0)
        user-hash-str (base32-encode user-hash)
        priv-key-path (get-user-priv-file user-hash-str)
        pub-key-path (get-user-pub-file user-hash-str)
        priv-key-bytes (read-key-file priv-key-path)]
    (load-user-keys priv-key-bytes)
    (set-my-hash-bytes user-hash)
    (set-my-hash-str user-hash-str)))

(defn create-imported-user
  "Replaces current user with imported user."
  [user-hash-str-list]
  (let [imported-user-str (first user-hash-str-list)
        imported-user (base32-decode imported-user-str)
        old-user-hash my-hash-bytes]
    (if (and imported-user-str imported-user)
      (do
        (write-user-list-file [imported-user])
        (create-or-load-user)
        (doseq [f (file-seq (file (get-meta-dir imported-user-str)))]
          (when (.isFile f)
            (on-recv-meta-file imported-user
                               (read-meta-file (.getAbsolutePath f)))))
        (when (not= imported-user-str (base32-encode old-user-hash))
          (add-user-and-meta-torrents imported-user-str)
          (remove-user-hash old-user-hash))
        true)
      false)))

(defn create-meta-torrent
  "Creates a new meta torrent."
  []
  (let [path (get-meta-dir my-hash-str)]
    (remove-torrent (str path torrent-ext))
    (write-link-file (add-torrent path false on-recv-meta))
    (send-meta-link)))

(defn start-router
  "Starts the I2P router, I2PSnark manager, and the user and meta torrents."
  [dir]
  ; set main dir and initialize the database
  (set-base-dir dir)
  (init-db dir)
  ; start i2psnark
  (start-torrent-manager)
  (init-dht)
  ; create or load keys
  (create-or-load-user)
  (future
    ; start i2p router
    (when enable-router?
      (java.lang.System/setProperty "i2p.dir.base" dir)
      (java.lang.System/setProperty "i2p.dir.config" dir)
      (java.lang.System/setProperty "wrapper.logfile" (str dir slash "wrapper.log"))
      (net.i2p.router.RouterLaunch/main nil)
      (java.lang.Thread/sleep 10000))
    ; add all user and meta torrents
    (iterate-dir (get-user-dir) add-user-and-meta-torrents)
    ; add default fav user
    (when is-first-boot?
      (let [user-hash-bytes (base32-decode "zc3bf63ca7p756p5lffnypyzbo53qtzb")]
        (write-fav-file (.getTime (java.util.Date.))
                        (fav-encode user-hash-bytes nil 1))
        (create-meta-torrent)))))

(defn stop-router
  "Shuts down the I2P router."
  []
  (when-let [contexts (net.i2p.router.RouterContext/listContexts)]
    (when-not (.isEmpty contexts)
      (when-let [context (.get contexts 0)]
        (.shutdown (.router context) net.i2p.router.Router/EXIT_HARD)))))
