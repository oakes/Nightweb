(ns nightweb.page
  (:use noir.core)
  (:require [noir.server :as server]))

(defpage "/" []
    "Welcome to Noir!")

(defn start-web-server
  "Launch the Noir server (non-blocking)."
  []
  (server/start 8080))