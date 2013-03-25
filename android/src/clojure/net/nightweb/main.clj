(ns net.nightweb.main
  (:use [neko.application :only [defapplication]]
        [neko.notify :only [notification]]
        [neko.resource :only [get-resource get-string]]
        [net.clandroid.service :only [defservice
                                      start-foreground
                                      start-receiver
                                      stop-receiver]]
        [nightweb.router :only [start-router
                                stop-router]]))

(defapplication net.nightweb.Application)

(def service-name "net.nightweb.MainService")
(def shutdown-receiver-name "ACTION_CLOSE_APP")

(defservice
  net.nightweb.MainService
  :def service
  :on-create
  (fn [this]
    (start-foreground
      this 1 (notification
               :icon (get-resource :drawable :ic_launcher)
               :content-title (get-string :shutdown_nightweb)
               :content-text (get-string :nightweb_is_running)
               :action [:broadcast shutdown-receiver-name]))
    (start-receiver this
                    shutdown-receiver-name
                    (fn [context intent] (.stopSelf service)))
    (start-router (.getAbsolutePath (.getFilesDir this))))
  :on-destroy
  (fn [this]
    (stop-receiver this shutdown-receiver-name)
    (stop-router)))
