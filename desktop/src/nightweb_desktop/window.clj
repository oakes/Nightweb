(ns nightweb-desktop.window
  (:require [splendid.jfx :as jfx])
  (:import (javafx.scene.layout VBox Priority)
           (javafx.scene.control TabPane Tab)
           javafx.scene.web.WebView
           javafx.beans.value.ChangeListener)
  (:use [nightweb.router :only [stop-router]]
        [nightweb-desktop.server :only [port]]))

(defn create-tab
  "Creates a new tab."
  []
  (let [new-tab (Tab.)
        web-view (WebView.)
        web-engine (.getEngine web-view)
        history (.getHistory web-engine)
        dots "..."]
    (VBox/setVgrow web-view Priority/ALWAYS)
    (.addListener (.stateProperty (.getLoadWorker web-engine))
                  (proxy [ChangeListener] []
                    (changed [ov old-state new-state]
                      (if (= new-state javafx.concurrent.Worker$State/RUNNING)
                        (.setText new-tab dots)
                        (.setText new-tab (or (.getTitle web-engine) dots))))))
    (.load web-engine (str "http://localhost:" port))
    (.setContent new-tab web-view)
    new-tab))

(defn add-tab
  "Creates a tab and adds it to the tab bar."
  [tab-bar]
  (let [index (- (.size (.getTabs tab-bar)) 1)
        new-tab (create-tab)]
    (.add (.getTabs tab-bar) index new-tab)
    (.select (.getSelectionModel tab-bar) index)))

(defn start-window
  "Launches a JavaFX window."
  []
  (jfx/jfx
    (let [window (VBox.)
          tab-bar (TabPane.)
          plus-tab (Tab. " + ")]
      (.setTabClosingPolicy
        tab-bar javafx.scene.control.TabPane$TabClosingPolicy/ALL_TABS)
      (.setClosable plus-tab false)
      (.add (.getTabs tab-bar) plus-tab)
      (jfx/defhandler :onSelectionChanged plus-tab (add-tab tab-bar))
      (.addListener (.getTabs tab-bar)
                    (reify javafx.collections.ListChangeListener
                      (onChanged [this change]
                        (let [tabs (butlast (.getTabs tab-bar))]
                          (doseq [tab tabs]
                            (.setClosable tab (> (count tabs) 1)))))))
      (add-tab tab-bar)
      (VBox/setVgrow tab-bar Priority/ALWAYS)
      (.setWidth jfx/primary-stage 1024)
      (.setMinWidth jfx/primary-stage 800)
      (.setHeight jfx/primary-stage 768)
      (.setMinHeight jfx/primary-stage 600)
      (jfx/add window [tab-bar])
      (jfx/show window)
      (.setOnCloseRequest jfx/primary-stage
                          (reify javafx.event.EventHandler
                            (handle [this event]
                              (stop-router)
                              (java.lang.System/exit 0)))))))
