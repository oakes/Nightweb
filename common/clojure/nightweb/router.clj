(ns nightweb.router
  (:use [nightweb.constants :only [nw-dir]]
        [nightweb.crypto :only [create-keys]]
        [nightweb.torrent :only [start-download-manager create-download]]))

(defn start-router
  [context]
  (let [dir (.getAbsolutePath (.getFilesDir context))]
    (java.lang.System/setProperty "i2p.dir.base" dir)
    (java.lang.System/setProperty "i2p.dir.config" dir)
    (java.lang.System/setProperty "wrapper.logfile" (str dir "/wrapper.log"))
    (net.i2p.router.RouterLaunch/main nil)
    (start-download-manager)
    (create-download (create-keys (str dir nw-dir)))))

(defn stop-router
  []
  (if-let [contexts (net.i2p.router.RouterContext/listContexts)]
    (if-not (.isEmpty contexts)
      (if-let [context (.get contexts 0)]
        (.shutdown (.router context) net.i2p.router.Router/EXIT_HARD)))))
