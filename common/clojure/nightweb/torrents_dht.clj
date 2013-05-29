(ns nightweb.torrents-dht
  (:use [clojure.java.io :only [file]]
        [nightweb.io :only [file-exists?
                            write-file
                            make-dir
                            iterate-dir
                            delete-file-recursively
                            read-priv-node-key-file
                            read-pub-node-key-file
                            write-priv-node-key-file
                            write-pub-node-key-file
                            read-key-file
                            read-link-file
                            read-meta-file
                            delete-orphaned-files]]
        [nightweb.db :only [insert-meta-data
                            get-single-fav-data
                            get-fav-data
                            delete-user]]
        [nightweb.formats :only [base32-encode
                                 base32-decode
                                 b-encode
                                 b-decode
                                 b-decode-map
                                 b-decode-bytes
                                 b-decode-long]]
        [nightweb.crypto :only [verify-signature]]
        [nightweb.constants :only [is-me?
                                   my-hash-str
                                   torrent-ext
                                   get-user-dir
                                   get-user-pub-file
                                   get-meta-dir
                                   get-meta-link-file]]
        [nightweb.torrents :only [manager
                                  iterate-torrents
                                  iterate-peers
                                  get-torrent-by-path
                                  add-hash
                                  remove-torrent]]))

; dht nodes

(defn get-node-info-for-peer
  [peer]
  (let [dht (.getDHT (.util @manager))
        destination (.getAddress (.getPeerID peer))]
    (.getNodeInfo dht destination)))

(defn get-public-node
  []
  (if-let [dht (.getDHT (.util @manager))]
    (.toPersistentString (.getNodeInfo dht nil))
    (println "Failed to get public node")))

(defn get-private-node
  []
  (if-let [socket-manager (.getSocketManager (.util @manager))]
    (.getSession socket-manager)
    (println "Failed to get private node")))

(defn add-node
  [node-info-str]
  (if-let [dht (.getDHT (.util @manager))]
    (let [ninfo (.heardAbout dht (org.klomp.snark.dht.NodeInfo. node-info-str))]
      (.setPermanent ninfo true))
    (println "Failed to add bootstrap node")))

(defn is-connecting?
  []
  (if-let [util (.util @manager)]
    (.isConnecting util)
    true))

; sending meta links

(defn send-custom-query
  "Sends a query over KRPC."
  [node-info method args]
  (let [query (doto (java.util.HashMap.)
                (.put "q" method)
                (.put "a" args))]
    (.sendQuery (.getDHT (.util @manager)) node-info query true)))

(defn send-meta-link
  "Sends the relevant meta link to all peers in a given user torrent."
  ([]
   (when-let [torrent (-> (get-user-pub-file @my-hash-str)
                          (str torrent-ext)
                          (get-torrent-by-path))]
     (send-meta-link torrent)))
  ([torrent]
   (let [info-hash-str (base32-encode (.getInfoHash torrent))
         args (read-link-file info-hash-str)]
     (iterate-peers torrent
                    (fn [peer]
                      (let [node-info (get-node-info-for-peer peer)]
                        (send-custom-query node-info "announce_meta" args)))))))

(defn send-meta-link-periodically
  "Sends the relevant meta link to all peers in each user torrent."
  [seconds]
  (future
    (while true
      (java.lang.Thread/sleep (* seconds 1000))
      (iterate-torrents (fn [torrent]
                          (when (.getPersistent torrent)
                            (send-meta-link torrent)))))))

; ingest meta torrents

(defn add-user-hash
  "Begins following the supplied user hash if we aren't already."
  [their-hash-bytes]
  (when their-hash-bytes
    (let [their-hash-str (base32-encode their-hash-bytes)
          path (get-user-dir their-hash-str)]
      (when-not (file-exists? path)
        (make-dir path)
        (add-hash path their-hash-str true send-meta-link)))))

(defn remove-user-hash
  "Removes a user completely if nobody we care about is following them."
  [their-hash-bytes]
  (when (and their-hash-bytes
             (not (is-me? their-hash-bytes))
             (-> {:ptrhash their-hash-bytes}
                 (get-fav-data)
                 (count)
                 (= 0)))
    (let [their-hash-str (base32-encode their-hash-bytes)
          user-dir (get-user-dir their-hash-str)]
      (println "Deleting user" their-hash-str)
      (iterate-torrents
        (fn [torrent]
          (when (>= (.indexOf (.getDataDir torrent) their-hash-str) 0)
            (remove-torrent (.getName torrent)))))
      (delete-file-recursively user-dir)
      (delete-user their-hash-bytes)
      (iterate-dir (get-user-dir)
                   (fn [user-hash-str]
                     (remove-user-hash (base32-decode user-hash-str)))))))

(defn on-recv-fav
  "Add or remove user if necessary based on a fav we received."
  [user-hash ptr-hash status]
  ; if this is from a user we care about
  (when (or (is-me? user-hash)
            (-> {:userhash user-hash}
                (get-single-fav-data)
                (get :status)
                (= 1)))
    (case status
      ; if the fav has a status of 0, unfollow them if necessary
      0 (remove-user-hash ptr-hash)
      ; if the fav has a status of 1, make sure we are following them
      1 (add-user-hash ptr-hash)
      nil)))

(defn on-recv-meta-file
  "Ingests a given file from a meta torrent"
  [user-hash-bytes meta-file]
  ; insert it into the db
  (insert-meta-data user-hash-bytes meta-file)
  ; if this is a fav of a user, act on it if necessary
  (let [meta-contents (:contents meta-file)]
    (when (and (= "fav" (:dir-name meta-file))
               (nil? (get meta-contents "ptrtime")))
      (on-recv-fav user-hash-bytes
                   (b-decode-bytes (get meta-contents "ptrhash"))
                   (b-decode-long (get meta-contents "status"))))))

(defn on-recv-meta
  "Ingests all files in a meta torrent."
  [torrent]
  (let [parent-dir (.getParentFile (file (.getName torrent)))
        user-hash-bytes (base32-decode (.getName parent-dir))
        paths (.getFiles (.getMetaInfo torrent))]
    ; iterate over the files in this torrent
    (doseq [path-leaves paths]
      (on-recv-meta-file user-hash-bytes
                         (read-meta-file parent-dir path-leaves)))
    ; remove any files that the torrent no longer contains
    (when-not (is-me? user-hash-bytes)
      (delete-orphaned-files user-hash-bytes paths))))

; receiving meta links

(defn parse-meta-link
  "Creates a map of parsed values from a given meta link."
  [link]
  (let [{data-val "data" sig-val "sig"} link
        data-map (b-decode-map (b-decode (b-decode-bytes data-val)))
        {user-hash-val "user_hash"
         link-hash-val "link_hash"
         time-val "mtime"} data-map
        user-hash-bytes (b-decode-bytes user-hash-val)
        link-hash-bytes (b-decode-bytes link-hash-val)
        time-num (b-decode-long time-val)]
    (when (and link data-val user-hash-bytes)
      {:link (b-encode link)
       :data (b-decode-bytes data-val)
       :sig (b-decode-bytes sig-val)
       :user-hash-str (base32-encode user-hash-bytes)
       :link-hash-str (base32-encode link-hash-bytes)
       :time time-num})))

(defn validate-meta-link
  "Makes sure a meta link has the required values and signature."
  [link-map]
  (and link-map
       (:time link-map)
       (<= (:time link-map) (.getTime (java.util.Date.)))
       (let [user-hash-str (:user-hash-str link-map)
             pub-key-path (get-user-pub-file user-hash-str)]
         (verify-signature (read-key-file pub-key-path)
                           (:sig link-map)
                           (:data link-map)))))

(defn save-meta-link
  "Saves a meta link to the disk."
  [link-map]
  (let [user-hash-str (:user-hash-str link-map)
        link-path (get-meta-link-file user-hash-str)]
    (write-file link-path (:link link-map))))

(defn replace-meta-link
  "Stops sharing a given meta torrent and begins downloading an updated one."
  [user-hash-str old-link-map new-link-map]
  (let [user-dir (get-user-dir user-hash-str)
        meta-torrent-path (str (get-meta-dir user-hash-str) torrent-ext)]
    (remove-torrent meta-torrent-path)
    (when-let [old-hash-str (:link-hash-str old-link-map)]
      (remove-torrent old-hash-str))
    (save-meta-link new-link-map)
    (add-hash user-dir (:link-hash-str new-link-map) false on-recv-meta)
    (println "Saved meta link")))

(defn compare-meta-link
  "Checks if a given meta link is newer than the one we already have."
  [link-map]
  (let [user-hash-str (:user-hash-str link-map)
        my-link (read-link-file user-hash-str)
        my-link-map (parse-meta-link my-link)
        my-time (:time my-link-map)
        their-time (:time link-map)]
    (if (not= my-time their-time)
      (if (validate-meta-link link-map)
        (if (or (nil? my-time) (> their-time my-time))
          (replace-meta-link user-hash-str my-link-map link-map)
          my-link)
        my-link)
      (comment "Received identical link"))))

(defn receive-meta-link
  "Parses and, if necessary, saves a given meta link."
  [args]
  (if-let [link (parse-meta-link args)]
    (compare-meta-link link)
    (println "Meta link can't be parsed")))

; initialization

(defn init-dht
  "Sets the node keys, query handler, and bootstrap node for DHT."
  []
  ; set the node keys from the disk
  (let [priv-node (read-priv-node-key-file)
        pub-node (read-pub-node-key-file)]
    (when (and priv-node pub-node)
      (.setDHTNode (.util @manager) priv-node pub-node)))
  ; set the custom query handler
  (.setDHTCustomQueryHandler
    (.util @manager)
    (reify org.klomp.snark.dht.CustomQueryHandler
      (receiveQuery [this method args]
        (case method
          "announce_meta" (receive-meta-link args)
          nil))
      (receiveResponse [this args]
        (receive-meta-link args))))
  ; set the init callback
  (.setDHTInitCallback
    (.util @manager)
    (fn []
      (let [priv-node (get-private-node)
            pub-node (get-public-node)]
        (when (and priv-node pub-node)
          (write-priv-node-key-file priv-node)
          (write-pub-node-key-file pub-node)))
      ; add dht bootstrap node
      (add-node "rkJ0ws6PQz8FU7VvTW~Lelhb6DM=:rkJ0wkX6jrW3HJBNdhuLlWCUPKDAlX8T23lrTOeMGK8=:B5QFqHHlCT5fOA2QWLAlAKba1hIjW-KBt2HCqwtJg8JFa2KnjAzcexyveYT8HOcMB~W6nhwhzQ7~sywFkvcvRkKHbf6LqP0X43q9y2ADFk2t9LpUle-L-x34ZodEEDxQbwWo74f-rX5IemW2-Du-8NH-o124OGvq5N4uT4PjtxmgSVrBYVLjZRYFUWgdmgR1lVOncfMDbXzXGf~HdY97s9ZFHYyi7ymwzlk4bBN9-Pd4I1tJB2sYBzk62s3gzY1TlDKOdy7qy1Eyr4SEISAopJrvAnSkS1eIFyCoqfzzrBWM11uWppWetf3AkHxGitJIQe73wmZrrO36jHNewIct54v2iF~~3cqBVlT4ptX1Dc-thjrxXoV73A0HUASldCeFZSVJFMQgOQK9U85NQscAokftpyp4Ai89YWaUvSDcZPd-mQuA275zifPwp8s8UfYV5EBqvdHnfeJjxmyTcKR3g5Ft8ABai9yywxoA7yoABD4EGzsFtAh0nOLcmbM944zdAAAA:35701")
      (println "DHT initialized")))
  ; make sure we always have the newest meta link from our user torrents
  (send-meta-link-periodically 60))
