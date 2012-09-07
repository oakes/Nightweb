(ns nightweb.core
  (:use splendid.jfx)
  (:import javafx.scene.layout.VBox
           javafx.scene.control.TabPane
           javafx.scene.control.Tab
           javafx.scene.web.WebView
           javafx.scene.layout.Priority
           net.i2p.router.Router
           java.util.HashMap
           winstone.Launcher)
  (:gen-class))

(defn start-web-server
  "Launch the Winstone server"
  []
  (let [args (HashMap.)]
    (Launcher/initLogger args)
    (Launcher. args)))

(defn start-browser
  "Launch the JavaFX browser"
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
  "Launch the router"
  []
  (Router/main nil))

(defn -main
  "Launch everything"
  [& args]
  (start-web-server)
  (start-browser)
  (comment (start-router)))
