(ns nightweb.torrent
  (:use [clojure.java.io :only [file input-stream]]
        [nightweb.io :only [base32-encode
                            read-priv-node-key-file
                            read-pub-node-key-file]]
        [nightweb.constants :only [torrent-ext]]))

(def manager nil)

(defn start-torrent-manager
  [base-dir]
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
    (let [priv-node (read-priv-node-key-file base-dir)
          pub-node (read-pub-node-key-file base-dir)]
      (if (and priv-node pub-node)
        (.setDHTNode (.util manager) priv-node pub-node)))
    (.start manager false)))

(defn get-torrent-paths
  []
  (.listTorrentFiles manager))

(defn get-torrent-by-path
  [path]
  (.getTorrent manager path))

(defn get-public-node
  []
  (if-let [dht (.getDHT (.util manager))]
    (.toPersistentString (.getNodeInfo dht))
    (println "Failed to get our public node")))

(defn get-private-node
  []
  (if-let [socket-manager (.getSocketManager (.util manager))]
    (.getSession socket-manager)
    (println "Failed to get our private node")))

(defn add-node
  [node-info-str]
  (if-let [dht (.getDHT (.util manager))]
    (.heardAbout dht (org.klomp.snark.dht.NodeInfo. node-info-str))
    (println "Failed to add bootstrap node")))

(defn add-hash
  [path info-hash-bytes persistent?]
  (future
    (try
      (let [info-hash-str (base32-encode info-hash-bytes)]
        (.addMagnet manager
                    info-hash-str
                    info-hash-bytes
                    nil
                    false
                    true
                    (reify org.klomp.snark.CompleteListener
                      (torrentComplete [this snark]
                        (println "torrentComplete")
                        (.torrentComplete manager snark))
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
                        (.getSavedTorrentTime manager snark))
                      (getSavedTorrentBitField [this snark]
                        (println "getSavedTorrentBitField")
                        (.getSavedTorrentBitField manager snark)))
                    path)
        (if-let [torrent (get-torrent-by-path info-hash-str)]
          (.setPersistent torrent persistent?))
        (println "Hash added to" path))
      (catch IllegalArgumentException iae
        (println "Error adding hash:" (.getMessage iae))))))

(defn add-torrent
  ([path persistent?] (add-torrent path persistent? nil))
  ([path persistent? func]
   (try
     (let [base-file (file path)
           root-path (.getParent base-file)
           torrent-file (file root-path (str (.getName base-file) torrent-ext))
           torrent-path (.getCanonicalPath torrent-file)
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
           storage (if (and (not persistent?) (.exists torrent-file))
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
         (when (and (not persistent?) (.exists torrent-file))
           (.stopTorrent manager torrent-path true)
           (.delete torrent-file))
         (.addTorrent manager
                      meta-info
                      bit-field
                      torrent-path
                      false
                      root-path)
         (if-let [torrent (get-torrent-by-path torrent-path)]
           (.setPersistent torrent persistent?))
         (println "Torrent added to" torrent-path)
         (if func (func)))
       (.getInfoHash meta-info))
     (catch java.io.IOException ioe
       (println "Error adding torrent:" (.getMessage ioe))
       nil))))
