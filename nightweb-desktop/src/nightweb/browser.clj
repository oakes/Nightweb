(ns nightweb.browser
  (:use splendid.jfx
        nightweb.console)
  (:import (javafx.scene.layout VBox HBox)
           (javafx.scene.control TabPane Tab Button TextField)
           (javafx.geometry Insets Pos)
           javafx.scene.web.WebView
           javafx.scene.layout.Priority
           javafx.scene.text.Font
           javafx.beans.value.ChangeListener))

(defn create-browser-tab
  "Create a new browser tab."
  []
  (let [new-tab (Tab. "Browser")
        tab-view (VBox.)
        nav-bar (HBox.)
        reload-icon "⟳"
        stop-icon "x"
        back-btn (Button. "<")
        for-btn (Button. ">")
        reload-btn (Button. reload-icon)
        url-field (TextField.)
        web-view (WebView.)
        web-engine (.getEngine web-view)]
    ; resize the controls
    (.setFont back-btn (Font. 16))
    (.setFont for-btn (Font. 16))
    (.setFont reload-btn (Font. 16))
    (.setMinHeight url-field 28)
    (HBox/setHgrow url-field Priority/ALWAYS)
    (VBox/setVgrow web-view Priority/ALWAYS)
    ; set actions for the controls
    (defhandler :onAction back-btn
                (if (> (.getCurrentIndex (.getHistory web-engine)) 0)
                  (.go (.getHistory web-engine) -1)))
    (defhandler :onAction for-btn
                (if (< (+ (.getCurrentIndex (.getHistory web-engine)) 1)
                       (.size (.getEntries (.getHistory web-engine))))
                  (.go (.getHistory web-engine) 1)))
    (defhandler :onAction reload-btn
                (if (= (.getText reload-btn) reload-icon)
                  (.reload web-engine)
                  (.cancel (.getLoadWorker web-engine))))
    (defhandler :onAction url-field
                (.load web-engine (.getText url-field)))
    ; things to do the loading state changes
    (.addListener (.stateProperty (.getLoadWorker web-engine))
                  (proxy [ChangeListener] []
                    (changed [ov oldState newState]
                             (if (= newState javafx.concurrent.Worker$State/RUNNING)
                               (do
                                 (.setText reload-btn stop-icon)
                                 (.setText url-field (.getLocation web-engine)))
                               (do
                                 (.setText reload-btn reload-icon)
                                 (if (.getTitle web-engine)
                                   (.setText new-tab (.getTitle web-engine))))))))
    ; set spacing for the nav bar
    (.setAlignment nav-bar javafx.geometry.Pos/CENTER)
    (.setPadding nav-bar (Insets. 4))
    (.setSpacing nav-bar 2)
    ; add everything to the window
    (add nav-bar [back-btn for-btn reload-btn url-field])
    (add tab-view [nav-bar web-view])
    (.setContent new-tab tab-view)
    new-tab))

(defn add-tab
  "Create a tab with the supplied function and add it to the tab bar."
  [tab-bar create-tab]
  (let [index (- (.size (.getTabs tab-bar)) 1)
        new-tab (create-tab)]
    (.add (.getTabs tab-bar) index new-tab)
    (.select (.getSelectionModel tab-bar) index)))

(defn start-browser
  "Launch the JavaFX browser."
  []
  (jfx (let [window (VBox.)
             tab-bar (TabPane.)
             plus-tab (Tab. " + ")]
         (.setTabClosingPolicy
           tab-bar
           javafx.scene.control.TabPane$TabClosingPolicy/ALL_TABS)
         (.setClosable plus-tab false)
         (.add (.getTabs tab-bar) plus-tab)
         (defhandler :onSelectionChanged plus-tab (add-tab tab-bar create-console-tab))
         (add-tab tab-bar create-console-tab)
         (VBox/setVgrow tab-bar Priority/ALWAYS)
         (add window [tab-bar])
         (show window))))