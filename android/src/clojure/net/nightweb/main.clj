(ns net.nightweb.main
  (:require [neko.application :as app]
            [neko.notify :as notify]
            [neko.resource :as r]
            [net.clandroid.service :as s]
            [nightweb.router :as router]))

(app/defapplication net.nightweb.Application)

(def ^:const service-name "net.nightweb.MainService")
(def ^:const shutdown-receiver-name "ACTION_CLOSE_APP")

(s/defservice
  net.nightweb.MainService
  :def service
  :on-create
  (fn [this]
    (s/start-foreground
      this 1 (notify/notification
               :icon (r/get-resource :drawable :ic_launcher)
               :content-title (r/get-string :shut_down_nightweb)
               :content-text (r/get-string :nightweb_is_running)
               :action [:broadcast shutdown-receiver-name]))
    (s/start-receiver
      this
      shutdown-receiver-name
      (fn [context intent]
        (try
          (.stopSelf service)
          (catch Exception e nil))))
    (router/start-router (.getAbsolutePath (.getFilesDir this)) false))
  :on-destroy
  (fn [this]
    (s/stop-receiver this shutdown-receiver-name)
    (router/stop-router)))
