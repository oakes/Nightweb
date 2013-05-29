(ns nightweb.torrents
  (:use [clojure.java.io :only [file input-stream]]
        [nightweb.io :only [file-exists?]]
        [nightweb.formats :only [base32-decode]]
        [nightweb.constants :only [torrent-ext]]))

(def manager (atom nil))

; active torrents

(defn get-torrent-paths
  []
  (.listTorrentFiles @manager))

(defn get-torrent-by-path
  [path]
  (.getTorrent @manager path))

(defn iterate-torrents
  [func]
  (doseq [path (get-torrent-paths)]
    (when-let [torrent (get-torrent-by-path path)]
      (func torrent))))

(defn iterate-peers
  [torrent func]
  (doseq [peer (.getPeerList torrent)]
    (func peer)))

; starting and stopping torrents

(defn get-storage
  "Creates a Storage object with a listener for each storage-related event."
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
                    (.util @manager)
                    (org.klomp.snark.MetaInfo. (input-stream torrent-path))
                    listener)
                  (org.klomp.snark.Storage.
                    (.util @manager) (file path) nil nil false listener))]
    (.close storage)
    storage))

(defn get-complete-listener
  "Creates a listener for each event in a given torrent download."
  [path complete-callback]
  (reify org.klomp.snark.CompleteListener
    (torrentComplete [this snark]
      (println "torrentComplete")
      (.torrentComplete @manager snark)
      (complete-callback snark))
    (updateStatus [this snark]
      (println "updateStatus")
      (.updateStatus @manager snark))
    (gotMetaInfo [this snark]
      (println "gotMetaInfo")
      (.gotMetaInfo @manager snark path))
    (fatal [this snark error]
      (println "fatal" error)
      (.fatal @manager snark error))
    (addMessage [this snark message]
      (println "addMessage" message)
      (.addMessage @manager snark message))
    (gotPiece [this snark]
      (println "gotPiece")
      (.gotPiece @manager snark))
    (getSavedTorrentTime [this snark]
      (println "getSavedTorrentTime")
      ;(.getSavedTorrentTime @manager snark)
      0)
    (getSavedTorrentBitField [this snark]
      (println "getSavedTorrentBitField")
      ;(.getSavedTorrentBitField @manager snark)
      nil)))

(defn add-hash
  "Adds an info hash to download."
  [path info-hash-str is-persistent? complete-callback]
  (future
    (try
      (.addMagnet @manager
                  info-hash-str
                  (base32-decode info-hash-str)
                  nil
                  false
                  true
                  (get-complete-listener path complete-callback)
                  path)
      (when-let [torrent (get-torrent-by-path info-hash-str)]
        (.setPersistent torrent is-persistent?))
      (println "Hash added to" path)
      (catch IllegalArgumentException iae
        (println "Error adding hash:" (.getMessage iae))))))

(defn add-torrent
  "Adds a torrent to download or seed."
  [path is-persistent? complete-callback]
  (try
    (let [base-file (file path)
          root-path (.getParent base-file)
          torrent-file (file (str path torrent-ext))
          torrent-path (.getCanonicalPath torrent-file)
          storage (get-storage path)
          meta-info (.getMetaInfo storage)
          bit-field (.getBitField storage)
          listener (get-complete-listener root-path complete-callback)]
      (future
        (.addTorrent @manager
                     meta-info
                     bit-field
                     torrent-path
                     false
                     listener
                     root-path)
        (when-let [torrent (get-torrent-by-path torrent-path)]
          (.setPersistent torrent is-persistent?))
        (println "Torrent added to" torrent-path))
      (.getInfoHash meta-info))
    (catch java.io.IOException ioe
      (println "Error adding torrent:" (.getMessage ioe))
      nil)))

(defn remove-torrent
  "Stops and deletes a torrent."
  [path]
  (.removeTorrent @manager path))

(defn get-info-hash
  "Gets the info hash for a given path."
  [path]
  (let [storage (get-storage path)
        meta-info (.getMetaInfo storage)]
    (.getInfoHash meta-info)))

; initialization

(defn start-torrent-manager
  "Starts the I2PSnark manager."
  []
  (let [context (net.i2p.I2PAppContext/getGlobalContext)]
    (reset! manager (org.klomp.snark.SnarkManager. context))
    (.updateConfig @manager
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
    (.start @manager false)))
