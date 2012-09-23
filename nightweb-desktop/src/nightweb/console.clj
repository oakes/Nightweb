(ns nightweb.console
  (:require [nightweb.browser :as browser]
            [nightweb.db :as db]
            [splendid.jfx :as jfx])
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
  (jfx/jfx (let [window (VBox.)
             tab-bar (TabPane.)
             plus-tab (Tab. " + ")]
         (db/create-database)
         (.setTabClosingPolicy
           tab-bar
           javafx.scene.control.TabPane$TabClosingPolicy/ALL_TABS)
         (.setClosable plus-tab false)
         (.add (.getTabs tab-bar) plus-tab)
         (jfx/defhandler :onSelectionChanged plus-tab (add-tab tab-bar create-console-tab))
         (add-tab tab-bar create-console-tab)
         (VBox/setVgrow tab-bar Priority/ALWAYS)
         (jfx/add window [tab-bar])
         (jfx/show window))))