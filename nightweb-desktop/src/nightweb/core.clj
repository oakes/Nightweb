(ns nightweb.core
  (:use nightweb.console
        nightweb.browser)
  (:import net.i2p.router.Router
           net.i2p.i2ptunnel.I2PTunnel)
  (:gen-class))

(defn start-http-proxy
  "Launch the http proxy (non-blocking)."
  []
  (future (I2PTunnel/main
    (into-array java.lang.String
                ["-nogui"
                 "-nocli"
                 "-e"
                 "httpclient 4708"]))))

(defn start-router
  "Launch the router (blocking)."
  []
  (Router/main nil))

(defn -main
  "Launch everything."
  [& args]
  (start-console)
  (start-browser)
  (start-http-proxy)
  (start-router))