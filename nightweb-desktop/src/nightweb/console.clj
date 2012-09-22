(ns nightweb.console
  (:use nightweb.browser
        nightweb.db
        splendid.jfx)
  (:import javafx.scene.layout.VBox
           (javafx.scene.control TabPane Tab ScrollPane)
           javafx.scene.layout.Priority))

(defn create-console-tab
  "Create a new console tab."
  []
  (let [new-tab (Tab. "Console")
        tab-view (ScrollPane.)]
    (.setContent new-tab tab-view)
    (.setMinWidth tab-view 800)
    (.setMinHeight tab-view 600)
    new-tab))

(defn add-tab
  "Create a tab with the supplied function and add it to the tab bar."
  [tab-bar create-tab]
  (let [index (- (.size (.getTabs tab-bar)) 1)
        new-tab (create-tab)]
    (.add (.getTabs tab-bar) index new-tab)
    (.select (.getSelectionModel tab-bar) index)))

(defn start-console
  "Launch the JavaFX browser."
  []
  (jfx (let [window (VBox.)
             tab-bar (TabPane.)
             plus-tab (Tab. " + ")]
         (create-database)
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