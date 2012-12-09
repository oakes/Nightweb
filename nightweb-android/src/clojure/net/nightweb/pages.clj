(ns net.nightweb.pages
  (:use [neko.resource :only [get-resource get-string]]
        [neko.activity :only [set-content-view!]]
        [net.nightweb.clandroid.activity :only [defactivity]]
        [net.nightweb.clandroid.service :only [bind-service unbind-service]]
        [net.nightweb.views :only [create-tab
                                   get-grid-view
                                   get-new-post-view]]
        [net.nightweb.menus :only [create-main-menu]]
        [net.nightweb.actions :only [do-menu-action
                                     show-profile
                                     show-favorites
                                     show-downloads
                                     show-tags]]))

(defactivity
  net.nightweb.MainPage
  :on-create
  (fn [this bundle]
    (def conn (bind-service this
                            "net.nightweb.MainService"
                            (fn [service] (.act service :test))))
    (let [action-bar (.getActionBar this)
          me-view (get-grid-view this
                                 [{:title (get-string :profile)
                                   :func show-profile}
                                  {:title (get-string :favorites)
                                   :func show-favorites}
                                  {:title (get-string :downloads)
                                   :func show-downloads}])
          users-view (get-grid-view this
                                    [{:title (get-string :tags)
                                      :func show-tags}])
          photos-view (get-grid-view this
                                    [{:title (get-string :tags)
                                      :func show-tags}])
          videos-view (get-grid-view this
                                    [{:title (get-string :tags)
                                      :func show-tags}])
          audio-view (get-grid-view this
                                    [{:title (get-string :tags)
                                      :func show-tags}])]
      (.setNavigationMode action-bar android.app.ActionBar/NAVIGATION_MODE_TABS)
      (.setDisplayShowTitleEnabled action-bar false)
      (.setDisplayShowHomeEnabled action-bar false)
      (create-tab action-bar (get-string :me) me-view)
      (create-tab action-bar (get-string :users) users-view)
      (create-tab action-bar (get-string :photos) photos-view)
      (create-tab action-bar (get-string :videos) videos-view)
      (create-tab action-bar (get-string :audio) audio-view))
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
    (create-main-menu this menu)))

(defactivity
  net.nightweb.FavoritesPage
  :on-create
  (fn [this bundle]
    (let [action-bar (.getActionBar this)
          users-view (get-grid-view this [])
          photos-view (get-grid-view this [])
          videos-view (get-grid-view this [])
          audio-view (get-grid-view this [])]
      (.setNavigationMode action-bar android.app.ActionBar/NAVIGATION_MODE_TABS)
      (.setDisplayHomeAsUpEnabled action-bar true)
      (.setTitle action-bar (get-string :favorites))
      (create-tab action-bar (get-string :users) users-view)
      (create-tab action-bar (get-string :photos) photos-view)
      (create-tab action-bar (get-string :videos) videos-view)
      (create-tab action-bar (get-string :audio) audio-view)))
  :on-create-options-menu
  (fn [this menu]
    (create-main-menu this menu))
  :on-options-item-selected
  (fn [this item]
    (do-menu-action this item)))

(defactivity
  net.nightweb.DownloadsPage
  :on-create
  (fn [this bundle]
    (let [action-bar (.getActionBar this)
          all-view (get-grid-view this [])
          photos-view (get-grid-view this [])
          videos-view (get-grid-view this [])
          audio-view (get-grid-view this [])]
      (.setNavigationMode action-bar android.app.ActionBar/NAVIGATION_MODE_TABS)
      (.setDisplayHomeAsUpEnabled action-bar true)
      (.setTitle action-bar (get-string :downloads))
      (create-tab action-bar (get-string :all) all-view)
      (create-tab action-bar (get-string :photos) photos-view)
      (create-tab action-bar (get-string :videos) videos-view)
      (create-tab action-bar (get-string :audio) audio-view)))
  :on-create-options-menu
  (fn [this menu]
    (create-main-menu this menu))
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
    (create-main-menu this menu))
  :on-options-item-selected
  (fn [this item]
    (do-menu-action this item)))
