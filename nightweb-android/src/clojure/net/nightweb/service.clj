(ns net.nightweb.service
  (:import (android.app Service Notification)
           (android.content Context Intent))
  (:use neko.-utils))

(defn start-service
  [context class-name]
  (let [intent (Intent.)]
    (.setClassName intent context class-name)
    (.startService context intent)))

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
        :extends ~(or extends Service)
        :exposes-methods {~'onCreate ~'superOnCreate
                          ~'onStartCommand ~'superOnStartCommand
                          ~'onDestroy ~'superOnDestroy})
       ~(when on-start-command
          `(defn ~(symbol (str prefix "onStartCommand"))
             [~(vary-meta 'this assoc :tag name),
              ^android.content.Intent ~'intent,
              ~'flags,
              ~'startId]
             (.superOnStartCommand ~'this ~'intent ~'flags ~'startId)
             (def ~(vary-meta def assoc :tag name) ~'this)
             (~on-start-command ~'this ~'intent ~'flags ~'startId)))
       ~@(map #(let [func (options %)
                     event-name (keyword->camelcase %)]
                 (when func
                   `(defn ~(symbol (str prefix event-name))
                      [~(vary-meta 'this assoc :tag name)]
                      (~(symbol (str ".super" (capitalize event-name))) ~'this)
                      (~func ~'this))))
              [:on-create :on-destroy]))))