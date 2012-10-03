(ns nightweb.router
  (:import net.i2p.router.Router))

(defn start-router
  "Launch the router."
  []
  (Router/main nil))
