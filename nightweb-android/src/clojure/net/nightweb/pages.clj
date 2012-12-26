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

(defn start-service
  [context]
  (let [service (bind-service context
                              "net.nightweb.MainService"
                              (fn [service] (.act service :test)))]
    (swap! (.state context) assoc :service service)))

(defn stop-service
  [context]
  (if-let [service (get @(.state context) :service)]
    (unbind-service context service)))

(defn start-receiver
  [context]
  (let [receiver (proxy [android.content.BroadcastReceiver] []
                   (onReceive [context intent]
                     (.finish context)))]
    (.registerReceiver context
                       receiver
                       (android.content.IntentFilter. "ACTION_CLOSE_APP"))
    (swap! (.state context) assoc :receiver receiver)))

(defn stop-receiver
  [context]
  (if-let [receiver (get @(.state context) :receiver)]
    (.unregisterReceiver context receiver)))

(defactivity
  net.nightweb.MainPage
  :on-create
  (fn [this bundle]
    ; create database
    (defspec (.getPath (.getFilesDir this)) nil)
    (run-query drop-tables nil nil)
    (run-query create-tables nil nil)
    (run-query insert-test-data nil nil)
    ; start service and receiver
    (start-service this)
    (start-receiver this)
    ; create ui
    (let [action-bar (.getActionBar this)]
      (.setNavigationMode action-bar android.app.ActionBar/NAVIGATION_MODE_TABS)
      (.setDisplayShowTitleEnabled action-bar false)
      (.setDisplayShowHomeEnabled action-bar false)
      (create-tab action-bar
                  (get-string :me)
                  #(get-user-view this {:hash (byte-array (map byte [0]))}))
      (create-tab action-bar
                  (get-string :users)
                  #(get-category-view this {:type :users} true))
      (create-tab action-bar
                  (get-string :photos)
                  #(get-category-view this {:type :photos} true))
      (create-tab action-bar
                  (get-string :videos)
                  #(get-category-view this {:type :videos} true))
      (create-tab action-bar
                  (get-string :audio)
                  #(get-category-view this {:type :audio} true))))
  :on-destroy
  (fn [this]
    (stop-receiver this)
    (stop-service this))
  :on-create-options-menu
  (fn [this menu]
    (create-main-menu this menu true)))

(defactivity
  net.nightweb.FavoritesPage
  :on-create
  (fn [this bundle]
    ; start service and receiver
    (start-service this)
    (start-receiver this)
    ; create ui
    (let [params (into {} (.getSerializableExtra (.getIntent this) "params"))
          action-bar (.getActionBar this)]
      (.setNavigationMode action-bar android.app.ActionBar/NAVIGATION_MODE_TABS)
      (.setDisplayHomeAsUpEnabled action-bar true)
      (.setTitle action-bar (get-string :favorites))
      (create-tab action-bar
                  (get-string :users)
                  #(get-category-view this
                                      (assoc params :type :users-favorites)))
      (create-tab action-bar
                  (get-string :photos)
                  #(get-category-view this
                                      (assoc params :type :photos-favorites)))
      (create-tab action-bar
                  (get-string :videos)
                  #(get-category-view this
                                      (assoc params :type :videos-favorites)))
      (create-tab action-bar
                  (get-string :audio)
                  #(get-category-view this
                                      (assoc params :type :audio-favorites)))))
  :on-destroy
  (fn [this]
    (stop-receiver this)
    (stop-service this))
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
    ; start service and receiver
    (start-service this)
    (start-receiver this)
    ; create ui
    (let [action-bar (.getActionBar this)]
      (.setNavigationMode action-bar android.app.ActionBar/NAVIGATION_MODE_TABS)
      (.setDisplayHomeAsUpEnabled action-bar true)
      (.setTitle action-bar (get-string :downloads))
      (create-tab action-bar
                  (get-string :all)
                  #(get-category-view this {:type :all-downloads}))
      (create-tab action-bar
                  (get-string :photos)
                  #(get-category-view this {:type :photos-downloads}))
      (create-tab action-bar
                  (get-string :videos)
                  #(get-category-view this {:type :videos-downloads}))
      (create-tab action-bar
                  (get-string :audio)
                  #(get-category-view this {:type :audio-downloads}))))
  :on-destroy
  (fn [this]
    (stop-receiver this)
    (stop-service this))
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
    ; start service and receiver
    (start-service this)
    (start-receiver this)
    ; create ui
    (let [params (.getSerializableExtra (.getIntent this) "params")
          action-bar (.getActionBar this)
          grid-view (case (get params :type)
                      :users (get-user-view this params)
                      (get-grid-view this []))]
      (.setDisplayHomeAsUpEnabled action-bar true)
      (if-let [title (get params :text)]
        (.setTitle action-bar title)
        (.setDisplayShowTitleEnabled action-bar false))
      (set-content-view! grid-page grid-view)))
  :on-destroy
  (fn [this]
    (stop-receiver this)
    (stop-service this))
  :on-create-options-menu
  (fn [this menu]
    (create-main-menu this menu true))
  :on-options-item-selected
  (fn [this item]
    (do-menu-action this item)))
