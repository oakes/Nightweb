(ns nightweb.core
  (:use splendid.jfx
        nightweb.page)
  (:import javafx.scene.layout.VBox
           javafx.scene.control.TabPane
           javafx.scene.control.Tab
           javafx.scene.web.WebView
           javafx.scene.layout.Priority
           net.i2p.router.Router)
  (:gen-class))

(defn start-browser
  "Launch the JavaFX browser (non-blocking)."
  []
  (jfx (let [vbox (VBox.)
             tp (TabPane.)
             tab (Tab.)
             wv (WebView.)]
         (.setText tab "Main Page")
         (.setContent tab wv)
         (.add (.getTabs tp) tab)
         (.load (.getEngine wv) "http://localhost:8080")
         (VBox/setVgrow tp Priority/ALWAYS)
         (add vbox [tp])
         (show vbox))))

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
