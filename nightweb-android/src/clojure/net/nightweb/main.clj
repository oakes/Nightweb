(ns net.nightweb.main
  (:use [neko.activity :only [defactivity set-content-view!]]
        [neko.threading :only [on-ui]]
        [neko.ui :only [make-ui]]
        [neko.application :only [defapplication]]))

(defapplication net.nightweb.Application)

(defactivity net.nightweb.MainActivity
  :def a
  :on-create
  (fn [this bundle]
    (on-ui
     (set-content-view! a
      (make-ui [:linear-layout {}
                [:text-view {:text "Hello from Clojure!"}]])))))