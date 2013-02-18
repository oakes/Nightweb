(ns nightweb.torrent
  (:use [clojure.java.io :only [file input-stream]]
        [nightweb.io :only [file-exists?
                            base32-decode
                            b-decode
                            b-decode-bytes
                            b-decode-number
                            read-priv-node-key-file
                            read-pub-node-key-file
                            read-link-file]]
        [nightweb.constants :only [users-dir pub-key torrent-ext get-meta-dir]]))

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
    (.setDHTCustomQueryHandler
      (.util manager)
      (reify org.klomp.snark.dht.CustomQueryHandler
        (receiveQuery [this method args]
          (case method
            "announce_meta"
            (let [{data-val "data" sig-val "sig"} args
                  data-map (b-decode (b-decode-bytes data-val))
                  {user-hash-val "user_hash"
                   link-hash-val "link_hash"
                   time-val "time"} data-map
                  user-hash-bytes (b-decode-bytes user-hash-val)
                  link-hash-bytes (b-decode-bytes link-hash-val)
                  time-num (b-decode-number time-val)]
              (println data-val sig-val user-hash-bytes link-hash-bytes time-num))))))
    (.start manager false)))

(defn send-custom-query
  [node-info args]
  (.sendQuery (.getDHT (.util manager)) node-info args true))

(defn get-torrent-paths
  []
  (.listTorrentFiles manager))

(defn get-torrent-by-path
  [path]
  (.getTorrent manager path))

(defn floodfill-meta-links
  [base-dir my-user-hash-str]
  (doseq [path (get-torrent-paths)]
    (if (.contains path pub-key)
      (let [torrent (get-torrent-by-path path)
            user-hash-str (if (.contains path users-dir)
                            (.getName (.getParentFile (file path)))
                            my-user-hash-str)
            meta-dir-path (str base-dir (get-meta-dir user-hash-str))
            meta-link (read-link-file meta-dir-path)]
        (if (and torrent meta-link)
          (doseq [peer (.getPeerList torrent)]
            (let [dht (.getDHT (.util manager))
                  destination (.getAddress (.getPeerID peer))
                  node-info (.getNodeInfo dht destination)]
              (send-custom-query node-info
                                 (doto (java.util.HashMap.)
                                   (.put "q" "announce_meta")
                                   (.put "a" meta-link))))))))))

(defn get-public-node
  []
  (if-let [dht (.getDHT (.util manager))]
    (.toPersistentString (.getNodeInfo dht nil))
    nil))

(defn get-private-node
  []
  (if-let [socket-manager (.getSocketManager (.util manager))]
    (.getSession socket-manager)
    nil))

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
      ;(.getSavedTorrentTime manager snark)
      0)
    (getSavedTorrentBitField [this snark]
      (println "getSavedTorrentBitField")
      ;(.getSavedTorrentBitField manager snark)
      nil)))

(defn add-node
  [node-info-str]
  (if-let [dht (.getDHT (.util manager))]
    (.heardAbout dht (org.klomp.snark.dht.NodeInfo. node-info-str))
    (println "Failed to add bootstrap node")))

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
  ([path persistent?] (add-torrent path persistent? nil))
  ([path persistent? func]
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
         (println "Torrent added to" torrent-path)
         (if func (func)))
       (.getInfoHash meta-info))
     (catch java.io.IOException ioe
       (println "Error adding torrent:" (.getMessage ioe))
       nil))))

(defn remove-torrent
  [path]
  (.stopTorrent manager path true)
  (.delete (file path)))
