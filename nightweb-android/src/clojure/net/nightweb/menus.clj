(ns net.nightweb.menus
  (:use [neko.resource :only [get-string get-resource]]
        [net.nightweb.actions :only [share-url
                                     show-new-post]]))

(defn create-main-menu
  [context menu show-share-button]
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
          (println "typing:" new-text)
          true)
        (onQueryTextSubmit [query]
          (println "submitted:" query)
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
          (show-new-post context {})
          true))))
    ; create share button
  (if show-share-button
    (let [share-item (.add menu (get-string :share))]
      (.setIcon share-item (get-resource :drawable :android/ic_menu_share))
      (.setShowAsAction share-item
                        android.view.MenuItem/SHOW_AS_ACTION_IF_ROOM)
      (.setOnMenuItemClickListener
        share-item
        (proxy [android.view.MenuItem$OnMenuItemClickListener] []
          (onMenuItemClick [menu-item]
            (share-url context)
            true))))))
