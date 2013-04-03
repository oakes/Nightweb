(ns nightweb-desktop.core
  (:gen-class)
  (:use [nightweb.router :only [start-router]]
        [nightweb-desktop.server :only [start-server]]
        [nightweb-desktop.window :only [start-window]]))

(defn -main
  []
  ;(start-router "nightweb" false)
  (start-server)
  (start-window))
