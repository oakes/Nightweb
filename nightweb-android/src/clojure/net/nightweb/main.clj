(ns net.nightweb.main
  (:use [neko.application :only [defapplication]]
        [neko.notify :only [notification]]
        [neko.resource :only [get-resource]]
        [net.nightweb.service :only [defservice
                                     bind-service
                                     unbind-service
                                     start-foreground]]
        [net.nightweb.activity :only [defactivity]]
        [net.nightweb.views :only [create-tab create-view]]
        [nightweb.router :only [start-router
                                stop-router
                                start-download-manager]]))

(defapplication net.nightweb.Application)

(defactivity
  net.nightweb.MainActivity
  :def activity
  :on-create
  (fn [this bundle]
    (def conn (bind-service this
                            "net.nightweb.MainService"
                            (fn [service] (.act service :test))))
    (let [action-bar (.getActionBar this)
          home-view (create-view :grid {})
          people-view (create-view :grid {})
          photos-view (create-view :grid {})
          audio-view (create-view :grid {})
          videos-view (create-view :grid {})]
      (.setNavigationMode action-bar android.app.ActionBar/NAVIGATION_MODE_TABS)
      (.setDisplayShowTitleEnabled action-bar false)
      (.setDisplayShowHomeEnabled action-bar false)
      (create-tab action-bar
                  (get-resource :string :home)
                  (create-view :grid [{:title "Profile"}
                                      {:title "Following"}]))
      (create-tab action-bar
                  (get-resource :string :people)
                  (create-view :grid []))
      (create-tab action-bar
                  (get-resource :string :photos)
                  (create-view :grid []))
      (create-tab action-bar
                  (get-resource :string :audio)
                  (create-view :grid []))
      (create-tab action-bar
                  (get-resource :string :videos)
                  (create-view :grid [])))
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
  :menu-resource
  (get-resource :menu :main_activity))

(defservice
  net.nightweb.MainService
  :def service
  :on-create
  (fn [this]
    (start-foreground
      this 1 (notification
               :icon (get-resource :drawable :ic_launcher)
               :content-title "Nightweb is running"
               :content-text "Touch to shut down"
               :action [:broadcast "ACTION_CLOSE_APP"]))
    (def service-receiver (proxy [android.content.BroadcastReceiver] []
                            (onReceive [context intent]
                              (.stopSelf service))))
    (.registerReceiver this
                       service-receiver
                       (android.content.IntentFilter. "ACTION_CLOSE_APP"))
    ;(start-router this)
    ;(def download-manager (start-download-manager))
    )
  :on-destroy
  (fn [this]
    (.unregisterReceiver this service-receiver)
    ;(stop-router)
    )
  :on-action
  (fn [this action]
    (if (= action :test)
      (println action))))
