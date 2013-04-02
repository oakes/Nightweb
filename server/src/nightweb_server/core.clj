(ns nightweb-server.core
  (:gen-class)
  (:use [nightweb.router :only [start-router]]))

(defn -main
  []
  (start-router "nightweb" false))
