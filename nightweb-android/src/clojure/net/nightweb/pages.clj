(ns net.nightweb.pages
  (:use [neko.resource :only [get-resource get-string]]
        [neko.activity :only [set-content-view!]]
        [net.nightweb.clandroid.activity :only [defactivity]]
        [net.nightweb.clandroid.service :only [bind-service unbind-service]]
        [net.nightweb.views :only [create-tab
                                   get-grid-view
                                   get-new-post-view]]
        [net.nightweb.menus :only [create-main-menu
                                   create-new-post-menu
                                   go-home]]))

(defactivity
  net.nightweb.MainPage
  :on-create
  (fn [this bundle]
    (def conn (bind-service this
                            "net.nightweb.MainService"
                            (fn [service] (.act service :test))))
    (let [action-bar (.getActionBar this)]
      (.setNavigationMode action-bar android.app.ActionBar/NAVIGATION_MODE_TABS)
      (.setDisplayShowTitleEnabled action-bar false)
      (.setDisplayShowHomeEnabled action-bar false)
      (create-tab action-bar
                  (get-string :home)
                  (get-grid-view this
                                 [{:title (get-string :profile)}
                                  {:title (get-string :favorites)}
                                  {:title (get-string :downloads)}]))
      (create-tab action-bar
                  (get-string :users)
                  (get-grid-view this
                                 [{:title (get-string :browse_tags)}]))
      (create-tab action-bar
                  (get-string :photos)
                  (get-grid-view this
                                 [{:title (get-string :browse_tags)}]))
      (create-tab action-bar
                  (get-string :videos)
                  (get-grid-view this
                                 [{:title (get-string :browse_tags)}]))
      (create-tab action-bar
                  (get-string :audio)
                  (get-grid-view this
                                 [{:title (get-string :browse_tags)}])))
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
    (let [action-bar (.getActionBar this)]
      (.setDisplayShowTitleEnabled action-bar false)
      (.setDisplayHomeAsUpEnabled action-bar true))
    (set-content-view! new-post-page (get-new-post-view this [])))
  :on-create-options-menu
  (fn [this menu]
    (create-new-post-menu this menu))
  :on-options-item-selected
  (fn [this item]
    (if (= (.getItemId item) (get-resource :id :android/home))
      (go-home this))))
