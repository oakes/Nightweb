(ns nightweb.core
  (:use splendid.jfx
        nightweb.page)
  (:import javafx.scene.layout.VBox
           javafx.scene.layout.HBox
           javafx.scene.control.TabPane
           javafx.scene.control.Tab
           javafx.scene.web.WebView
           javafx.scene.layout.Priority
           javafx.scene.control.Button
           javafx.scene.control.TextField
           javafx.geometry.Insets
           net.i2p.router.Router)
  (:gen-class))

(defn start-browser
  "Launch the JavaFX browser (non-blocking)."
  []
  (jfx (let [window (VBox.)
             tab-view (VBox.)
             nav-bar (HBox.)
             back-btn (Button. "<-")
             for-btn (Button. "->")
             reload-btn (Button. "Reload")
             url-field (TextField.)
             tab-bar (TabPane.)
             main-tab (Tab. "Main Page")
             add-tab (Tab. "+")
             web-view (WebView.)]
         (.setPadding nav-bar (Insets. 4))
         (.setSpacing nav-bar 2)
         (add nav-bar [back-btn for-btn reload-btn url-field])
         (HBox/setHgrow url-field Priority/ALWAYS)
         (add tab-view [nav-bar web-view])
         (.setClosable add-tab false)
         (.setContent main-tab tab-view)
         (.add (.getTabs tab-bar) main-tab)
         (.add (.getTabs tab-bar) add-tab)
         (.load (.getEngine web-view) "http://localhost:8080")
         (VBox/setVgrow tab-bar Priority/ALWAYS)
         (add window [tab-bar])
         (show window))))

(defn start-router
  "Launch the router (blocking)."
  []
  (Router/main nil))

(defn -main
  "Launch everything."
  [& args]
  (start-web-server)
  (start-browser)
  (comment (start-router)))
