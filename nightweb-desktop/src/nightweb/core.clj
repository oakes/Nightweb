(ns nightweb.core
  (:use nightweb.browser)
  (:import net.i2p.router.Router
           net.i2p.i2ptunnel.I2PTunnel
           net.i2p.util.EventDispatcher)
  (:gen-class))

(defn start-http-proxy
  "Launch the http proxy."
  []
  (java.lang.System/setProperty "http.proxyHost" "localhost")
  (java.lang.System/setProperty "http.proxyPort" "4708")
  (java.lang.System/setProperty "https.proxyHost" "localhost")
  (java.lang.System/setProperty "https.proxyPort" "4708")
  (future (I2PTunnel/main
            (into-array java.lang.String ["-nogui" "-nocli" "-e" "httpclient 4708"]))))

(defn start-router
  "Launch the router."
  []
  (Router/main nil))

(defn -main
  "Launch everything."
  [& args]
  (start-http-proxy)
  (start-browser)
  (comment (start-router)))