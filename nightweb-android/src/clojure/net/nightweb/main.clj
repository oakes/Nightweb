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
                            (fn [service] (.act service :test))))
    (let [action-bar (.getActionBar this)]
      (.setNavigationMode action-bar android.app.ActionBar/NAVIGATION_MODE_TABS)
      (.setDisplayShowTitleEnabled action-bar false)
      (.setDisplayShowHomeEnabled action-bar false)
      (create-tab action-bar (get-resource :string :home))
      (create-tab action-bar (get-resource :string :people))
      (create-tab action-bar (get-resource :string :photos))
      (create-tab action-bar (get-resource :string :audio))
      (create-tab action-bar (get-resource :string :videos))))
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
               :action [:activity "net.nightweb.MAINACTIVITY"]))
    (future (start-router this)))
  :on-action
  (fn [this action]
    (if (= action :test)
      (println action))))