(ns nightweb.router
  (:use [nightweb.crypto :only [create-user-keys create-signature]]
        [nightweb.io :only [file-exists?
                            make-dir
                            iterate-dir
                            base32-encode
                            base32-decode
                            write-priv-node-key-file
                            write-pub-node-key-file
                            write-link-file]]
        [nightweb.constants :only [slash
                                   torrent-ext
                                   pub-key
                                   users-dir
                                   get-meta-dir
                                   get-user-dir]]
        [nightweb.torrent :only [start-torrent-manager
                                 floodfill-meta-links
                                 get-public-node
                                 get-private-node
                                 add-node
                                 add-hash
                                 add-torrent]]))

(def base-dir nil)
(def user-hash-bytes nil)
(def user-hash-str nil)

(defn get-user-hash
  [is-binary?]
  (if is-binary? user-hash-bytes user-hash-str))

(defn add-user-hash
  [their-hash-bytes]
  (let [their-hash-str (base32-encode their-hash-bytes)
        path (str base-dir (get-user-dir their-hash-str))]
    (when-not (file-exists? path)
      (make-dir path)
      (add-hash path their-hash-bytes true))))

(defn add-user-torrents
  []
  (iterate-dir (str base-dir users-dir)
               (fn [dir-name]
                 (let [user-dir (str base-dir (get-user-dir dir-name))
                       pub-path (str user-dir slash pub-key)
                       pub-torrent-path (str pub-path torrent-ext)
                       meta-path (str base-dir (get-meta-dir dir-name))
                       meta-torrent-path (str meta-path torrent-ext)]
                   (if (not= dir-name user-hash-str)
                     (if (file-exists? pub-torrent-path)
                       (add-torrent pub-path true)
                       (add-hash user-dir (base32-decode dir-name) true)))
                   (if (file-exists? meta-torrent-path)
                     (add-torrent meta-path false))))))

(defn dht-init-callback
  []
  (write-priv-node-key-file base-dir (get-private-node))
  (write-pub-node-key-file base-dir (get-public-node))
  (add-node "9eVgs6E2RYAIml6BnHjYVvPJuTg=:9eVgs-3gaPVhgNMqlMR3HqeR3MXjcs3jWU~UZw0~kGw=:KLYFvjezm9bG0fI73KE2vqq3ixGYHylcyw0HallFAii7UrSv~boLMHXJAAEx4nkzj5wW-IldGzitWDoQOBDPCHwtg7z5Cw0VYcuyJh7f4o-WLlv~wOAOyERRdwpaH5OT0BPZdLJQJ3u~z4qZ2wgww2kB25GDHMA5sbNiaR4wuYHzIwAAPuyYisCmfDOQ7U1UyGf6PhTGdkASFZgZKuCqLkuc8D6eh4FNXnP3LFtmDBQ7zuYyZf0SGWlKhKoWT0Kspxr1Z~8ksBlk7RAF3s9353FM-vK8G2o5Q4RIxcRkroGi4rBYxXl39-6LCo4xHi~p-tTRxGS8HGqJjjozbZ~7PRS9zDkxJLnt7d8oQBmIECUlDw-E66gKlvUwYMSk6gSJbjG7uQTsMWqXd9SWqyL1AXCL2ynEytmgCwX0L77In8lyOI5SkE2SzNqwZMSr3FNaUkfdm0Vq9S8RH5ru~BTglJOp2ptGZUaBb-7Hmz3-Fs2DwsCxLIl-9rG19GXplwoJAAAA:19670"))

(defn create-user-torrent
  []
  (let [pub-key-path (create-user-keys base-dir)]
    (add-torrent pub-key-path true dht-init-callback)))

(defn create-meta-torrent
  []
  (let [path (str base-dir (get-meta-dir (get-user-hash false)))
        info-hash (add-torrent path false)]
    (write-link-file path info-hash (fn [data] (create-signature data)))))

(defn parse-url
  [url]
  (let [url-str (subs url (+ 1 (.indexOf url "#")))
        url-vec (clojure.string/split url-str #"[&=]")
        url-map (if (even? (count url-vec))
                  (apply hash-map url-vec)
                  {})
        {type-val "type" hash-val "hash"} url-map]
    {:type (if type-val (keyword type-val) nil)
     :hash (if hash-val (base32-decode hash-val) nil)}))

(defn start-router
  [dir]
  (def base-dir dir)
  (java.lang.System/setProperty "i2p.dir.base" dir)
  (java.lang.System/setProperty "i2p.dir.config" dir)
  (java.lang.System/setProperty "wrapper.logfile" (str dir slash "wrapper.log"))
  (net.i2p.router.RouterLaunch/main nil)
  (start-torrent-manager dir)
  (java.lang.Thread/sleep 3000)
  (def user-hash-bytes (create-user-torrent))
  (def user-hash-str (base32-encode user-hash-bytes))
  (add-user-torrents)
  (future
    (while true
      (java.lang.Thread/sleep 10000)
      (floodfill-meta-links dir user-hash-str))))

(defn stop-router
  []
  (if-let [contexts (net.i2p.router.RouterContext/listContexts)]
    (if-not (.isEmpty contexts)
      (if-let [context (.get contexts 0)]
        (.shutdown (.router context) net.i2p.router.Router/EXIT_HARD)))))
