(ns net.nightweb.main
  (:use [neko.application :only [defapplication]]
        [neko.notify :only [notification]]
        [neko.resource :only [get-resource]]
        [net.nightweb.clandroid.service :only [defservice
                                               start-foreground
                                               start-receiver
                                               stop-receiver]]
        [nightweb.router :only [start-router stop-router]]
        [nightweb.torrent :only [add-hash]]
        [nightweb.db :only [init-db]]))

(defapplication net.nightweb.Application)

(def service-name "net.nightweb.MainService")
(def shutdown-receiver-name "ACTION_CLOSE_APP")
(def download-receiver-name "ACTION_START_TORRENT")

(defn download-receiver-func
  [context intent]
  (if-let [params (.getSerializableExtra intent "params")]
    (if-let [download-hash (get params :hash)]
      (add-hash download-hash))))

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
               :action [:broadcast shutdown-receiver-name]))
    (start-receiver this
                    shutdown-receiver-name
                    (fn [context intent] (.stopSelf service)))
    (start-receiver this download-receiver-name download-receiver-func)
    (let [dir (.getAbsolutePath (.getFilesDir this))]
      (init-db dir)
      (start-router dir)))
  :on-destroy
  (fn [this]
    (stop-receiver this shutdown-receiver-name)
    (stop-receiver this download-receiver-name)
    (stop-router)))
