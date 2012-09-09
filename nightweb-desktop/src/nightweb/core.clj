(ns nightweb.core
  (:use nightweb.page
        nightweb.browser)
  (:import net.i2p.router.Router
           net.i2p.i2ptunnel.I2PTunnel)
  (:gen-class))

(defn start-proxies
  "Launch the proxies (non-blocking)."
  []
  (future (I2PTunnel/main
    (into-array java.lang.String
                ["-nogui"
                 "-nocli"
                 "-e"
                 "httpServer localhost 8081 localhost router.keys"]))))

(defn start-router
  "Launch the router (blocking)."
  []
  (Router/main (into-array java.lang.String [])))

(defn -main
  "Launch everything."
  [& args]
  (start-main-page)
  (start-browser)
  (start-proxies)
  (comment (start-router)))
