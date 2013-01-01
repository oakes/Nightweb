(ns nightweb.router)

(defn start-router
  [context]
  (let [dir (.getAbsolutePath (.getFilesDir context))]
    (java.lang.System/setProperty "i2p.dir.base" dir)
    (java.lang.System/setProperty "i2p.dir.config" dir)
    (java.lang.System/setProperty "wrapper.logfile" (str dir "/wrapper.log"))
    (net.i2p.router.RouterLaunch/main nil)))

(defn stop-router
  []
  (if-let [contexts (net.i2p.router.RouterContext/listContexts)]
    (if (not (.isEmpty contexts))
      (if-let [context (.get contexts 0)]
        (.shutdown (.router context) net.i2p.router.Router/EXIT_HARD)))))

(defn start-download-manager
  []
  (let [context (net.i2p.I2PAppContext/getGlobalContext)
        manager (org.klomp.snark.SnarkManager. context)
        config-file "i2psnark.config"]
    (.loadConfig manager config-file)
    (.start manager)
    manager))
