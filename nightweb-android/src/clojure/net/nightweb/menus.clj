(ns net.nightweb.menus
  (:use [neko.resource :only [get-string get-resource]]))

(defn create-menu-from-resource
  [context menu menu-resource]
  (.inflate (.getMenuInflater context) menu-resource menu))

(defn activate-share-button
  [context]
  (let [intent (android.content.Intent. android.content.Intent/ACTION_SEND)]
    (.setType intent "text/plain")
    (.putExtra intent android.content.Intent/EXTRA_TEXT "nightweb://asdf")
    (.startActivity context intent)))

(defn go-home
  [context]
  (let [intent
        (android.content.Intent.
          context (java.lang.Class/forName "net.nightweb.MainPage"))]
    (.startActivity context intent)))

(defn create-main-menu
  [context menu]
  (let [new-post-item (.add menu (get-string :new_post))
        search-item (.add menu (get-string :search))
        share-item (.add menu (get-string :share))
        if-room android.view.MenuItem/SHOW_AS_ACTION_IF_ROOM]
    (.setIcon new-post-item (get-resource :drawable :android/ic_menu_add))
    (.setIcon search-item (get-resource :drawable :android/ic_menu_search))
    (.setIcon share-item (get-resource :drawable :android/ic_menu_share))
    (.setShowAsAction new-post-item if-room)
    (.setShowAsAction search-item if-room)
    (.setShowAsAction share-item if-room)
    (.setOnMenuItemClickListener
      new-post-item
      (proxy [android.view.MenuItem$OnMenuItemClickListener] []
        (onMenuItemClick [menu-item]
          (.startActivity
            context
            (android.content.Intent.
              context (java.lang.Class/forName "net.nightweb.NewPostPage")))
          true)))
    (.setOnMenuItemClickListener
      search-item
      (proxy [android.view.MenuItem$OnMenuItemClickListener] []
        (onMenuItemClick [menu-item]
          (println "search")
          true)))
    (.setOnMenuItemClickListener
      share-item
      (proxy [android.view.MenuItem$OnMenuItemClickListener] []
        (onMenuItemClick [menu-item]
          (activate-share-button context)
          true)))))

(defn create-new-post-menu
  [context menu]
  (let [send-item (.add menu (get-string :send))
        if-room android.view.MenuItem/SHOW_AS_ACTION_IF_ROOM
        with-text android.view.MenuItem/SHOW_AS_ACTION_WITH_TEXT]
    (.setIcon send-item (get-resource :drawable :android/ic_menu_send))
    (.setShowAsAction send-item (bit-or if-room with-text))
    (.setOnMenuItemClickListener
    send-item
    (proxy [android.view.MenuItem$OnMenuItemClickListener] []
      (onMenuItemClick [menu-item]
        (println "send")
        true)))))
