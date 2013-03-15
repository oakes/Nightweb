(ns nightweb.router
  (:use [nightweb.crypto :only [priv-key
                                pub-key
                                load-user-keys]]
        [nightweb.io :only [file-exists?
                            read-file
                            make-dir
                            iterate-dir
                            write-key-file
                            read-key-file
                            write-link-file]]
        [nightweb.formats :only [base32-encode
                                 base32-decode
                                 b-decode
                                 b-decode-map]]
        [nightweb.constants :only [set-base-dir
                                   my-hash-bytes
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
                                  add-hash
                                  add-torrent
                                  remove-torrent
                                  parse-meta-link]]))

(def is-first-boot? false)

(defn user-exists?
  [user-hash-bytes]
  (-> (base32-encode user-hash-bytes)
      (get-user-dir)
      (file-exists?)))

(defn user-has-content?
  [user-hash-bytes]
  (-> (base32-encode user-hash-bytes)
      (get-meta-torrent-file)
      (file-exists?)))

(defn add-user-hash
  [their-hash-bytes]
  (if their-hash-bytes
    (let [their-hash-str (base32-encode their-hash-bytes)
          path (get-user-dir their-hash-str)]
      (when-not (file-exists? path)
        (make-dir path)
        (add-hash path their-hash-str true)))))

(defn add-user-and-meta-torrents
  []
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
                   (if (not= their-hash-str my-hash-str)
                     (if (file-exists? pub-torrent-path)
                       (add-torrent pub-path true)
                       (add-hash user-dir their-hash-str true)))
                   ; add meta torrent
                   (if (file-exists? meta-torrent-path)
                     (add-torrent meta-path false)
                     (if-let [new-link-str (get link-map :link-hash-str)]
                       (add-hash user-dir new-link-str false)))))))

(defn create-user-torrent
  []
  (let [priv-key-path (get-user-priv-file)
        pub-key-path (get-user-pub-file)
        priv-key-bytes (read-key-file priv-key-path)]
    (load-user-keys priv-key-bytes)
    (when (nil? priv-key-bytes)
      (write-key-file priv-key-path priv-key)
      (write-key-file pub-key-path pub-key)
      (def is-first-boot? true))
    (add-torrent pub-key-path true)))

(defn create-meta-torrent
  []
  (let [path (get-meta-dir my-hash-str)]
    (remove-torrent (str path torrent-ext))
    (write-link-file (add-torrent path false true))))

(defn start-router
  [dir]
  (set-base-dir dir)
  (java.lang.System/setProperty "i2p.dir.base" dir)
  (java.lang.System/setProperty "i2p.dir.config" dir)
  (java.lang.System/setProperty "wrapper.logfile" (str dir slash "wrapper.log"))
  (net.i2p.router.RouterLaunch/main nil)
  (start-torrent-manager)
  (java.lang.Thread/sleep 3000)
  (set-my-hash-bytes (create-user-torrent))
  (set-my-hash-str (base32-encode my-hash-bytes))
  (add-user-and-meta-torrents))

(defn stop-router
  []
  (if-let [contexts (net.i2p.router.RouterContext/listContexts)]
    (if-not (.isEmpty contexts)
      (if-let [context (.get contexts 0)]
        (.shutdown (.router context) net.i2p.router.Router/EXIT_HARD)))))
