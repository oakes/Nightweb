(ns net.nightweb.clandroid.service
  (:use [neko.-utils :only [simple-name
                            unicaseize
                            keyword->camelcase
                            capitalize]]))

(defn bind-service
  [context class-name connected]
  (let [intent (android.content.Intent.)
        connection (proxy [android.content.ServiceConnection] []
                     (onServiceConnected [component-name binder]
                                         (connected binder))
                     (onServiceDisconnected [component-name] ()))]
    (.setClassName intent context class-name)
    (.startService context intent)
    (.bindService context intent connection 0)
    connection))

(defn unbind-service
  [context connection]
  (.unbindService context connection))

(defn start-service
  ([context service-name] (start-service context service-name (fn [binder])))
  ([context service-name on-connected]
   (let [service (bind-service context service-name on-connected)]
     (swap! (.state context) assoc :service service))))

(defn stop-service
  [context]
  (if-let [service (get @(.state context) :service)]
    (unbind-service context service)))

(defn start-receiver
  [context receiver-name func]
  (let [receiver (proxy [android.content.BroadcastReceiver] []
                   (onReceive [context intent]
                     (func context intent)))]
    (.registerReceiver context
                       receiver
                       (android.content.IntentFilter. receiver-name))
    (swap! (.state context) assoc receiver-name receiver)))

(defn stop-receiver
  [context receiver-name]
  (if-let [receiver (get @(.state context) receiver-name)]
    (.unregisterReceiver context receiver)))

(defn send-broadcast
  [context params action-name]
  (let [intent (android.content.Intent.)]
    (.putExtra intent "params" params)
    (.setAction intent action-name)
    (.sendBroadcast context intent)))

(defn start-foreground
  [service id notification]
  (.startForeground service id notification))

(do
  (gen-class
    :name "CustomBinder"
    :extends android.os.Binder
    :state "state"
    :init "init"
    :constructors {[android.app.Service] []}
    :prefix "binder-")
  (defn binder-init
    [service]
    [[] service])
  (defn create-binder
    [service]
    (CustomBinder. service)))

(defmacro defservice
  [name & {:keys [extends prefix on-start-command def] :as options}]
  (let [options (or options {})
        sname (simple-name name)
        prefix (or prefix (str sname "-"))
        def (or def (symbol (unicaseize sname)))]
    `(do
       (gen-class
        :name ~name
        :main false
        :prefix ~prefix
        :init "init"
        :state "state"
        :extends ~(or extends android.app.Service)
        :exposes-methods {~'onCreate ~'superOnCreate
                          ~'onDestroy ~'superOnDestroy})
       (defn ~(symbol (str prefix "init"))
         [] [[] (atom {})])
       (defn ~(symbol (str prefix "onBind"))
         [~(vary-meta 'this assoc :tag name),
          ^android.content.Intent ~'intent]
         (def ~(vary-meta def assoc :tag name) ~'this)
         (~create-binder ~'this))
       ~(when on-start-command
          `(defn ~(symbol (str prefix "onStartCommand"))
             [~(vary-meta 'this assoc :tag name),
              ^android.content.Intent ~'intent,
              ^int ~'flags,
              ^int ~'startId]
             (def ~(vary-meta def assoc :tag name) ~'this)
             (~on-start-command ~'this ~'intent ~'flags ~'startId)
             android.app.Service/START_STICKY))
       ~@(map #(let [func (options %)
                     event-name (keyword->camelcase %)]
                 (when func
                   `(defn ~(symbol (str prefix event-name))
                      [~(vary-meta 'this assoc :tag name)]
                      (~(symbol (str ".super" (capitalize event-name))) ~'this)
                      (~func ~'this))))
              [:on-create :on-destroy]))))
