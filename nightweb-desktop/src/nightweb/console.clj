(ns nightweb.console
  (:use splendid.jfx)
  (:import (javafx.scene.control Tab ScrollPane)))

(defn create-console-tab
  "Create a new console tab."
  []
  (let [new-tab (Tab. "Console")
        tab-view (ScrollPane.)]
    (.setContent new-tab tab-view)
    (.setMinWidth tab-view 800)
    (.setMinHeight tab-view 600)
    new-tab))