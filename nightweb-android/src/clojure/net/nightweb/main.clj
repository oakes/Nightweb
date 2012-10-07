(ns net.nightweb.main
  (:use [neko.activity :only [defactivity set-content-view!]]
        [neko.threading :only [on-ui]]
        [neko.ui :only [make-ui]]
        [neko.application :only [defapplication]]
        [neko.notify :only [notification fire]]
        [neko.resource :only [get-resource]]
        net.nightweb.service)
  (:require nightweb.router))

(defapplication net.nightweb.Application)

(defactivity net.nightweb.MainActivity
  :def a
  :on-create
  (fn [this bundle]
    (on-ui
     (set-content-view! a
      (make-ui [:linear-layout {}
                [:text-view {:text "Hello from Clojure!"}]])))
    (start-service this "net.nightweb.MainService")))

(defservice net.nightweb.MainService
  :def s
  :on-create
  (fn [this]
    (start-foreground
      this 1 (notification
               :icon (get-resource :drawable :ic_launcher)
               :content-title "Nightweb is running"
               :content-text ""
               :action [:activity "net.nightweb.MAINACTIVITY"]))))