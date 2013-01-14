(ns nightweb.router
  (:use [nightweb.constants :only [priv-nkey-file]]
        [nightweb.crypto :only [create-priv-nkey]]
        [nightweb.torrent :only [start-download-manager]]))

(defn start-router
  [context]
  (let [dir (.getAbsolutePath (.getFilesDir context))]
    (java.lang.System/setProperty "i2p.dir.base" dir)
    (java.lang.System/setProperty "i2p.dir.config" dir)
    (java.lang.System/setProperty "wrapper.logfile" (str dir "/wrapper.log"))
    (net.i2p.router.RouterLaunch/main nil)
    (start-download-manager)
    (create-priv-nkey (str dir priv-nkey-file))))

(defn stop-router
  []
  (if-let [contexts (net.i2p.router.RouterContext/listContexts)]
    (if-not (.isEmpty contexts)
      (if-let [context (.get contexts 0)]
        (.shutdown (.router context) net.i2p.router.Router/EXIT_HARD)))))
