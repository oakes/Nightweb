(ns nightweb.torrents
  (:require [clojure.java.io :as java.io]
            [nightweb.constants :as c]
            [nightweb.formats :as f]
            [nightweb.io :as io])
  (:import [net.i2p I2PAppContext]
           [org.klomp.snark CompleteListener MetaInfo Snark SnarkManager
                            Storage StorageListener]))

(def manager (atom nil))

; active torrents

(defn get-torrent-paths
  []
  (.listTorrentFiles ^SnarkManager @manager))

(defn get-torrent-by-path
  [path]
  (.getTorrent ^SnarkManager @manager path))

(defn iterate-torrents
  [func]
  (doseq [path (get-torrent-paths)]
    (when-let [torrent (get-torrent-by-path path)]
      (func torrent))))

(defn iterate-peers
  [^Snark torrent func]
  (doseq [peer (.getPeerList torrent)]
    (func peer)))

; starting and stopping torrents

(defn get-storage
  "Creates a Storage object with a listener for each storage-related event."
  [path]
  (let [listener (reify StorageListener
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
        torrent-path (str path c/torrent-ext)
        storage (if (io/file-exists? torrent-path)
                  (Storage.
                    (.util ^SnarkManager @manager)
                    (MetaInfo. (java.io/input-stream torrent-path))
                    listener)
                  (Storage. (.util ^SnarkManager @manager)
                            (java.io/file path)
                            nil
                            nil
                            false
                            listener))]
    (.close storage)
    storage))

(defn get-complete-listener
  "Creates a listener for each event in a given torrent download."
  [path complete-callback]
  (reify CompleteListener
    (torrentComplete [this snark]
      (println "torrentComplete")
      (.torrentComplete ^SnarkManager @manager snark)
      (complete-callback snark))
    (updateStatus [this snark]
      (println "updateStatus")
      (.updateStatus ^SnarkManager @manager snark))
    (gotMetaInfo [this snark]
      (println "gotMetaInfo")
      (.gotMetaInfo ^SnarkManager @manager snark path))
    (fatal [this snark error]
      (println "fatal" error)
      (.fatal ^SnarkManager @manager snark error))
    (addMessage [this snark message]
      (println "addMessage" message)
      (.addMessage ^SnarkManager @manager snark message))
    (gotPiece [this snark]
      (println "gotPiece")
      (.gotPiece ^SnarkManager @manager snark))
    (getSavedTorrentTime [this snark]
      (println "getSavedTorrentTime")
      ;(.getSavedTorrentTime ^SnarkManager @manager snark)
      0)
    (getSavedTorrentBitField [this snark]
      (println "getSavedTorrentBitField")
      ;(.getSavedTorrentBitField ^SnarkManager @manager snark)
      nil)))

(defn add-hash
  "Adds an info hash to download."
  [path info-hash-str is-persistent? complete-callback]
  (future
    (try
      (.addMagnet ^SnarkManager @manager
                  info-hash-str
                  (f/base32-decode info-hash-str)
                  nil
                  false
                  true
                  (get-complete-listener path complete-callback)
                  path)
      (when-let [^Snark torrent (get-torrent-by-path info-hash-str)]
        (.setPersistent torrent is-persistent?))
      (println "Hash added to" path)
      (catch IllegalArgumentException iae
        (println "Error adding hash:" (.getMessage iae))))))

(defn add-torrent
  "Adds a torrent to download or seed."
  [path is-persistent? complete-callback]
  (try
    (let [base-file (java.io/file path)
          root-path (.getParent base-file)
          torrent-file (java.io/file (str path c/torrent-ext))
          torrent-path (.getCanonicalPath torrent-file)
          ^Storage storage (get-storage path)
          meta-info (.getMetaInfo storage)
          bit-field (.getBitField storage)
          listener (get-complete-listener root-path complete-callback)]
      (future
        (.addTorrent ^SnarkManager @manager
                     meta-info
                     bit-field
                     torrent-path
                     false
                     listener
                     root-path)
        (when-let [^Snark torrent (get-torrent-by-path torrent-path)]
          (.setPersistent torrent is-persistent?))
        (println "Torrent added to" torrent-path))
      (.getInfoHash meta-info))
    (catch java.io.IOException ioe
      (println "Error adding torrent:" (.getMessage ioe))
      nil)))

(defn remove-torrent
  "Stops and deletes a torrent."
  [path]
  (.removeTorrent ^SnarkManager @manager path))

(defn get-info-hash
  "Gets the info hash for a given path."
  [path]
  (let [^Storage storage (get-storage path)
        meta-info (.getMetaInfo storage)]
    (.getInfoHash meta-info)))

; initialization

(defn start-torrent-manager
  "Starts the I2PSnark manager."
  [dir]
  (let [context (I2PAppContext/getGlobalContext)
        snark-dir (.getCanonicalPath (java.io/file dir "i2psnark"))]
    (reset! manager (SnarkManager. context snark-dir snark-dir))
    (.updateConfig ^SnarkManager @manager
                   nil ;dataDir
                   true ;filesPublic
                   true ;autoStart
                   nil ;refreshDelay
                   nil ;startDelay
                   nil ;pageSize
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
    (.start ^SnarkManager @manager false)))
