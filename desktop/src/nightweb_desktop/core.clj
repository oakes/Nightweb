(ns nightweb-desktop.core
  (:gen-class)
  (:require [clojure.java.io :as java.io]
            [nightweb.router :as router]
            [nightweb-desktop.server :as server]
            [nightweb-desktop.utils :as utils]
            [nightweb-desktop.window :as window]))

(defn get-data-dir
  []
  (let [home-dir (System/getProperty "user.home")
        app-name "Nightweb"
        app-name-lower (clojure.string/lower-case app-name)
        osx-dir (java.io/file home-dir "Library" "Application Support" app-name)
        win-dir (java.io/file home-dir "AppData" "Roaming" app-name)
        lin-dir (java.io/file home-dir (str "." app-name-lower))]
    (.getCanonicalPath
      (cond
        (.exists (.getParentFile osx-dir)) osx-dir
        (.exists (.getParentFile win-dir)) win-dir
        (.exists lin-dir) lin-dir
        :else (if-let [config-dir (System/getenv "XDG_CONFIG_HOME")]
                (java.io/file config-dir app-name-lower)
                (java.io/file home-dir ".config" app-name-lower))))))

(defn -main
  [& args]
  (router/start-router! (get-data-dir))
  (server/start-server!)
  (when-not (contains? (set args) "-nw")
    (window/start-window!)))
