(ns nightweb.router
  (:require [clojure.java.io :as java.io]
            [nightweb.actions :as actions]
            [nightweb.constants :as c]
            [nightweb.db :as db]
            [nightweb.io :as io]
            [nightweb.formats :as f]
            [nightweb.torrents :as t]
            [nightweb.torrents-dht :as dht]
            [nightweb.users :as users]))

(def ^:const enable-router? true) ; if false, I2P won't boot
(def is-first-boot? (atom false))

(defn start-router!
  "Starts the I2P router, I2PSnark manager, and the user and meta torrents."
  [dir]
  ; set main dir and initialize the database
  (reset! c/base-dir dir)
  (db/init-db dir)
  ; start i2psnark
  (System/setProperty "i2p.dir.base" dir)
  (System/setProperty "i2p.dir.config" dir)
  (System/setProperty "wrapper.logfile" (-> (java.io/file dir "wrapper.log")
                                            .getCanonicalPath))
  (t/start-torrent-manager! dir)
  (dht/init-dht)
  ; create or load user
  (when (= 0 (count (io/read-user-list-file)))
    (reset! is-first-boot? true)
    (users/create-user))
  (users/load-user nil)
  ; run the rest of the initialization in a separate thread
  (future
    ; start i2p router
    (when enable-router?
      (net.i2p.router.RouterLaunch/main nil)
      (Thread/sleep 10000))
    ; add all user and meta torrents
    (io/iterate-dir (c/get-user-dir) users/add-user-and-meta-torrents)
    ; add default fav user
    (when @is-first-boot? (actions/fav-default-user!))))

(defn get-router
  "Returns the router object if it exists."
  []
  (when-let [contexts (net.i2p.router.RouterContext/listContexts)]
    (when-not (.isEmpty contexts)
      (when-let [context (.get contexts 0)]
        (.router context)))))

(defn stop-router!
  "Shuts down the I2P router."
  []
  (when-let [router (get-router)]
    (.shutdown router net.i2p.router.Router/EXIT_HARD)))
