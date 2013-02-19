(ns nightweb.torrent-dht
  (:use [clojure.java.io :only [file]]
        [nightweb.torrent :only [manager
                                 add-hash
                                 remove-torrent]]
        [nightweb.io :only [write-file
                            base32-encode
                            b-encode
                            b-decode
                            b-decode-bytes
                            b-decode-number
                            read-priv-node-key-file
                            read-pub-node-key-file
                            read-key-file
                            read-link-file]]
        [nightweb.constants :only [users-dir
                                   pub-key
                                   torrent-ext
                                   get-user-dir
                                   get-user-pub-file
                                   get-meta-dir
                                   get-meta-link-file]]
        [nightweb.crypto :only [verify-signature]]))

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
    (.heardAbout dht (org.klomp.snark.dht.NodeInfo. node-info-str))
    (println "Failed to add bootstrap node")))

(defn parse-meta-link
  [link]
  (let [{data-val "data" sig-val "sig"} link
        data-map (b-decode (b-decode-bytes data-val))
        {user-hash-val "user_hash"
         link-hash-val "link_hash"
         time-val "time"} data-map
        user-hash-bytes (b-decode-bytes user-hash-val)
        link-hash-bytes (b-decode-bytes link-hash-val)
        time-num (b-decode-number time-val)]
    (if (and user-hash-bytes link-hash-bytes time-num)
      {:link (b-encode link)
       :data (b-decode-bytes data-val)
       :sig (b-decode-bytes sig-val)
       :user-hash user-hash-bytes
       :link-hash link-hash-bytes
       :time time-num})))

(defn validate-meta-link
  [base-dir link-map]
  (and link-map
       (<= (get link-map :time) (.getTime (java.util.Date.)))
       (let [user-hash-str (base32-encode (get link-map :user-hash))
             pub-key-path (str base-dir (get-user-pub-file user-hash-str))]
         (verify-signature (read-key-file pub-key-path)
                           (get link-map :sig)
                           (get link-map :data)))))

(defn get-meta-link
  [base-dir user-hash-str]
  (let [link-path (str base-dir (get-meta-dir user-hash-str))
        link (read-link-file link-path)]
    (doto (java.util.HashMap.)
      (.put "q" "announce_meta")
      (.put "a" link))))

(defn set-meta-link
  [base-dir link-map]
  (let [user-hash-str (base32-encode (get link-map :user-hash))
        link-path (str base-dir (get-meta-link-file user-hash-str))]
    (write-file link-path (get link-map :link))))

(defn compare-meta-link
  [base-dir link-map]
  (let [user-hash-str (base32-encode (get link-map :user-hash))
        my-link (get-meta-link base-dir user-hash-str)
        my-link-map (parse-meta-link (get my-link "a"))
        my-time (get my-link-map :time)
        their-time (get link-map :time)]
    (if (and their-time (not= my-time their-time))
      (if (or (nil? my-time) (> their-time my-time))
        (let [user-dir (str base-dir (get-user-dir user-hash-str))
              meta-file (str base-dir (get-meta-dir user-hash-str) torrent-ext)
              link-hash-old (base32-encode (get my-link-map :link-hash))
              link-hash-new (base32-encode (get link-map :link-hash))]
          (set-meta-link base-dir link-map)
          (remove-torrent meta-file)
          (remove-torrent link-hash-old)
          (add-hash user-dir link-hash-new false))
        my-link))))

(defn send-custom-query
  [node-info args]
  (.sendQuery (.getDHT (.util manager)) node-info args true))

(defn send-meta-link
  [path node-info base-dir my-user-hash-str]
  (if (.contains path pub-key)
    (let [user-hash-str (if (.contains path users-dir)
                              (.getName (.getParentFile (file path)))
                              my-user-hash-str)
          link (get-meta-link base-dir user-hash-str)]
      (send-custom-query node-info link))))

(defn set-dht-node-keys-from-disk
  [base-dir]
  (let [priv-node (read-priv-node-key-file base-dir)
        pub-node (read-pub-node-key-file base-dir)]
    (if (and priv-node pub-node)
      (.setDHTNode (.util manager) priv-node pub-node))))

(defn set-dht-custom-query-handler
  [base-dir]
  (.setDHTCustomQueryHandler
    (.util manager)
    (reify org.klomp.snark.dht.CustomQueryHandler
      (receiveQuery [this method args]
        (case method
          "announce_meta" (let [link (parse-meta-link args)]
                            (if (validate-meta-link base-dir link)
                              (compare-meta-link base-dir link)
                              (println "Invalid meta link:" args))))))))
