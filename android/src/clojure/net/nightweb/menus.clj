(ns net.nightweb.menus
  (:require [neko.resource :as r]
            [neko.ui.menu :as ui.menu]
            [net.nightweb.actions :as actions]
            [net.nightweb.dialogs :as d])
  (:import [android.view Menu]))

(defn create-main-menu
  [context ^Menu menu show-share-button? show-switch-button?]
  (->> [[:item {:title (r/get-string :search)
                :icon (r/get-resource :drawable :action_search)
                :show-as-action [:if-room :collapse-action-view]
                :action-view
                [:search-view
                 {:on-query-text-change (fn [query menu-item] false)
                  :on-query-text-submit
                  (fn [query menu-item]
                    (actions/show-categories
                      context {:title (str (r/get-string :search) ": " query)
                               :query query
                               :type :search})
                    true)}]}]
        [:item {:title (r/get-string :new_post)
                :icon (r/get-resource :drawable :content_new)
                :show-as-action :if-room
                :on-click (fn [_] (d/show-new-post-dialog context {}))}]
        (when show-share-button?
          [:item {:title (r/get-string :share)
                  :icon (r/get-resource :drawable :social_share)
                  :show-as-action :if-room
                  :on-click (fn [_] (actions/share-url context))}])
        (when show-switch-button?
          [:item {:title (r/get-string :switch_user)
                  :icon (r/get-resource :drawable :social_group)
                  :show-as-action :if-room
                  :on-click (fn [_] (d/show-switch-user-dialog context {}))}])]
       (remove nil?)
       (ui.menu/make-menu menu)))
