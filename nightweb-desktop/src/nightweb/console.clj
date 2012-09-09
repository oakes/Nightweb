(ns nightweb.console
  (:use noir.core)
  (:require [noir.server :as server]))

(defpage "/" []
    "Welcome to Noir!")

(defn start-console
  "Launch the Noir server (non-blocking)."
  []
  (server/start 4707))