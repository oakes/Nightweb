(ns net.clandroid.activity
  (:use [neko.-utils :only [simple-name
                            unicaseize
                            keyword->camelcase
                            capitalize]]))

(defn set-state
  [context content-key content-val]
  (swap! (.state context) assoc content-key content-val))

(defn get-state
  [context content-key]
  (get @(.state context) content-key))

(defmacro defactivity
  "Creates an activity with the given full package-qualified name.
  Optional arguments should be provided in a key-value fashion.

  Available optional arguments:

  :extends, :prefix - same as for `gen-class`.

  :def - symbol to bind the Activity object to in the onCreate
  method. Relevant only if :create is used.

  :on-create - takes a two-argument function. Generates a handler for
  activity's `onCreate` event which automatically calls the
  superOnCreate method and creates a var with the name denoted by
  `:def` (or activity's lower-cased name by default) to store the
  activity object. Then calls the provided function onto the
  Application object.

  :on-start, :on-restart, :on-resume, :on-pause, :on-stop, :on-destroy
  - same as :on-create but require a one-argument function."
  [name & {:keys [extends
                  prefix
                  on-create
                  on-create-options-menu
                  on-options-item-selected
                  on-activity-result
                  on-new-intent
                  def]
           :as options}]
  (let [options (or options {}) ;; Handle no-options case
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
        :extends ~(or extends android.app.Activity)
        :exposes-methods {~'onCreate ~'superOnCreate
                          ~'onStart ~'superOnStart
                          ~'onRestart ~'superOnRestart
                          ~'onResume ~'superOnResume
                          ~'onPause ~'superOnPause
                          ~'onStop ~'superOnStop
                          ~'onDestroy ~'superOnDestroy
                          ~'onCreateOptionsMenu ~'superOnCreateOptionsMenu
                          ~'onOptionsItemSelected ~'superOnOptionsItemSelected
                          ~'onActivityResult ~'superOnActivityResult
                          ~'onNewIntent ~'superOnNewIntent})
       (defn ~(symbol (str prefix "init"))
         [] [[] (atom {})])
       ~(when on-create
          `(defn ~(symbol (str prefix "onCreate"))
             [~(vary-meta 'this assoc :tag name),
              ^android.os.Bundle ~'savedInstanceState]
             (.superOnCreate ~'this ~'savedInstanceState)
             (def ~(vary-meta def assoc :tag name) ~'this)
             (~on-create ~'this ~'savedInstanceState)))
       ~(when on-create-options-menu
          `(defn ~(symbol (str prefix "onCreateOptionsMenu"))
             [~(vary-meta 'this assoc :tag name),
              ^android.view.Menu ~'menu]
             (.superOnCreateOptionsMenu ~'this ~'menu)
             (def ~(vary-meta def assoc :tag name) ~'this)
             (~on-create-options-menu ~'this ~'menu)
             true))
       ~(when on-options-item-selected
          `(defn ~(symbol (str prefix "onOptionsItemSelected"))
             [~(vary-meta 'this assoc :tag name),
              ^android.view.MenuItem ~'item]
             (def ~(vary-meta def assoc :tag name) ~'this)
             (~on-options-item-selected ~'this ~'item)
             true))
       ~(when on-activity-result
          `(defn ~(symbol (str prefix "onActivityResult"))
             [~(vary-meta 'this assoc :tag name),
              ^int ~'requestCode,
              ^int ~'resultCode,
              ^android.content.Intent ~'intent]
             (.superOnActivityResult ~'this ~'requestCode ~'resultCode ~'intent)
             (def ~(vary-meta def assoc :tag name) ~'this)
             (~on-activity-result ~'this ~'requestCode ~'resultCode ~'intent)))
       ~(when on-new-intent
          `(defn ~(symbol (str prefix "onNewIntent"))
             [~(vary-meta 'this assoc :tag name),
              ^android.content.Intent ~'intent]
             (.superOnNewIntent ~'this ~'intent)
             (def ~(vary-meta def assoc :tag name) ~'this)
             (~on-new-intent ~'this ~'intent)))
       ~@(map #(let [func (options %)
                     event-name (keyword->camelcase %)]
                 (when func
                   `(defn ~(symbol (str prefix event-name))
                      [~(vary-meta 'this assoc :tag name)]
                      (~(symbol (str ".super" (capitalize event-name))) ~'this)
                      (~func ~'this))))
              [:on-start :on-restart :on-resume
               :on-pause :on-stop :on-destroy]))))
