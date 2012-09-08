(ns nightweb.browser
  (:use splendid.jfx)
  (:import javafx.scene.layout.VBox
           javafx.scene.layout.HBox
           javafx.scene.control.TabPane
           javafx.scene.control.Tab
           javafx.scene.web.WebView
           javafx.scene.layout.Priority
           javafx.scene.control.Button
           javafx.scene.control.TextField
           javafx.geometry.Insets
           javafx.event.EventHandler))

(defn create-tab
  "Creates a new tab."
  [tab-bar]
  (let [new-tab (Tab. "Main Page")
        tab-view (VBox.)
        nav-bar (HBox.)
        back-btn (Button. "<-")
        for-btn (Button. "->")
        reload-btn (Button. "Reload")
        url-field (TextField.)
        web-view (WebView.)
        index (- (.size (.getTabs tab-bar)) 1)]
    (.setPadding nav-bar (Insets. 4))
    (.setSpacing nav-bar 2)
    (add nav-bar [back-btn for-btn reload-btn url-field])
    (HBox/setHgrow url-field Priority/ALWAYS)
    (add tab-view [nav-bar web-view])
    (.load (.getEngine web-view) "http://localhost:8080")
    (.setContent new-tab tab-view)
    (.add (.getTabs tab-bar) index new-tab)
    (.select (.getSelectionModel tab-bar) index)))

(defn start-browser
  "Launch the JavaFX browser (non-blocking)."
  []
  (jfx (let [window (VBox.)
             tab-bar (TabPane.)
             add-tab (Tab. " + ")]
         (.setClosable add-tab false)
         (.add (.getTabs tab-bar) add-tab)
         (defhandler :onSelectionChanged add-tab (create-tab tab-bar))
         (create-tab tab-bar)
         (VBox/setVgrow tab-bar Priority/ALWAYS)
         (add window [tab-bar])
         (show window))))