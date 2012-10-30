(ns net.nightweb.service
  (:import (android.app Service Notification)
           (android.content Context Intent ServiceConnection)
           (android.os Binder Bundle))
  (:use neko.-utils))

(defn bind-service
  [context class-name connected]
  (let [intent (Intent.)
        connection (proxy [ServiceConnection] []
                     (onServiceConnected [name binder]
                                         (connected (.state binder)))
                     (onServiceDisconnected [name] ()))]
    (.setClassName intent context class-name)
    (.startService context intent)
    (.bindService context intent connection 0)
    connection))

(defn unbind-service
  [context connection]
  (.unbindService context connection))

(defn send-to-service
  [context class-name message]
  (let [intent (Intent.)]
    (.setClassName intent context class-name)
    (let [bundle (Bundle.)]
      (.putString bundle "message" message)
      (.putExtras intent bundle))
    (.startService context intent)))

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
        :methods ~[["act" [clojure.lang.Keyword] 'void]]
        :extends ~(or extends Service)
        :exposes-methods {~'onCreate ~'superOnCreate
                          ~'onDestroy ~'superOnDestroy})
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