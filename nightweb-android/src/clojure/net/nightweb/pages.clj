(ns net.nightweb.pages
  (:use [neko.resource :only [get-resource get-string]]
        [neko.activity :only [set-content-view!]]
        [net.nightweb.clandroid.activity :only [defactivity]]
        [net.nightweb.clandroid.service :only [bind-service unbind-service]]
        [net.nightweb.views :only [create-tab
                                   get-grid-view
                                   get-new-post-view]]
        [net.nightweb.menus :only [create-main-menu
                                   create-new-post-menu]]
        [net.nightweb.actions :only [show-page
                                     show-profile]]))

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
                                  {:title (get-string :favorites)}
                                  {:title (get-string :downloads)}])
          users-view (get-grid-view this
                                    [{:title (get-string :tags)}])
          photos-view (get-grid-view this
                                    [{:title (get-string :tags)}])
          videos-view (get-grid-view this
                                    [{:title (get-string :tags)}])
          audio-view (get-grid-view this
                                    [{:title (get-string :tags)}])]
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
  net.nightweb.NewPostPage
  :def new-post-page
  :on-create
  (fn [this bundle]
    (let [action-bar (.getActionBar this)
          new-post-view
          (get-new-post-view this
                             [{:title (get-string :attach_users)}
                              {:title (get-string :attach_photos)}
                              {:title (get-string :attach_videos)}
                              {:title (get-string :attach_audio)}])]
      (.setDisplayShowTitleEnabled action-bar false)
      (.setDisplayHomeAsUpEnabled action-bar true)
      (set-content-view! new-post-page new-post-view)))
  :on-create-options-menu
  (fn [this menu]
    (create-new-post-menu this menu))
  :on-options-item-selected
  (fn [this item]
    (if (= (.getItemId item) (get-resource :id :android/home))
      (show-page this "net.nightweb.MainPage"))))
