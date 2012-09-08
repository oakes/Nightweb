(ns nightweb.browser
  (:use splendid.jfx)
  (:import (javafx.scene.layout VBox HBox)
           (javafx.scene.control TabPane Tab Button TextField)
           javafx.scene.web.WebView
           javafx.scene.layout.Priority
           javafx.geometry.Insets
           javafx.scene.text.Font
           javafx.geometry.Pos))

(defn create-tab
  "Creates a new tab."
  [tab-bar]
  (let [new-tab (Tab. "Main Page")
        tab-view (VBox.)
        nav-bar (HBox.)
        back-btn (Button. "<")
        for-btn (Button. ">")
        reload-btn (Button. "⟳")
        url-field (TextField.)
        web-view (WebView.)
        index (- (.size (.getTabs tab-bar)) 1)]
    ; resize the controls
    (.setFont back-btn (Font. 16))
    (.setFont for-btn (Font. 16))
    (.setFont reload-btn (Font. 16))
    (.setMinHeight url-field 28)
    (HBox/setHgrow url-field Priority/ALWAYS)
    ; load the main page
    (.load (.getEngine web-view) "http://localhost:8080")
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