(ns net.nightweb.main
  (:use [neko.activity :only [set-content-view!]]
        [neko.threading :only [on-ui]]
        [neko.ui :only [make-ui]]
        [neko.application :only [defapplication]]
        [neko.notify :only [notification fire]]
        [neko.resource :only [get-resource]]
        net.nightweb.service
        net.nightweb.activity)
  (:require nightweb.router))

(defapplication net.nightweb.Application)

(defn create-tab
  []
  (proxy [android.app.ActionBar$TabListener] []
    (onTabSelected [tab ft])
    (onTabUnselected [tab ft])
    (onTabReselected [tab ft])))

(defactivity net.nightweb.MainActivity
  :def a
  :on-create
  (fn [this bundle]
    (on-ui
     (set-content-view! a
      (make-ui [:linear-layout {}
                [:text-view {:text "Hello from Clojure!"}]])))
    (def conn (bind-service this
                            "net.nightweb.MainService"
                            (fn [service] (.act service "Action!"))))
    (let [action-bar (.getActionBar this)
          home-tab (.newTab action-bar)
          people-tab (.newTab action-bar)
          photos-tab (.newTab action-bar)
          audio-tab (.newTab action-bar)
          videos-tab (.newTab action-bar)]
      (.setNavigationMode action-bar android.app.ActionBar/NAVIGATION_MODE_TABS)
      (.setDisplayShowTitleEnabled action-bar false)
      (.setText home-tab (get-resource :string :home))
      (.setText people-tab (get-resource :string :people))
      (.setText photos-tab (get-resource :string :photos))
      (.setText audio-tab (get-resource :string :audio))
      (.setText videos-tab (get-resource :string :videos))
      (.setTabListener home-tab (create-tab))
      (.setTabListener people-tab (create-tab))
      (.setTabListener photos-tab (create-tab))
      (.setTabListener audio-tab (create-tab))
      (.setTabListener videos-tab (create-tab))
      (.addTab action-bar home-tab)
      (.addTab action-bar people-tab)
      (.addTab action-bar photos-tab)
      (.addTab action-bar audio-tab)
      (.addTab action-bar videos-tab)))
  :on-destroy
  (fn [this]
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
               :content-text ""
               :action [:activity "net.nightweb.MAINACTIVITY"])))
  :on-action
  (fn [this action]
    (println action)))