(ns nightweb.torrents
  (:use [clojure.java.io :only [file input-stream]]
        [nightweb.io :only [file-exists?
                            write-file
                            read-priv-node-key-file
                            read-pub-node-key-file
                            write-priv-node-key-file
                            write-pub-node-key-file
                            read-key-file
                            read-link-file]]
        [nightweb.formats :only [base32-encode
                                 base32-decode
                                 b-encode
                                 b-decode
                                 b-decode-map
                                 b-decode-bytes
                                 b-decode-long]]
        [nightweb.crypto :only [verify-signature]]
        [nightweb.constants :only [torrent-ext
                                   get-user-dir
                                   get-user-pub-file
                                   get-meta-dir
                                   get-meta-link-file]]))

(def manager nil)

; DHT nodes

(defn get-node-info-for-peer
  [peer]
  (let [dht (.getDHT (.util manager))
        destination (.getAddress (.getPeerID peer))]
    (.getNodeInfo dht destination)))

(defn get-public-node
  []
  (if-let [dht (.getDHT (.util manager))]
    (.toPersistentString (.getNodeInfo dht nil))
    (println "Failed to get public node")))

(defn get-private-node
  []
  (if-let [socket-manager (.getSocketManager (.util manager))]
    (.getSession socket-manager)
    (println "Failed to get private node")))

(defn add-node
  [node-info-str]
  (if-let [dht (.getDHT (.util manager))]
    (let [ninfo (.heardAbout dht (org.klomp.snark.dht.NodeInfo. node-info-str))]
      (.setPermanent ninfo true))
    (println "Failed to add bootstrap node")))

(defn is-connecting?
  []
  (if-let [util (.util manager)]
    (.isConnecting util)
    true))

; active torrents

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

; sending meta links

(defn send-custom-query
  [node-info method args]
  (let [query (doto (java.util.HashMap.)
                (.put "q" method)
                (.put "a" args))]
    (.sendQuery (.getDHT (.util manager)) node-info query true)))

(defn send-meta-link
  ([]
   (if-let [torrent (str (get-torrent-by-path (get-user-pub-file)) torrent-ext)]
     (send-meta-link torrent)))
  ([torrent]
   (let [info-hash-str (base32-encode (.getInfoHash torrent))
         args (read-link-file info-hash-str)]
     (iterate-peers torrent
                    (fn [peer]
                      (let [node-info (get-node-info-for-peer peer)]
                        (send-custom-query node-info "announce_meta" args)))))))

(defn send-meta-link-periodically
  [seconds]
  (future
    (while true
      (java.lang.Thread/sleep (* seconds 1000))
      (iterate-torrents (fn [torrent]
                          (if (.getPersistent torrent)
                            (send-meta-link torrent)))))))

; starting and stopping torrents

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
                    (.util manager) (file path) nil nil false listener))]
    (.close storage)
    storage))

(defn get-info-hash
  [path]
  (let [storage (get-storage path)
        meta-info (.getMetaInfo storage)]
    (.getInfoHash meta-info)))

(defn get-complete-listener
  [path complete-callback]
  (reify org.klomp.snark.CompleteListener
    (torrentComplete [this snark]
      (println "torrentComplete")
      (.torrentComplete manager snark)
      (complete-callback snark))
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
  [path info-hash-str is-persistent? complete-callback]
  (future
    (try
      (.addMagnet manager
                  info-hash-str
                  (base32-decode info-hash-str)
                  nil
                  false
                  true
                  (get-complete-listener path complete-callback)
                  path)
      (if-let [torrent (get-torrent-by-path info-hash-str)]
        (.setPersistent torrent is-persistent?))
      (println "Hash added to" path)
      (catch IllegalArgumentException iae
        (println "Error adding hash:" (.getMessage iae))))))

(defn add-torrent
  ([path is-persistent? complete-callback]
   (add-torrent path is-persistent? complete-callback false))
  ([path is-persistent? complete-callback should-block?]
   (try
     (let [base-file (file path)
           root-path (.getParent base-file)
           torrent-file (file (str path torrent-ext))
           torrent-path (.getCanonicalPath torrent-file)
           storage (get-storage path)
           meta-info (.getMetaInfo storage)
           bit-field (.getBitField storage)
           listener (get-complete-listener root-path complete-callback)
           thread (future
                    (.addTorrent manager
                                 meta-info
                                 bit-field
                                 torrent-path
                                 false
                                 listener
                                 root-path)
                    (if-let [torrent (get-torrent-by-path torrent-path)]
                      (.setPersistent torrent is-persistent?))
                    (println "Torrent added to" torrent-path))]
       (if should-block? (deref thread))
       (.getInfoHash meta-info))
     (catch java.io.IOException ioe
       (println "Error adding torrent:" (.getMessage ioe))
       nil))))

(defn remove-torrent
  [path]
  (.removeTorrent manager path))

; receiving meta links

(defn parse-meta-link
  [link]
  (let [{data-val "data" sig-val "sig"} link
        data-map (b-decode-map (b-decode (b-decode-bytes data-val)))
        {user-hash-val "user_hash"
         link-hash-val "link_hash"
         time-val "mtime"} data-map
        user-hash-bytes (b-decode-bytes user-hash-val)
        link-hash-bytes (b-decode-bytes link-hash-val)
        time-num (b-decode-long time-val)]
    (if (and link data-val user-hash-bytes)
      {:link (b-encode link)
       :data (b-decode-bytes data-val)
       :sig (b-decode-bytes sig-val)
       :user-hash-str (base32-encode user-hash-bytes)
       :link-hash-str (base32-encode link-hash-bytes)
       :time time-num})))

(defn validate-meta-link
  [link-map]
  (and link-map
       (get link-map :time)
       (<= (get link-map :time) (.getTime (java.util.Date.)))
       (let [user-hash-str (get link-map :user-hash-str)
             pub-key-path (get-user-pub-file user-hash-str)]
         (verify-signature (read-key-file pub-key-path)
                           (get link-map :sig)
                           (get link-map :data)))))

(defn save-meta-link
  [link-map]
  (let [user-hash-str (get link-map :user-hash-str)
        link-path (get-meta-link-file user-hash-str)]
    (write-file link-path (get link-map :link))))

(defn replace-meta-link
  [user-hash-str old-link-map new-link-map]
  (let [user-dir (get-user-dir user-hash-str)
        meta-torrent-path (str (get-meta-dir user-hash-str) torrent-ext)]
    (remove-torrent meta-torrent-path)
    (if-let [old-hash-str (get old-link-map :link-hash-str)]
      (remove-torrent old-hash-str))
    (save-meta-link new-link-map)
    (add-hash user-dir (get new-link-map :link-hash-str) false)
    (println "Saved meta link")))

(defn compare-meta-link
  [link-map]
  (let [user-hash-str (get link-map :user-hash-str)
        my-link (read-link-file user-hash-str)
        my-link-map (parse-meta-link my-link)
        my-time (get my-link-map :time)
        their-time (get link-map :time)]
    (if (not= my-time their-time)
      (if (validate-meta-link link-map)
        (if (or (nil? my-time) (> their-time my-time))
          (replace-meta-link user-hash-str my-link-map link-map)
          my-link)
        my-link)
      (comment "Received identical link"))))

(defn receive-meta-link
  [args]
  (if-let [link (parse-meta-link args)]
    (compare-meta-link link)
    (println "Meta link can't be parsed")))

; initialization

(defn init-dht
  []
  ; set the node keys from the disk
  (let [priv-node (read-priv-node-key-file)
        pub-node (read-pub-node-key-file)]
    (if (and priv-node pub-node)
      (.setDHTNode (.util manager) priv-node pub-node)))
  ; set the custom query handler
  (.setDHTCustomQueryHandler
    (.util manager)
    (reify org.klomp.snark.dht.CustomQueryHandler
      (receiveQuery [this method args]
        (case method
          "announce_meta" (receive-meta-link args)))
      (receiveResponse [this args]
        (receive-meta-link args))))
  ; set the init callback
  (.setDHTInitCallback
    (.util manager)
    (fn []
      (let [priv-node (get-private-node)
            pub-node (get-public-node)]
        (when (and priv-node pub-node)
          (write-priv-node-key-file priv-node)
          (write-pub-node-key-file pub-node)))
      (add-node "rkJ0ws6PQz8FU7VvTW~Lelhb6DM=:rkJ0wkX6jrW3HJBNdhuLlWCUPKDAlX8T23lrTOeMGK8=:B5QFqHHlCT5fOA2QWLAlAKba1hIjW-KBt2HCqwtJg8JFa2KnjAzcexyveYT8HOcMB~W6nhwhzQ7~sywFkvcvRkKHbf6LqP0X43q9y2ADFk2t9LpUle-L-x34ZodEEDxQbwWo74f-rX5IemW2-Du-8NH-o124OGvq5N4uT4PjtxmgSVrBYVLjZRYFUWgdmgR1lVOncfMDbXzXGf~HdY97s9ZFHYyi7ymwzlk4bBN9-Pd4I1tJB2sYBzk62s3gzY1TlDKOdy7qy1Eyr4SEISAopJrvAnSkS1eIFyCoqfzzrBWM11uWppWetf3AkHxGitJIQe73wmZrrO36jHNewIct54v2iF~~3cqBVlT4ptX1Dc-thjrxXoV73A0HUASldCeFZSVJFMQgOQK9U85NQscAokftpyp4Ai89YWaUvSDcZPd-mQuA275zifPwp8s8UfYV5EBqvdHnfeJjxmyTcKR3g5Ft8ABai9yywxoA7yoABD4EGzsFtAh0nOLcmbM944zdAAAA:35701")
      (println "DHT initialized")))
  (send-meta-link-periodically 60))

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
    (.start manager false)
    (init-dht)))
