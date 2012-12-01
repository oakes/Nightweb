(ns net.nightweb.menus
  (:use [neko.resource :only [get-resource]]))

(defn create-menu-from-resource
  [context menu menu-resource]
  (.inflate (.getMenuInflater context) menu-resource menu))

(defn activate-share-button
  [context menu share-resource]
  (let [intent (android.content.Intent. android.content.Intent/ACTION_SEND)
        action-provider (.getActionProvider (.findItem menu share-resource))]
    (.setType intent "text/plain")
    (.putExtra intent android.content.Intent/EXTRA_TEXT "nightweb://asdf")
    (.setShareIntent action-provider intent)))

(defn go-home
  [context]
  (let [intent
        (android.content.Intent.
          context (java.lang.Class/forName "net.nightweb.MainPage"))]
    (.startActivity context intent)))

(defn create-main-menu
  [context menu]
  (create-menu-from-resource context menu (get-resource :menu :main))
  (.setOnMenuItemClickListener
    (.findItem menu (get-resource :id :new_post))
    (proxy [android.view.MenuItem$OnMenuItemClickListener] []
      (onMenuItemClick [menu-item]
        (.startActivity context
                        (android.content.Intent.
                          context
                          (java.lang.Class/forName "net.nightweb.NewPostPage")))
        true)))
  (.setOnMenuItemClickListener
    (.findItem menu (get-resource :id :search))
    (proxy [android.view.MenuItem$OnMenuItemClickListener] []
      (onMenuItemClick [menu-item]
        (println "search")
        true)))
  (activate-share-button context menu (get-resource :id :share)))

(defn create-new-post-menu
  [context menu]
  (create-menu-from-resource context menu (get-resource :menu :new_post))
  (.setOnMenuItemClickListener
    (.findItem menu (get-resource :id :send))
    (proxy [android.view.MenuItem$OnMenuItemClickListener] []
      (onMenuItemClick [menu-item]
        (println "send")
        true))))
