(ns nightweb-desktop.core
  (:gen-class)
  (:require [nightweb.router :as router]
            [nightweb-desktop.server :as server]
            [nightweb-desktop.window :as window]))

(defn -main
  []
  (router/start-router "nightweb")
  (server/start-server)
  (window/start-window))
