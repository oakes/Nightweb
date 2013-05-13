(ns net.nightweb.menus
  (:use [neko.resource :only [get-string get-resource]]
        [net.nightweb.actions :only [share-url]]
        [net.nightweb.utils :only [show-categories]]
        [net.nightweb.dialogs :only [show-new-post-dialog]]))

(defn create-main-menu
  [context menu show-share-button?]
  ; create search button
  (let [search-item (.add menu (get-string :search))
        search-view (android.widget.SearchView. context)]
    (.setIcon search-item (get-resource :drawable :android/ic_menu_search))
    (.setShowAsAction
      search-item
      (bit-or android.view.MenuItem/SHOW_AS_ACTION_IF_ROOM
              android.view.MenuItem/SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW))
    (.setOnQueryTextListener
      search-view
      (proxy [android.widget.SearchView$OnQueryTextListener] []
        (onQueryTextChange [new-text]
          false)
        (onQueryTextSubmit [query]
          (show-categories context {:title (str (get-string :search) ": " query)
                                    :query query
                                    :type :search})
          true)))
    (.setActionView search-item search-view))
  ; create new post button
  (let [new-post-item (.add menu (get-string :new_post))]
    (.setIcon new-post-item (get-resource :drawable :android/ic_menu_add))
    (.setShowAsAction new-post-item
                      android.view.MenuItem/SHOW_AS_ACTION_IF_ROOM)
    (.setOnMenuItemClickListener
      new-post-item
      (proxy [android.view.MenuItem$OnMenuItemClickListener] []
        (onMenuItemClick [menu-item]
          (show-new-post-dialog context {})
          true))))
  ; create share button
  (when show-share-button?
    (let [share-item (.add menu (get-string :share))]
      (.setIcon share-item (get-resource :drawable :android/ic_menu_share))
      (.setShowAsAction share-item
                        android.view.MenuItem/SHOW_AS_ACTION_IF_ROOM)
      (.setOnMenuItemClickListener
        share-item
        (proxy [android.view.MenuItem$OnMenuItemClickListener] []
          (onMenuItemClick [menu-item]
            (share-url context)
            true)))))
  (let [switch-user-item (.add menu (get-string :switch_user))]
    (.setIcon switch-user-item (get-resource :drawable :profile_small))
    (.setShowAsAction switch-user-item
                      android.view.MenuItem/SHOW_AS_ACTION_IF_ROOM)
    (.setOnMenuItemClickListener
      switch-user-item
      (proxy [android.view.MenuItem$OnMenuItemClickListener] []
        (onMenuItemClick [menu-item]
          true)))))
