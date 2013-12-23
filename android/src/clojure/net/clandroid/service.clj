(ns net.clandroid.service
  (:require [neko.-utils :as utils])
  (:import [android.app Activity]))

(defn start-service
  [^Activity context class-name connected]
  (let [intent (android.content.Intent.)
        connection (proxy [android.content.ServiceConnection] []
                     (onServiceConnected [component-name binder]
                                         (connected binder))
                     (onServiceDisconnected [component-name] ()))]
    (.setClassName intent context class-name)
    (.startService context intent)
    (.bindService context intent connection 0)
    connection))

(defn stop-service
  [^Activity context connection]
  (.unbindService context connection))

(defn start-receiver
  [^Activity context receiver-name func]
  (let [receiver (proxy [android.content.BroadcastReceiver] []
                   (onReceive [context intent]
                     (func context intent)))]
    (.registerReceiver context
                       receiver
                       (android.content.IntentFilter. receiver-name))
    receiver))

(defn start-local-receiver
  [^Activity context receiver-name func]
  (-> (android.support.v4.content.LocalBroadcastManager/getInstance context)
      (start-receiver receiver-name func)))

(defn stop-receiver
  [^Activity context receiver]
  (.unregisterReceiver context receiver))

(defn stop-local-receiver
  [^Activity context receiver-name]
  (-> (android.support.v4.content.LocalBroadcastManager/getInstance context)
      (stop-receiver receiver-name)))

(defn send-broadcast
  [^Activity context params action-name]
  (let [intent (android.content.Intent.)]
    (.putExtra intent "params" params)
    (.setAction intent action-name)
    (.sendBroadcast context intent)))

(defn send-local-broadcast
  [^Activity context params action-name]
  (-> (android.support.v4.content.LocalBroadcastManager/getInstance context)
      (send-broadcast params action-name)))

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
  [name & {:keys [extends prefix on-start-command def state] :as options}]
  (let [options (or options {})
        sname (utils/simple-name name)
        prefix (or prefix (str sname "-"))
        def (or def (symbol (utils/unicaseize sname)))]
    `(do
       (gen-class
        :name ~name
        :main false
        :prefix ~prefix
        ~@(when state
            '(:init "init" :state "state"))
        :extends ~(or extends android.app.Service)
        :exposes-methods {~'onCreate ~'superOnCreate
                          ~'onDestroy ~'superOnDestroy})
       ~(when state
          `(defn ~(symbol (str prefix "init"))
             [] [[] ~state]))
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
                     event-name (utils/keyword->camelcase %)]
                 (when func
                   `(defn ~(symbol (str prefix event-name))
                      [~(vary-meta 'this assoc :tag name)]
                      (~(symbol (str ".super" (utils/capitalize event-name)))
                          ~'this)
                      (~func ~'this))))
              [:on-create :on-destroy]))))
