(ns nightweb.core
  (:use splendid.jfx)
  (:import javafx.scene.layout.VBox
           javafx.scene.control.TabPane
           javafx.scene.control.Tab
           javafx.scene.web.WebView
           javafx.scene.layout.Priority
           net.i2p.router.Router))

(defn -main
  "I don't do a whole lot."
  [& args]
  (jfx (let [vbox (VBox.)
             tp (TabPane.)
             tab (Tab.)
             wv (WebView.)]
         (.setText tab "Main Page")
         (.setContent tab wv)
         (.add (.getTabs tp) tab)
         (.load (.getEngine wv) "https://www.google.com")
         (VBox/setVgrow tp Priority/ALWAYS)
         (add vbox [tp])
         (show vbox)))
  (Router/main nil))
