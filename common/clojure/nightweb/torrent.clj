(ns nightweb.torrent
  (:use [clojure.java.io :only [file input-stream]]
        [nightweb.io :only [base32-encode]]))

(def manager nil)

(defn start-torrent-manager
  []
  (let [context (net.i2p.I2PAppContext/getGlobalContext)]
    (def manager (org.klomp.snark.SnarkManager. context))
    (.loadConfig manager "i2psnark.config")
    (.start manager)))

(defn add-torrent
  [info-hash]
  (try
    (.addMagnet manager (base32-encode info-hash) info-hash nil false)
    (catch IllegalArgumentException iae
      (println "Invalid info hash"))))

(defn create-torrent
  ([path] (create-torrent path true))
  ([path overwrite?]
   (try
     (let [base-file (file path)
           torrent-file (file
                          (.getParent base-file)
                          (str (.getName base-file) ".torrent"))
           torrent-path (.getAbsolutePath torrent-file)
           listener (reify org.klomp.snark.StorageListener
                      (storageCreateFile [this storage file-name length])
                      (storageAllocated [this storage length])
                      (storageChecked [this storage piece-num checked])
                      (storageAllChecked [this storage])
                      (storageCompleted [this storage])
                      (setWantedPieces [this storage])
                      (addMessage [this message]))]
       (if (and overwrite? (.exists torrent-file))
         (do
           (.stopTorrent manager torrent-path true)
           (.delete torrent-file)))
       (let [storage (if (.exists torrent-file)
                       (org.klomp.snark.Storage.
                         (.util manager)
                         (org.klomp.snark.MetaInfo. (input-stream torrent-path))
                         listener)
                       (org.klomp.snark.Storage.
                         (.util manager) base-file nil false listener))
             _ (.close storage)
             info (.getMetaInfo storage)]
         (future
           (.addTorrent manager info (.getBitField storage) torrent-path false)
           (println "Torrent created for" path "at" torrent-path))
         (.getInfoHash info)))
     (catch java.io.IOException ioe
       (println "Error creating torrent for" path)
       nil))))
