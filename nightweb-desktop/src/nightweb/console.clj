(ns nightweb.console
  (:require [nightweb.browser :as browser]
            [nightweb.db :as db]
            [splendid.jfx :as jfx])
  (:import (javafx.scene.layout VBox Priority TilePane ColumnConstraints)
           (javafx.scene.control TabPane Tab Label)
           (javafx.geometry Insets Pos)
           javafx.scene.text.Font))

(defn create-grid-item
  [text]
  (let [item (Label. text)]
    (.setFont item (Font. 24))
    item))

(defn create-console-tab
  "Create a new console tab."
  []
  (let [new-tab (Tab. "Console")
        tab-view (TilePane.)]
    (.setPrefTileHeight tab-view 50)
    (.setPrefTileWidth tab-view 150)
    (.setPadding tab-view (Insets. 10))
    (.setTileAlignment tab-view Pos/CENTER_LEFT)
    (.addAll (.getChildren tab-view)
             [(create-grid-item "Test")
              (create-grid-item "Test2")
              (create-grid-item "Test3")
              (create-grid-item "Test4")
              (create-grid-item "Test5")
              (create-grid-item "Test6")
              (create-grid-item "Test7")
              (create-grid-item "Test8")])
    (.setContent new-tab tab-view)
    new-tab))

(defn add-tab
  "Create a tab with the supplied function and add it to the tab bar."
  [tab-bar create-tab]
  (let [index (- (.size (.getTabs tab-bar)) 1)
        new-tab (create-tab)]
    (.add (.getTabs tab-bar) index new-tab)
    (.select (.getSelectionModel tab-bar) index)))

(defn start-console
  "Launch the JavaFX console."
  []
  (jfx/jfx (let [window (VBox.)
             tab-bar (TabPane.)
             plus-tab (Tab. " + ")]
         ;(db/create-database)
         (.setTabClosingPolicy
           tab-bar
           javafx.scene.control.TabPane$TabClosingPolicy/ALL_TABS)
         (.setClosable plus-tab false)
         (.add (.getTabs tab-bar) plus-tab)
         (jfx/defhandler :onSelectionChanged plus-tab (add-tab tab-bar create-console-tab))
         (add-tab tab-bar create-console-tab)
         (VBox/setVgrow tab-bar Priority/ALWAYS)
         (.setWidth jfx/primary-stage 800)
         (.setHeight jfx/primary-stage 600)
         (jfx/add window [tab-bar])
         (jfx/show window))))