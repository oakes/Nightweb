(ns nightweb.browser
  (:use splendid.jfx)
  (:import (javafx.scene.layout VBox HBox)
           (javafx.scene.control TabPane Tab Button TextField)
           javafx.scene.web.WebView
           javafx.scene.layout.Priority
           javafx.geometry.Insets
           javafx.scene.text.Font
           javafx.geometry.Pos
           javafx.beans.value.ChangeListener))

(defn create-tab
  "Create a new tab."
  [tab-bar]
  (let [new-tab (Tab. "Main Page")
        tab-view (VBox.)
        nav-bar (HBox.)
        reload-icon "⟳"
        stop-icon "x"
        back-btn (Button. "<")
        for-btn (Button. ">")
        reload-btn (Button. reload-icon)
        url-field (TextField.)
        web-view (WebView.)
        web-engine (.getEngine web-view)
        index (- (.size (.getTabs tab-bar)) 1)]
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
    (.addListener (.stateProperty (.getLoadWorker web-engine))
                  (proxy [ChangeListener] []
                    (changed [ov oldState newState]
                             (if (= newState javafx.concurrent.Worker$State/RUNNING)
                               (do
                                 (.setText url-field (.getLocation web-engine))
                                 (.setText reload-btn stop-icon))
                               (.setText reload-btn reload-icon)))))
    ; load the main page
    (.load web-engine "http://localhost:4707")
    ; set spacing for the nav bar
    (.setAlignment nav-bar javafx.geometry.Pos/CENTER)
    (.setPadding nav-bar (Insets. 4))
    (.setSpacing nav-bar 2)
    ; add everything to the window
    (add nav-bar [back-btn for-btn reload-btn url-field])
    (add tab-view [nav-bar web-view])
    (.setContent new-tab tab-view)
    (.add (.getTabs tab-bar) index new-tab)
    ; select the new tab
    (.select (.getSelectionModel tab-bar) index)))

(defn start-browser
  "Launch the JavaFX browser (non-blocking)."
  []
  (jfx (let [window (VBox.)
             tab-bar (TabPane.)
             add-tab (Tab. " + ")]
         (.setTabClosingPolicy
           tab-bar
           javafx.scene.control.TabPane$TabClosingPolicy/ALL_TABS)
         (.setClosable add-tab false)
         (.add (.getTabs tab-bar) add-tab)
         (defhandler :onSelectionChanged add-tab (create-tab tab-bar))
         (create-tab tab-bar)
         (VBox/setVgrow tab-bar Priority/ALWAYS)
         (add window [tab-bar])
         (show window))))