(ns nightweb-server.core
  (:gen-class)
  (:require [nightweb.router :as router]))

(defn -main
  []
  (router/start-router "nightweb" false))
