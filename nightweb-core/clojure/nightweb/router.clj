(ns nightweb.router
  (:import net.i2p.router.RouterLaunch
           java.lang.System))

(defn start-router
  "Launch the router."
  [context]
  (let [dir (.getAbsolutePath (.getFilesDir context))]
    (System/setProperty "i2p.dir.base" dir)
    (System/setProperty "i2p.dir.config" dir)
    (System/setProperty "wrapper.logfile" (str dir "/wrapper.log"))
    (RouterLaunch/main nil)))
