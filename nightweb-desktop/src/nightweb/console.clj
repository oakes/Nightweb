(ns nightweb.console
  (:require [nightweb.browser :as browser]
            [nightweb.db :as db]
            [splendid.jfx :as jfx])
  (:import (javafx.scene.layout VBox BorderPane Priority TilePane ColumnConstraints)
           (javafx.scene.control TabPane Tab Label)
           (javafx.scene.text Font TextAlignment)
           javafx.geometry.Insets))

(defn create-section-item
  [text]
  (let [border-pane (BorderPane.)
        label (Label. text)]
    (.setFont label (Font. 18))
    (.setPrefSize border-pane 250 50)
    (.setPadding border-pane (Insets. 5))
    (jfx/defhandler :onMouseEntered border-pane
                    (.setStyle border-pane "-fx-background-color: gray"))
    (jfx/defhandler :onMouseExited border-pane
                    (.setStyle border-pane "-fx-background-color: lightgray"))
    (.setTop border-pane label)
    border-pane))

(defn create-section
  [title]
  (let [vbox (VBox.)
        label (Label. title)
        tile-pane (TilePane.)]
    (.setFont label (Font. 24))
    (.addAll (.getChildren tile-pane)
             [(create-section-item "Test1")
              (create-section-item "Test2")
              (create-section-item "Test3")
              (create-section-item "Test4")
              (create-section-item "Test5")
              (create-section-item "Test6")
              (create-section-item "Test7")
              (create-section-item "Test8")])
    (.setPadding vbox (Insets. 10))
    (jfx/add vbox label)
    (jfx/add vbox tile-pane)
    vbox))

(defn create-console-tab
  "Create a new console tab."
  []
  (let [new-tab (Tab. "Console")
        vbox (VBox.)]
    (jfx/add vbox (create-section "Websites"))
    (jfx/add vbox (create-section "Settings"))
    (.setStyle vbox "-fx-background-color: lightgray")
    (.setContent new-tab vbox)
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