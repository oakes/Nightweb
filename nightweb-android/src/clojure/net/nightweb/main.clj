(ns net.nightweb.main
  (:use [neko.activity :only [set-content-view!]]
        [neko.threading :only [on-ui]]
        [neko.application :only [defapplication]]
        [neko.notify :only [notification fire]]
        [neko.resource :only [get-resource]]
        net.nightweb.service
        net.nightweb.activity
        net.nightweb.views
        nightweb.router))

(defapplication net.nightweb.Application)

(defactivity net.nightweb.MainActivity
  :def a
  :on-create
  (fn [this bundle]
    (def conn (bind-service this
                            "net.nightweb.MainService"
                            (fn [service])))
    (let [action-bar (.getActionBar this)]
      (.setNavigationMode action-bar android.app.ActionBar/NAVIGATION_MODE_TABS)
      (.setDisplayShowTitleEnabled action-bar false)
      (.setDisplayShowHomeEnabled action-bar false)
      (create-tab action-bar (get-resource :string :home) (get-profile-view))
      (create-tab action-bar (get-resource :string :people) (get-grid-view))
      (create-tab action-bar (get-resource :string :photos) (get-grid-view))
      (create-tab action-bar (get-resource :string :audio) (get-grid-view))
      (create-tab action-bar (get-resource :string :videos) (get-grid-view)))
    (def receiver (proxy [android.content.BroadcastReceiver] []
                    (onReceive [context intent]
                               (send-to-service context "net.nightweb.MainService" "stop")
                               (.finish context))))
    (.registerReceiver this
                       receiver
                       (android.content.IntentFilter. "ACTION_CLOSE_APP")))
  :on-destroy
  (fn [this]
    (.unregisterReceiver this receiver)
    (unbind-service this conn))
  :menu-resource
  (get-resource :menu :main_activity))

(defservice net.nightweb.MainService
  :def s
  :on-create
  (fn [this]
    (start-foreground
      this 1 (notification
               :icon (get-resource :drawable :ic_launcher)
               :content-title "Nightweb is running"
               :content-text "Touch to shut down"
               :action [:broadcast "ACTION_CLOSE_APP"]))
    (start-router this)
    (def download-manager (start-download-manager)))
  :on-destroy
  (fn [this]
    (stop-router))
  :on-start-command
  (fn [this intent flags start-id]
    (if intent
      (if-let [bundle (.getExtras intent)]
        (case (.getString bundle "message")
          "stop" (.stopSelf this))))))