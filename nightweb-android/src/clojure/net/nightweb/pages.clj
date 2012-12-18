(ns net.nightweb.pages
  (:use [neko.resource :only [get-resource get-string]]
        [neko.activity :only [set-content-view!]]
        [net.nightweb.clandroid.activity :only [defactivity]]
        [net.nightweb.clandroid.service :only [bind-service unbind-service]]
        [net.nightweb.views :only [create-tab
                                   get-grid-view
                                   get-user-view
                                   get-category-view]]
        [net.nightweb.menus :only [create-main-menu]]
        [net.nightweb.actions :only [do-menu-action]]
        [nightweb.db :only [defspec
                            run-query
                            drop-tables
                            create-tables
                            insert-test-data]]))

(defactivity
  net.nightweb.MainPage
  :on-create
  (fn [this bundle]
    ; create database
    (defspec (.getPath (.getFilesDir this)) nil)
    (run-query drop-tables nil nil)
    (run-query create-tables nil nil)
    (run-query insert-test-data nil nil)
    ; start service
    (def conn (bind-service this
                            "net.nightweb.MainService"
                            (fn [service] (.act service :test))))
    ; create main ui
    (let [action-bar (.getActionBar this)]
      (.setNavigationMode action-bar android.app.ActionBar/NAVIGATION_MODE_TABS)
      (.setDisplayShowTitleEnabled action-bar false)
      (.setDisplayShowHomeEnabled action-bar false)
      (create-tab action-bar
                  (get-string :me)
                  (get-user-view this {:hash (byte-array (map byte [0]))}))
      (create-tab action-bar 
                  (get-string :users)
                  (get-category-view this {:type :users} true))
      (create-tab action-bar
                  (get-string :photos)
                  (get-category-view this {:type :photos} true))
      (create-tab action-bar
                  (get-string :videos)
                  (get-category-view this {:type :videos} true))
      (create-tab action-bar
                  (get-string :audio)
                  (get-category-view this {:type :audio} true)))
    ; make sure activity shuts down when notification is touched
    (def activity-receiver (proxy [android.content.BroadcastReceiver] []
                             (onReceive [context intent]
                               (.finish context))))
    (.registerReceiver this
                       activity-receiver
                       (android.content.IntentFilter. "ACTION_CLOSE_APP")))
  :on-destroy
  (fn [this]
    (.unregisterReceiver this activity-receiver)
    (unbind-service this conn))
  :on-create-options-menu
  (fn [this menu]
    (create-main-menu this menu true)))

(defactivity
  net.nightweb.FavoritesPage
  :on-create
  (fn [this bundle]
    (let [extras (.getExtras (.getIntent this))
          params-str (if extras (.getString extras "params") nil)
          params (if params-str (read-string params-str) {})
          action-bar (.getActionBar this)]
      (.setNavigationMode action-bar android.app.ActionBar/NAVIGATION_MODE_TABS)
      (.setDisplayHomeAsUpEnabled action-bar true)
      (.setTitle action-bar (get-string :favorites))
      (create-tab action-bar
                  (get-string :users)
                  (get-category-view this {:type :favorites}))
      (create-tab action-bar
                  (get-string :photos)
                  (get-category-view this {:type :favorites}))
      (create-tab action-bar
                  (get-string :videos)
                  (get-category-view this {:type :favorites}))
      (create-tab action-bar
                  (get-string :audio)
                  (get-category-view this {:type :favorites}))))
  :on-create-options-menu
  (fn [this menu]
    (create-main-menu this menu true))
  :on-options-item-selected
  (fn [this item]
    (do-menu-action this item)))

(defactivity
  net.nightweb.DownloadsPage
  :on-create
  (fn [this bundle]
    (let [action-bar (.getActionBar this)]
      (.setNavigationMode action-bar android.app.ActionBar/NAVIGATION_MODE_TABS)
      (.setDisplayHomeAsUpEnabled action-bar true)
      (.setTitle action-bar (get-string :downloads))
      (create-tab action-bar
                  (get-string :all)
                  (get-category-view this {:type :downloads}))
      (create-tab action-bar
                  (get-string :photos)
                  (get-category-view this {:type :downloads}))
      (create-tab action-bar
                  (get-string :videos)
                  (get-category-view this {:type :downloads}))
      (create-tab action-bar
                  (get-string :audio)
                  (get-category-view this {:type :downloads}))))
  :on-create-options-menu
  (fn [this menu]
    (create-main-menu this menu false))
  :on-options-item-selected
  (fn [this item]
    (do-menu-action this item)))

(defactivity
  net.nightweb.GridPage
  :def grid-page
  :on-create
  (fn [this bundle]
    (let [extras (.getExtras (.getIntent this))
          params-str (if extras (.getString extras "params") nil)
          params (if params-str (read-string params-str) {})
          action-bar (.getActionBar this)
          grid-view (get-grid-view this [])]
      (.setDisplayHomeAsUpEnabled action-bar true)
      (if-let [title (get params :title)]
        (.setTitle action-bar title)
        (.setDisplayShowTitleEnabled action-bar false))
      (set-content-view! grid-page grid-view)))
  :on-create-options-menu
  (fn [this menu]
    (create-main-menu this menu true))
  :on-options-item-selected
  (fn [this item]
    (do-menu-action this item)))
