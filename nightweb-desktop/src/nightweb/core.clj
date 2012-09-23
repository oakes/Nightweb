(ns nightweb.core
  (:require [nightweb.console :as console]
            [nightweb.router :as router])
  (:gen-class))

(defn -main
  "Launch everything."
  [& args]
  (router/start-http-proxy)
  (console/start-console)
  (comment (router/start-router)))