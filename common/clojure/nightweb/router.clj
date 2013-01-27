(ns nightweb.router
  (:use [nightweb.crypto :only [create-keys create-signature]]
        [nightweb.io :only [base32-encode
                            base32-decode
                            write-link-file]]
        [nightweb.constants :only [slash get-meta-dir]]
        [nightweb.torrent :only [start-torrent-manager create-torrent]]))

(def base-dir nil)
(def user-hash nil)

(defn get-user-hash
  [is-binary?]
  (if is-binary? @user-hash (base32-encode @user-hash)))

(defn create-user-torrent
  []
  (let [pub-key-path (create-keys base-dir)]
    (create-torrent pub-key-path false)))

(defn create-meta-torrent
  []
  (let [path (str base-dir (get-meta-dir (get-user-hash false)))
        info-hash (create-torrent path)]
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
  (start-torrent-manager)
  (java.lang.Thread/sleep 2000)
  (def user-hash (future (create-user-torrent))))

(defn stop-router
  []
  (if-let [contexts (net.i2p.router.RouterContext/listContexts)]
    (if-not (.isEmpty contexts)
      (if-let [context (.get contexts 0)]
        (.shutdown (.router context) net.i2p.router.Router/EXIT_HARD)))))
