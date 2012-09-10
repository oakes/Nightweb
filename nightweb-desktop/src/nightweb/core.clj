(ns nightweb.core
  (:use nightweb.console
        nightweb.browser)
  (:import net.i2p.router.Router
           net.i2p.i2ptunnel.I2PTunnel)
  (:gen-class))

(defn start-http-proxy
  "Launch the http proxy (non-blocking)."
  []
  (java.lang.System/setProperty "javafx.autoproxy.disable" "true")
  (java.lang.System/setProperty "http.proxyHost" "localhost")
  (java.lang.System/setProperty "http.proxyPort" "4708")
  (future (I2PTunnel/main
    (into-array java.lang.String ["-nogui" "-nocli" "-e" "httpclient 4708"]))))

(defn start-router
  "Launch the router (blocking)."
  []
  (Router/main nil))

(defn -main
  "Launch everything."
  [& args]
  (start-http-proxy)
  (start-console)
  (start-browser)
  (start-router))