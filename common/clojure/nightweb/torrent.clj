(ns nightweb.torrent
  (:use [clojure.java.io :only [file input-stream]]
        [nightweb.io :only [base32-encode]]))

(def manager nil)

(defn start-torrent-manager
  []
  (let [context (net.i2p.I2PAppContext/getGlobalContext)]
    (def manager (org.klomp.snark.SnarkManager. context))
    (.updateConfig manager
                   nil ;dataDir
                   true ;filesPublic
                   true ;autoStart
                   nil ;refreshDelay
                   nil ;startDelay
                   nil ;seedPct
                   nil ;eepHost
                   nil ;eepPort
                   nil ;i2cpHost
                   nil ;i2cpPort
                   nil ;i2cpOps
                   nil ;upLimit
                   nil ;upBW
                   false ;useOpenTrackers
                   true ;useDHT
                   nil) ;theme
    (.start manager false)))

(defn add-torrent
  [info-hash]
  (future
    (try
      (.addMagnet manager
                  (base32-encode info-hash)
                  info-hash
                  nil
                  false
                  true
                  (reify org.klomp.snark.CompleteListener
                    (torrentComplete [this snark]
                      (println "torrentComplete"))
                    (updateStatus [this snark]
                      (println "updateStatus"))
                    (gotMetaInfo [this snark]
                      (println "gotMetaInfo"))
                    (fatal [this snark error]
                      (println "fatal" error))
                    (addMessage [this snark message]
                      (println "addMessage" message))
                    (gotPiece [this snark]
                      (println "gotPiece"))
                    (getSavedTorrentTime [this snark]
                      (println "getSavedTorrentTime"))
                    (getSavedTorrentBitField [this snark]
                      (println "getSavedTorrentBitField")))
                  "/sdcard/Download")
      (catch IllegalArgumentException iae
        (println "Invalid info hash")))))

(defn create-torrent
  ([path] (create-torrent path true))
  ([path overwrite?]
   (try
     (let [base-file (file path)
           root-path (.getParent base-file)
           torrent-file (file root-path (str (.getName base-file) ".torrent"))
           torrent-path (.getAbsolutePath torrent-file)
           listener (reify org.klomp.snark.StorageListener
                      (storageCreateFile [this storage file-name length]
                        (println "storageCreateFile" file-name))
                      (storageAllocated [this storage length]
                        (println "storageAllocated" length))
                      (storageChecked [this storage piece-num checked]
                        (println "storageChecked" piece-num))
                      (storageAllChecked [this storage]
                        (println "storageAllChecked"))
                      (storageCompleted [this storage]
                        (println "storageCompleted"))
                      (setWantedPieces [this storage]
                        (println "setWantedPieces"))
                      (addMessage [this message]
                        (println "addMessage" message)))
           storage (if (and (not overwrite?) (.exists torrent-file))
                     (org.klomp.snark.Storage.
                       (.util manager)
                       (org.klomp.snark.MetaInfo. (input-stream torrent-path))
                       listener)
                     (org.klomp.snark.Storage.
                       (.util manager) base-file nil false listener))
           _ (.close storage)
           meta-info (.getMetaInfo storage)
           bit-field (.getBitField storage)]
       (future
         (when (and overwrite? (.exists torrent-file))
           (.stopTorrent manager torrent-path true)
           (.delete torrent-file))
         (.addTorrent manager
                      meta-info
                      bit-field
                      torrent-path
                      false
                      root-path)
         (println "Torrent created for" path "at" torrent-path))
       (.getInfoHash meta-info))
     (catch java.io.IOException ioe
       (println "Error creating torrent for" path)
       nil))))
