(ns nightweb.router
  (:use [nightweb.crypto :only [create-keys]]
        [nightweb.io :only [base32-encode]]
        [nightweb.constants :only [get-meta-dir]]
        [nightweb.torrent :only [start-torrent-manager create-torrent]]))

(def base-dir nil)
(def user-hash nil)

(defn start-router
  [dir]
  (def base-dir dir)
  (java.lang.System/setProperty "i2p.dir.base" dir)
  (java.lang.System/setProperty "i2p.dir.config" dir)
  (java.lang.System/setProperty "wrapper.logfile" (str dir "/wrapper.log"))
  (net.i2p.router.RouterLaunch/main nil)
  (start-torrent-manager)
  (let [pub-key-path (create-keys dir)
        info-hash (create-torrent pub-key-path false)]
    (def user-hash (base32-encode info-hash))))

(defn stop-router
  []
  (if-let [contexts (net.i2p.router.RouterContext/listContexts)]
    (if-not (.isEmpty contexts)
      (if-let [context (.get contexts 0)]
        (.shutdown (.router context) net.i2p.router.Router/EXIT_HARD)))))

(defn create-meta-torrent
  []
  (create-torrent (str base-dir (get-meta-dir user-hash))))
