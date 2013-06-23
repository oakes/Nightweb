(ns net.nightweb.pages
  (:require [neko.activity :as a]
            [neko.notify :as notify]
            [neko.resource :as r]
            [neko.threading :as thread]
            [net.clandroid.activity :as activity]
            [net.clandroid.service :as service]
            [net.nightweb.actions :as actions]
            [net.nightweb.dialogs :as dialogs]
            [net.nightweb.main :as main]
            [net.nightweb.menus :as menus]
            [net.nightweb.utils :as utils]
            [net.nightweb.views :as views]
            [nightweb.constants :as c]
            [nightweb.formats :as f]
            [nightweb.router :as router]
            [nightweb.users :as users])
  (:import [android.app ActionBar]))

(def show-welcome-message? (atom true))

(defn shutdown-receiver-func
  [context intent]
  (.finish context))

(defn get-params
  [context]
  (into {} (.getSerializableExtra (.getIntent context) "params")))

(activity/defactivity
  net.nightweb.MainPage
  :on-create
  (fn [this bundle]
    (service/start-service
      this
      main/service-name
      (fn [binder]
        (let [action-bar (.getActionBar this)]
          (.setNavigationMode action-bar  ActionBar/NAVIGATION_MODE_TABS)
          (.setDisplayShowTitleEnabled action-bar false)
          (.setDisplayShowHomeEnabled action-bar false)
          (views/create-tab action-bar
                            (r/get-string :me)
                            #(let [content {:type :user
                                            :userhash @c/my-hash-bytes}]
                               (activity/set-state this :share content)
                               (views/get-user-view this content)))
          (views/create-tab action-bar
                            (r/get-string :users)
                            #(let [content {:type :user}]
                               (activity/set-state this :share content)
                               (views/get-category-view this content)))
          (views/create-tab action-bar
                            (r/get-string :posts)
                            #(let [content {:type :post}]
                               (activity/set-state this :share content)
                               (views/get-category-view this content)))
          (when (and @router/is-first-boot? @show-welcome-message?)
            (reset! show-welcome-message? false)
            (dialogs/show-welcome-dialog this)))))
    (service/start-receiver
      this main/shutdown-receiver-name shutdown-receiver-func))
  :on-new-intent
  (fn [this intent]
    (.setIntent this intent)
    (when-let [action-bar (.getActionBar this)]
      (when-let [tab (.getSelectedTab action-bar)]
        (.select tab))))
  :on-resume
  (fn [this]
    (when-let [uri-str (.getDataString (.getIntent this))]
      (dialogs/show-import-dialog this uri-str)
      (.setData (.getIntent this) nil)))
  :on-destroy
  (fn [this]
    (service/stop-receiver this main/shutdown-receiver-name)
    (service/stop-service this))
  :on-create-options-menu
  (fn [this menu]
    (menus/create-main-menu this menu true true))
  :on-activity-result
  actions/receive-result)

(activity/defactivity
  net.nightweb.CategoryPage
  :on-create
  (fn [this bundle]
    (service/start-receiver
      this main/shutdown-receiver-name shutdown-receiver-func)
    (let [params (get-params this)
          action-bar (.getActionBar this)]
      (.setNavigationMode action-bar ActionBar/NAVIGATION_MODE_TABS)
      (.setDisplayHomeAsUpEnabled action-bar true)
      (.setTitle action-bar (utils/get-string-at-runtime this (:title params)))
      (views/create-tab action-bar
                        (r/get-string :users)
                        #(views/get-category-view
                           this (assoc params :subtype :user)))
      (views/create-tab action-bar
                        (r/get-string :posts)
                        #(views/get-category-view
                           this (assoc params :subtype :post)))))
  :on-destroy
  (fn [this]
    (service/stop-receiver this main/shutdown-receiver-name))
  :on-create-options-menu
  (fn [this menu]
    (menus/create-main-menu this menu false false))
  :on-options-item-selected
  (fn [this item]
    (actions/menu-action this item))
  :on-activity-result
  actions/receive-result)

(activity/defactivity
  net.nightweb.GalleryPage
  :def gallery-page
  :on-create
  (fn [this bundle]
    (service/start-receiver
      this main/shutdown-receiver-name shutdown-receiver-func)
    (let [params (get-params this)
          action-bar (.getActionBar this)
          view (views/get-gallery-view this params)]
      (.hide action-bar)
      (a/set-content-view! gallery-page view)))
  :on-destroy
  (fn [this]
    (service/stop-receiver this main/shutdown-receiver-name))
  :on-activity-result
  actions/receive-result)

(activity/defactivity
  net.nightweb.BasicPage
  :def basic-page
  :on-create
  (fn [this bundle]
    (service/start-service
      this
      main/service-name
      (fn [binder]
        (let [params (if-let [url (.getDataString (.getIntent this))]
                       (f/url-decode url)
                       (get-params this))
              view (case (:type params)
                     :user (if (:userhash params)
                             (views/get-user-view this params)
                             (views/get-category-view this params))
                     :post (if (:time params)
                             (views/get-post-view this params)
                             (views/get-category-view this params))
                     :tag (views/get-category-view this params)
                     nil)
              action-bar (.getActionBar this)]
          (activity/set-state this :share params)
          (.setDisplayHomeAsUpEnabled action-bar true)
          (if-let [title (or (:title params)
                             (:tag params))]
            (.setTitle action-bar (utils/get-string-at-runtime this title))
            (.setDisplayShowTitleEnabled action-bar false))
          (if view
            (a/set-content-view! basic-page view)
            (thread/on-ui (notify/toast (r/get-string :nothing_here))))
          (when (:userhash params)
            (if-not (users/user-exists? (:userhash params))
              (dialogs/show-new-user-dialog this params)
              (when-not (users/user-has-content? (:userhash params))
                (dialogs/show-pending-user-dialog this)))))))
    (service/start-receiver
      this main/shutdown-receiver-name shutdown-receiver-func))
  :on-destroy
  (fn [this]
    (service/stop-receiver this main/shutdown-receiver-name)
    (service/stop-service this))
  :on-create-options-menu
  (fn [this menu]
    (menus/create-main-menu this menu true false))
  :on-options-item-selected
  (fn [this item]
    (actions/menu-action this item))
  :on-activity-result
  actions/receive-result)
