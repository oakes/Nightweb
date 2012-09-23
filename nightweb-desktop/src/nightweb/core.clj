(ns nightweb.core
  (:require [nightweb.console :as console]
            [nightweb.router :as router])
  (:import )
  (:gen-class))

(defn -main
  "Launch everything."
  [& args]
  (router/start-http-proxy)
  (console/start-console)
  (comment (router/start-router)))