(ns nightweb-desktop.server
  (:use [ring.adapter.jetty :only [run-jetty]]))

(defn handler [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "Hello World!"})

(defn start-server
  []
  (future (run-jetty handler {:port 3000})))
