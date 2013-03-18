(ns nightweb.router
  (:use [clojure.java.io :only [file]]
        [nightweb.crypto :only [priv-key
                                pub-key
                                load-user-keys]]
        [nightweb.io :only [file-exists?
                            read-file
                            delete-file
                            make-dir
                            iterate-dir
                            write-key-file
                            read-key-file
                            write-link-file
                            read-meta-file
                            read-user-list-file
                            write-user-list-file
                            delete-orphaned-files]]
        [nightweb.formats :only [base32-encode
                                 base32-decode
                                 b-decode
                                 b-decode-map
                                 b-decode-bytes]]
        [nightweb.db :only [insert-meta-data
                            get-single-fav-data]]
        [nightweb.constants :only [is-me?
                                   set-base-dir
                                   set-my-hash-bytes
                                   my-hash-str
                                   set-my-hash-str
                                   slash
                                   torrent-ext
                                   link-ext
                                   get-user-dir
                                   get-user-priv-file
                                   get-user-pub-file
                                   get-meta-dir
                                   get-meta-torrent-file]]
        [nightweb.torrents :only [start-torrent-manager
                                  get-info-hash
                                  add-hash
                                  add-torrent
                                  remove-torrent
                                  send-meta-link
                                  parse-meta-link]]))

(def is-first-boot? false)

(defn user-exists?
  "Checks if we are following this user."
  [user-hash-bytes]
  (-> (base32-encode user-hash-bytes)
      (get-user-dir)
      (file-exists?)))

(defn user-has-content?
  "Checks if we've received anything from this user."
  [user-hash-bytes]
  (-> (base32-encode user-hash-bytes)
      (get-meta-torrent-file)
      (file-exists?)))

(defn add-user-hash
  "Begins following the supplied user hash if we aren't already."
  [their-hash-bytes]
  (if their-hash-bytes
    (let [their-hash-str (base32-encode their-hash-bytes)
          path (get-user-dir their-hash-str)]
      (when-not (file-exists? path)
        (make-dir path)
        (add-hash path their-hash-str true send-meta-link)))))

(defn on-recv-meta
  "Performs various actions on a meta torrent that has finished downloading."
  [torrent]
  (let [parent-dir (.getParentFile (file (.getName torrent)))
        user-hash-bytes (base32-decode (.getName parent-dir))
        paths (.getFiles (.getMetaInfo torrent))
        is-fav-user? (-> {:userhash user-hash-bytes}
                         (get-single-fav-data)
                         (get :status)
                         (= 1))]
    ; iterate over the files in this torrent
    (doseq [path-leaves paths]
      (let [meta-file (read-meta-file parent-dir path-leaves)
            meta-contents (get meta-file :contents)]
        ; insert it into the db
        (insert-meta-data user-hash-bytes meta-file)
        ; if this is a fav user, start following their fav users
        (if (and is-fav-user?
                 (= "fav" (get meta-file :dir-name))
                 (nil? (get meta-contents "ptrtime")))
          (add-user-hash (b-decode-bytes (get meta-contents "ptrhash"))))))
    ; remove any files that the torrent no longer contains
    (if-not (is-me? user-hash-bytes)
      (delete-orphaned-files user-hash-bytes paths))))

(defn add-user-and-meta-torrents
  "Starts every user and meta torrent that we have."
  []
  ; iterate over everything in the user dir
  (iterate-dir (get-user-dir)
               (fn [their-hash-str]
                 (let [user-dir (get-user-dir their-hash-str)
                       pub-path (get-user-pub-file their-hash-str)
                       pub-torrent-path (str pub-path torrent-ext)
                       meta-path (get-meta-dir their-hash-str)
                       meta-torrent-path (str meta-path torrent-ext)
                       meta-link-path (str meta-path link-ext)
                       link-map (if (file-exists? meta-link-path)
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
                     (if-let [new-link-str (get link-map :link-hash-str)]
                       (add-hash user-dir new-link-str false on-recv-meta)))))))

(defn create-user-torrent
  "Creates our user keys and loads them into memory."
  []
  ; create keys if necessary
  (when (nil? (get (read-user-list-file) 0))
    (def is-first-boot? true)
    (load-user-keys nil)
    ; temporarily write pub key to the root dir
    (write-key-file (get-user-pub-file nil) pub-key)
    (let [info-hash (get-info-hash (get-user-pub-file nil))
          info-hash-str (base32-encode info-hash)]
      ; delete pub key from root, save keys in user dir, and save user list
      (delete-file (get-user-pub-file nil))
      (write-key-file (get-user-priv-file info-hash-str) priv-key)
      (write-key-file (get-user-pub-file info-hash-str) pub-key)
      (write-user-list-file [info-hash])))
  ; load keys based on the first hash stored in the user list
  (let [user-hash (get (read-user-list-file) 0)
        user-hash-str (base32-encode user-hash)
        priv-key-path (get-user-priv-file user-hash-str)
        pub-key-path (get-user-pub-file user-hash-str)
        priv-key-bytes (read-key-file priv-key-path)]
    (load-user-keys priv-key-bytes)
    user-hash))

(defn create-meta-torrent
  "Creates a new meta torrent."
  []
  (let [path (get-meta-dir my-hash-str)]
    (remove-torrent (str path torrent-ext))
    (write-link-file (add-torrent path false on-recv-meta true))
    (send-meta-link)))

(defn start-router
  "Starts the I2P router, I2PSnark manager, and the user and meta torrents."
  [dir]
  (set-base-dir dir)
  ; set I2P properties and launch
  (java.lang.System/setProperty "i2p.dir.base" dir)
  (java.lang.System/setProperty "i2p.dir.config" dir)
  (java.lang.System/setProperty "wrapper.logfile" (str dir slash "wrapper.log"))
  (net.i2p.router.RouterLaunch/main nil)
  ; start I2PSnark
  (start-torrent-manager)
  (java.lang.Thread/sleep 3000)
  ; create or load our keys and start all user/meta torrents
  (when-let [user-hash (create-user-torrent)]
    (set-my-hash-bytes user-hash)
    (set-my-hash-str (base32-encode user-hash))
    (add-user-and-meta-torrents)))

(defn stop-router
  "Shuts down the I2P router."
  []
  (if-let [contexts (net.i2p.router.RouterContext/listContexts)]
    (if-not (.isEmpty contexts)
      (if-let [context (.get contexts 0)]
        (.shutdown (.router context) net.i2p.router.Router/EXIT_HARD)))))
