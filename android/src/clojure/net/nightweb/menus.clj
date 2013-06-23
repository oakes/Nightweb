(ns net.nightweb.menus
  (:require [neko.resource :as r]
            [net.nightweb.actions :as actions]
            [net.nightweb.dialogs :as dialogs])
  (:import [android.view MenuItem]
           [android.widget SearchView]))

(defn create-main-menu
  [context menu show-share-button? show-switch-button?]
  ; create search button
  (let [search-item (.add menu (r/get-string :search))
        search-view (SearchView. context)]
    (.setIcon search-item (r/get-resource :drawable :android/ic_menu_search))
    (.setShowAsAction
      search-item
      (bit-or MenuItem/SHOW_AS_ACTION_IF_ROOM
              MenuItem/SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW))
    (.setOnQueryTextListener
      search-view
      (proxy [android.widget.SearchView$OnQueryTextListener] []
        (onQueryTextChange [new-text]
          false)
        (onQueryTextSubmit [query]
          (actions/show-categories
            context
            {:title (str (r/get-string :search) ": " query)
             :query query
             :type :search})
          true)))
    (.setActionView search-item search-view))
  ; create new post button
  (let [new-post-item (.add menu (r/get-string :new_post))]
    (.setIcon new-post-item (r/get-resource :drawable :android/ic_menu_add))
    (.setShowAsAction new-post-item MenuItem/SHOW_AS_ACTION_IF_ROOM)
    (.setOnMenuItemClickListener
      new-post-item
      (proxy [android.view.MenuItem$OnMenuItemClickListener] []
        (onMenuItemClick [menu-item]
          (dialogs/show-new-post-dialog context {})
          true))))
  ; create share button
  (when show-share-button?
    (let [share-item (.add menu (r/get-string :share))]
      (.setIcon share-item (r/get-resource :drawable :android/ic_menu_share))
      (.setShowAsAction share-item MenuItem/SHOW_AS_ACTION_IF_ROOM)
      (.setOnMenuItemClickListener
        share-item
        (proxy [android.view.MenuItem$OnMenuItemClickListener] []
          (onMenuItemClick [menu-item]
            (actions/share-url context)
            true)))))
  ; create switch user button
  (when show-switch-button?
    (let [switch-user-item (.add menu (r/get-string :switch_user))]
      (.setIcon switch-user-item (r/get-resource :drawable :profile_small))
      (.setShowAsAction switch-user-item MenuItem/SHOW_AS_ACTION_IF_ROOM)
      (.setOnMenuItemClickListener
        switch-user-item
        (proxy [android.view.MenuItem$OnMenuItemClickListener] []
          (onMenuItemClick [menu-item]
            (dialogs/show-switch-user-dialog context {})
            true))))))
