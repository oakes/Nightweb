(ns nightweb.console
  (:use splendid.jfx)
  (:import javafx.scene.layout.VBox
           javafx.scene.control.Tab))

(defn create-console-tab
  "Create a new console tab."
  []
  (let [new-tab (Tab. "Console Tab")
        tab-view (VBox.)]
    (.setContent new-tab tab-view)
    new-tab))