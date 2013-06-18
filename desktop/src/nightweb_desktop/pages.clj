(ns nightweb-desktop.pages
  (:require [hiccup.core :as hiccup]
            [nightweb.constants :as c]
            [nightweb-desktop.views :as views]
            [nightweb-desktop.dialogs :as dialogs]
            [nightweb-desktop.utils :as utils]))

(defmacro get-page
  [params & body]
  `(hiccup/html [:head
                 [:title (utils/get-string :app_name)]
                 [:link {:rel "stylesheet" :href "foundation.min.css"}]
                 [:link {:rel "stylesheet" :href "nw.css"}]
                 [:link {:rel "stylesheet" :href "fonts/general_foundicons.css"}]]
                [:body {:class "dark-gradient"}
                 ~@body
                 (dialogs/get-search-dialog ~params)
                 (dialogs/get-new-post-dialog ~params)
                 (dialogs/get-edit-post-dialog ~params)
                 (dialogs/get-link-dialog ~params)
                 (dialogs/get-export-dialog ~params)
                 (dialogs/get-import-dialog ~params)
                 (dialogs/get-switch-user-dialog ~params)
                 [:div {:id "lightbox"}]
                 [:script {:src "zepto.js"}]
                 [:script {:src "foundation.min.js"}]
                 [:script {:src "custom.modernizr.js"}]
                 [:script {:src "spin.min.js"}]
                 [:script {:src "nw.js"}]]))

(defn get-view
  [params]
  (case (:type params)
    :user (if (:userhash params)
            (views/get-user-view params)
            (views/get-category-view params))
    :post (if (:time params)
            (views/get-post-view params)
            (views/get-category-view params))
    :tag (views/get-category-view params)
    nil (views/get-user-view {:type :user :userhash @c/my-hash-bytes})
    [:h2 (utils/get-string :nothing_here)]))

(defn get-main-page
  [params]
  (get-page
    params
    (views/get-action-bar-view params :is-main? true :show-tabs? true)
    (get-view params)))

(defn get-category-page
  [params]
  (get-page
    params
    (views/get-action-bar-view params :show-home-button? true :show-tabs? true)
    (views/get-category-view params)))

(defn get-basic-page
  [params]
  (get-page
    params
    (views/get-action-bar-view params :show-home-button? true)
    (get-view params)))
