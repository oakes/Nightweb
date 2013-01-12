(ns nightweb.router
  (:use [nightweb.constants :only [priv-nkey-file]]
        [nightweb.crypto :only [create-key-if-not-exists]]))

(defn start-router
  [context]
  (let [dir (.getAbsolutePath (.getFilesDir context))]
    (java.lang.System/setProperty "i2p.dir.base" dir)
    (java.lang.System/setProperty "i2p.dir.config" dir)
    (java.lang.System/setProperty "wrapper.logfile" (str dir "/wrapper.log"))
    (net.i2p.router.RouterLaunch/main nil)
    (create-key-if-not-exists (str dir priv-nkey-file))))

(defn stop-router
  []
  (if-let [contexts (net.i2p.router.RouterContext/listContexts)]
    (if-not (.isEmpty contexts)
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

(defn add-download
  [manager url]
  (try
    (let [magnet (org.klomp.snark.MagnetURI. (.util manager) url)
          magnet-name (.getName magnet)
          info-hash (.getInfoHash magnet)
          tracker-url (.getTrackerURL magnet)]
      (.addMagnet manager magnet-name info-hash tracker-url true))
    (catch IllegalArgumentException iae
      (.addMessage manager (str "Invalid magnet URL " url)))))

(defn create-download
  [manager base-file-str]
  (try
    (let [base-file (java.io.File. (.getDataDir manager) base-file-str)
          storage (org.klomp.snark.Storage
                    (.util manager) base-file nil true nil)
          _ (.close storage)
          info (.getMetaInfo storage)
          torrent-file (java.io.File.
                         (.getDataDir manager)
                         (str (.getBaseName storage) ".torrent"))
          torrent-path (.getAbsolutePath torrent-file)]
      (.addTorrent manager info (.getBitField storage) torrent-path true)
      (.addMessage manager (str "Torrent created for " base-file-str)))
    (catch java.io.IOException ioe
      (.addMessage manager (str "Error creating torrent for " base-file-str)))))
