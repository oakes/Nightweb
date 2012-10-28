(ns nightweb.router
  (:import net.i2p.router.RouterLaunch
           net.i2p.I2PAppContext
           org.klomp.snark.SnarkManager
           java.lang.System))

(defn start-router
  "Launch the router."
  [context]
  (let [dir (.getAbsolutePath (.getFilesDir context))]
    (System/setProperty "i2p.dir.base" dir)
    (System/setProperty "i2p.dir.config" dir)
    (System/setProperty "wrapper.logfile" (str dir "/wrapper.log"))
    (RouterLaunch/main nil)))

(defn start-download-manager
  "Launch the download manager."
  []
  (let [context (I2PAppContext/getGlobalContext)
        manager (SnarkManager. context)
        config-file "download.config"]
    (.loadConfig manager config-file)
    (.start manager)
    manager))