(ns nightweb.router
  (:import net.i2p.router.Router
           net.i2p.router.RouterLaunch
           net.i2p.router.RouterContext
           net.i2p.I2PAppContext
           org.klomp.snark.SnarkManager
           java.lang.System))

(defn start-router
  [context]
  (let [dir (.getAbsolutePath (.getFilesDir context))]
    (System/setProperty "i2p.dir.base" dir)
    (System/setProperty "i2p.dir.config" dir)
    (System/setProperty "wrapper.logfile" (str dir "/wrapper.log"))
    (RouterLaunch/main nil)))

(defn stop-router
  []
  (if-let [contexts (RouterContext/listContexts)]
    (if (not (.isEmpty contexts))
      (let [context (.get contexts 0)]
        (.shutdown (.router context) Router/EXIT_HARD)))))

(defn start-download-manager
  []
  (let [context (I2PAppContext/getGlobalContext)
        manager (SnarkManager. context)
        config-file "i2psnark.config"]
    (.loadConfig manager config-file)
    (.start manager)
    manager))