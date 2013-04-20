(ns nightweb-desktop.window
  (:require [splendid.jfx :as jfx])
  (:import (javafx.scene.layout VBox Priority)
           (javafx.scene.control TabPane Tab)
           javafx.scene.web.WebView
           javafx.beans.value.ChangeListener)
  (:use [nightweb.router :only [stop-router]]
        [nightweb-desktop.server :only [port]]))

(defn create-webview
  "Creates a new WebView."
  []
  (let [web-view (WebView.)
        web-engine (.getEngine web-view)]
    (VBox/setVgrow web-view Priority/ALWAYS)
    (.load web-engine (str "http://localhost:" port))
    web-view))

(defn start-window
  "Launches a JavaFX window."
  []
  (jfx/jfx
    (let [window (VBox.)
          web-view (create-webview)]
      (.setWidth jfx/primary-stage 1024)
      (.setMinWidth jfx/primary-stage 800)
      (.setHeight jfx/primary-stage 768)
      (.setMinHeight jfx/primary-stage 600)
      (jfx/add window [web-view])
      (jfx/show window)
      (.setOnCloseRequest jfx/primary-stage
                          (reify javafx.event.EventHandler
                            (handle [this event]
                              (stop-router)
                              (java.lang.System/exit 0)))))))
