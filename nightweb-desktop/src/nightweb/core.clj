(ns nightweb.core
  (:use nightweb.page
        nightweb.browser)
  (:import net.i2p.router.Router)
  (:gen-class))

(defn start-router
  "Launch the router (blocking)."
  []
  (Router/main nil))

(defn -main
  "Launch everything."
  [& args]
  (start-web-server)
  (start-browser)
  (comment (start-router)))
