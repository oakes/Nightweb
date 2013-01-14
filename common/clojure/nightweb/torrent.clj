(ns nightweb.torrent
  (:use [nightweb.constants :only [files-dir]]))

(def dl-manager nil)

(defn start-download-manager
  []
  (let [context (net.i2p.I2PAppContext/getGlobalContext)]
    (def dl-manager (org.klomp.snark.SnarkManager. context))
    (.loadConfig dl-manager "i2psnark.config")
    (.start dl-manager)))

(defn add-download
  [url]
  (try
    (let [magnet (org.klomp.snark.MagnetURI. (.util dl-manager) url)
          magnet-name (.getName magnet)
          info-hash (.getInfoHash magnet)
          tracker-url (.getTrackerURL magnet)]
      (.addMagnet dl-manager magnet-name info-hash tracker-url false))
    (catch IllegalArgumentException iae
      (println (str "Invalid magnet URL " url)))))

(defn create-download
  [path]
  (try
    (let [base-file (java.io.File. path)
          listener (reify org.klomp.snark.StorageListener
                     (storageCreateFile [this storage file-name length])
                     (storageAllocated [this storage length])
                     (storageChecked [this storage piece-num checked])
                     (storageAllChecked [this storage])
                     (storageCompleted [this storage])
                     (setWantedPieces [this storage])
                     (addMessage [this message]))
          storage (org.klomp.snark.Storage.
                    (.util dl-manager) base-file nil false listener)
          _ (.close storage)
          info (.getMetaInfo storage)
          torrent-file (java.io.File.
                         files-dir
                         (str (.getBaseName storage) ".torrent"))
          torrent-path (.getAbsolutePath torrent-file)]
      (.addTorrent dl-manager info (.getBitField storage) torrent-path true)
      (println (str "Torrent created for " path)))
    (catch java.io.IOException ioe
      (println (str "Error creating torrent for " path)))))
