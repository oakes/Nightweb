(ns nightweb.torrent
  (:use [clojure.java.io :only [file input-stream]]
        [nightweb.io :only [file-exists?
                            base32-decode]]
        [nightweb.constants :only [torrent-ext]]))

(def manager nil)
(def torrent-complete-callback nil)

(defn start-torrent-manager
  [base-dir callback]
  (let [context (net.i2p.I2PAppContext/getGlobalContext)]
    (def manager (org.klomp.snark.SnarkManager. context))
    (def torrent-complete-callback callback)
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

(defn get-torrent-paths
  []
  (.listTorrentFiles manager))

(defn get-torrent-by-path
  [path]
  (.getTorrent manager path))

(defn iterate-torrents
  [func]
  (doseq [path (get-torrent-paths)]
    (if-let [torrent (get-torrent-by-path path)]
      (func torrent))))

(defn iterate-peers
  [torrent func]
  (doseq [peer (.getPeerList torrent)]
    (func peer)))

(defn get-storage
  [path]
  (let [listener (reify org.klomp.snark.StorageListener
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
        torrent-path (str path torrent-ext)
        storage (if (file-exists? torrent-path)
                  (org.klomp.snark.Storage.
                    (.util manager)
                    (org.klomp.snark.MetaInfo. (input-stream torrent-path))
                    listener)
                  (org.klomp.snark.Storage.
                    (.util manager) (file path) nil false listener))]
    (.close storage)
    storage))

(defn get-listener
  [path]
  (reify org.klomp.snark.CompleteListener
    (torrentComplete [this snark]
      (println "torrentComplete")
      (.torrentComplete manager snark)
      (torrent-complete-callback snark))
    (updateStatus [this snark]
      (println "updateStatus")
      (.updateStatus manager snark))
    (gotMetaInfo [this snark]
      (println "gotMetaInfo")
      (.gotMetaInfo manager snark path))
    (fatal [this snark error]
      (println "fatal" error)
      (.fatal manager snark error))
    (addMessage [this snark message]
      (println "addMessage" message)
      (.addMessage manager snark message))
    (gotPiece [this snark]
      (println "gotPiece")
      (.gotPiece manager snark))
    (getSavedTorrentTime [this snark]
      (println "getSavedTorrentTime")
      ;(.getSavedTorrentTime manager snark)
      0)
    (getSavedTorrentBitField [this snark]
      (println "getSavedTorrentBitField")
      ;(.getSavedTorrentBitField manager snark)
      nil)))

(defn add-hash
  [path info-hash-str persistent?]
  (future
    (try
      (.addMagnet manager
                  info-hash-str
                  (base32-decode info-hash-str)
                  nil
                  false
                  true
                  (get-listener path)
                  path)
      (if-let [torrent (get-torrent-by-path info-hash-str)]
        (.setPersistent torrent persistent?))
      (println "Hash added to" path)
      (catch IllegalArgumentException iae
        (println "Error adding hash:" (.getMessage iae))))))

(defn add-torrent
  [path persistent?]
  (try
    (let [base-file (file path)
          root-path (.getParent base-file)
          torrent-file (file (str path torrent-ext))
          torrent-path (.getCanonicalPath torrent-file)
          storage (get-storage path)
          meta-info (.getMetaInfo storage)
          bit-field (.getBitField storage)]
      (future
        (.addTorrent manager
                     meta-info
                     bit-field
                     torrent-path
                     false
                     (get-listener root-path)
                     root-path)
        (if-let [torrent (get-torrent-by-path torrent-path)]
          (.setPersistent torrent persistent?))
        (println "Torrent added to" torrent-path))
      (.getInfoHash meta-info))
    (catch java.io.IOException ioe
      (println "Error adding torrent:" (.getMessage ioe))
      nil)))

(defn remove-torrent
  [path]
  (when path
    (.stopTorrent manager path true)
    (if (file-exists? path)
      (.delete (file path)))))
