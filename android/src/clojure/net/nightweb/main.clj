(ns net.nightweb.main
  (:require [neko.notify :as notify]
            [neko.resource :as r]
            [neko.ui.mapping :as mapping]
            [net.clandroid.service :as service]
            [net.nightweb.utils :as utils]
            [nightweb.router :as router]))

(mapping/defelement :scroll-view
                    :classname android.widget.ScrollView
                    :inherits :view)
(mapping/defelement :frame-layout
                    :classname android.widget.FrameLayout
                    :inherits :view)
(mapping/defelement :relative-layout
                    :classname android.widget.RelativeLayout
                    :inherits :view)
(mapping/defelement :image-view
                    :classname android.widget.ImageView
                    :inherits :view)
(mapping/defelement :view-pager
                    :classname android.support.v4.view.ViewPager
                    :inherits :view)

(def ^:const service-name "net.nightweb.MainService")
(def ^:const shutdown-receiver-name "ACTION_CLOSE_APP")

(service/defservice
  net.nightweb.MainService
  :def service
  :state (atom {})
  :on-create
  (fn [this]
    (->> (notify/notification
           :icon (r/get-resource :drawable :ic_launcher)
           :content-title (r/get-string :shut_down_nightweb)
           :content-text (r/get-string :nightweb_is_running)
           :action [:broadcast shutdown-receiver-name])
         (service/start-foreground! this 1))
    (->> (fn [context intent]
           (try
             (.stopSelf service)
             (catch Exception e nil)))
         (service/start-receiver! this shutdown-receiver-name)
         (utils/set-state! this shutdown-receiver-name))
    (-> this .getFilesDir .getAbsolutePath router/start-router!))
  :on-destroy
  (fn [this]
    (when-let [receiver (utils/get-state this shutdown-receiver-name)]
      (service/stop-receiver! this receiver))
    (router/stop-router!)))
