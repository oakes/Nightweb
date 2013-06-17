(ns nightweb-desktop.pages
  (:use [hiccup.core :only [html]]
        [nightweb.constants :only [my-hash-bytes]]
        [nightweb-desktop.views :only [get-action-bar-view
                                       get-tab-view
                                       get-user-view
                                       get-post-view
                                       get-category-view]]
        [nightweb-desktop.dialogs :only [get-search-dialog
                                         get-new-post-dialog
                                         get-edit-post-dialog
                                         get-link-dialog
                                         get-export-dialog
                                         get-import-dialog
                                         get-switch-user-dialog]]
        [nightweb-desktop.utils :only [get-string]]))

(defmacro get-page
  [params & body]
  `(html [:head
          [:title (get-string :app_name)]
          [:link {:rel "stylesheet" :href "foundation.min.css"}]
          [:link {:rel "stylesheet" :href "nw.css"}]
          [:link {:rel "stylesheet" :href "fonts/general_foundicons.css"}]]
         [:body {:class "dark-gradient"}
          ~@body
          (get-search-dialog ~params)
          (get-new-post-dialog ~params)
          (get-edit-post-dialog ~params)
          (get-link-dialog ~params)
          (get-export-dialog ~params)
          (get-import-dialog ~params)
          (get-switch-user-dialog ~params)
          [:div {:id "lightbox"}]
          [:script {:src "zepto.js"}]
          [:script {:src "foundation.min.js"}]
          [:script {:src "custom.modernizr.js"}]
          [:script {:src "foundation/foundation.topbar.js"}]
          [:script {:src "spin.min.js"}]
          [:script {:src "nw.js"}]]))

(defn get-view
  [params]
  (case (:type params)
    :user (if (:userhash params)
            (get-user-view params)
            (get-category-view params))
    :post (if (:time params)
            (get-post-view params)
            (get-category-view params))
    :tag (get-category-view params)
    nil (get-user-view {:type :user :userhash @my-hash-bytes})
    [:h2 "There doesn't seem to be anything here."]))

(defn get-main-page
  [params]
  (get-page
    params
    (get-action-bar-view params :is-main? true :show-tabs? true)
    (get-view params)))

(defn get-category-page
  [params]
  (get-page
    params
    (get-action-bar-view params :show-home-button? true :show-tabs? true)
    (get-category-view params)))

(defn get-basic-page
  [params]
  (get-page
    params
    (get-action-bar-view params :show-home-button? true)
    (get-view params)))
