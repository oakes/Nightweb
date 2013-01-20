(ns nightweb.router
  (:use [nightweb.crypto :only [create-keys create-signature]]
        [nightweb.io :only [base32-encode write-link-file]]
        [nightweb.constants :only [slash get-meta-dir]]
        [nightweb.torrent :only [start-torrent-manager create-torrent]]))

(def base-dir nil)
(def user-hash nil)
(def user-hash-bytes nil)

(defn create-user-torrent
  []
  (let [pub-key-path (create-keys base-dir)
        info-hash (create-torrent pub-key-path false)]
    (def user-hash (base32-encode info-hash))
    (def user-hash-bytes info-hash)))

(defn create-meta-torrent
  []
  (let [path (str base-dir (get-meta-dir user-hash))
        info-hash (create-torrent path)]
    (write-link-file path info-hash (fn [data] (create-signature data)))))

(defn start-router
  [dir]
  (def base-dir dir)
  (java.lang.System/setProperty "i2p.dir.base" dir)
  (java.lang.System/setProperty "i2p.dir.config" dir)
  (java.lang.System/setProperty "wrapper.logfile" (str dir slash "wrapper.log"))
  (net.i2p.router.RouterLaunch/main nil)
  (start-torrent-manager)
  (create-user-torrent))

(defn stop-router
  []
  (if-let [contexts (net.i2p.router.RouterContext/listContexts)]
    (if-not (.isEmpty contexts)
      (if-let [context (.get contexts 0)]
        (.shutdown (.router context) net.i2p.router.Router/EXIT_HARD)))))
