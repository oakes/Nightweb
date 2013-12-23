(ns net.nightweb.pages
  (:require [neko.activity :as a]
            [neko.notify :as notify]
            [neko.resource :as r]
            [neko.threading :as thread]
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
  (:import [android.app ActionBar Activity]))

(def show-welcome-message? (atom true))

(defn start-shutdown-receiver
  [^Activity context]
  (->> (fn [^Activity context intent]
         (.finish context))
       (service/start-receiver context main/shutdown-receiver-name)
       (swap! (.state context) assoc main/shutdown-receiver-name)))

(defn stop-shutdown-receiver
  [^Activity context]
  (when-let [receiver (get @(.state context) main/shutdown-receiver-name)]
    (service/stop-receiver context receiver)))

(defn get-params
  [^Activity context]
  (into {} (.getSerializableExtra (.getIntent context) "params")))

(a/defactivity
  net.nightweb.MainPage
  :state (atom {})
  :on-create
  (fn [^Activity this bundle]
    (->> (fn [binder]
           (let [action-bar (.getActionBar this)]
             (.setNavigationMode action-bar  ActionBar/NAVIGATION_MODE_TABS)
             (.setDisplayShowTitleEnabled action-bar false)
             (.setDisplayShowHomeEnabled action-bar false)
             (views/create-tab action-bar
                               (r/get-string :me)
                               #(let [content {:type :user
                                               :userhash @c/my-hash-bytes}]
                                  (utils/set-state this :share content)
                                  (views/get-user-view this content)))
             (views/create-tab action-bar
                               (r/get-string :users)
                               #(let [content {:type :user}]
                                  (utils/set-state this :share content)
                                  (views/get-category-view this content)))
             (views/create-tab action-bar
                               (r/get-string :posts)
                               #(let [content {:type :post}]
                                  (utils/set-state this :share content)
                                  (views/get-category-view this content)))
             (when (and @router/is-first-boot? @show-welcome-message?)
               (reset! show-welcome-message? false)
               (dialogs/show-welcome-dialog this))))
         (service/start-service this main/service-name)
         (swap! (.state this) assoc :service))
    (start-shutdown-receiver this))
  :on-new-intent
  (fn [^Activity this intent]
    (.setIntent this intent)
    (when-let [action-bar (.getActionBar this)]
      (when-let [tab (.getSelectedTab action-bar)]
        (.select tab))))
  :on-resume
  (fn [^Activity this]
    (when-let [uri-str (.getDataString (.getIntent this))]
      (dialogs/show-import-dialog this uri-str)
      (.setData (.getIntent this) nil)))
  :on-destroy
  (fn [^Activity this]
    (stop-shutdown-receiver this)
    (service/stop-service (:service @(.state this))))
  :on-create-options-menu
  (fn [^Activity this menu]
    (menus/create-main-menu this menu true true))
  :on-activity-result
  actions/receive-result)

(a/defactivity
  net.nightweb.CategoryPage
  :state (atom {})
  :on-create
  (fn [^Activity this bundle]
    (start-shutdown-receiver this)
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
  (fn [^Activity this]
    (stop-shutdown-receiver this))
  :on-create-options-menu
  (fn [^Activity this menu]
    (menus/create-main-menu this menu false false))
  :on-options-item-selected
  (fn [^Activity this item]
    (actions/menu-action this item))
  :on-activity-result
  actions/receive-result)

(a/defactivity
  net.nightweb.GalleryPage
  :state (atom {})
  :on-create
  (fn [^Activity this bundle]
    (start-shutdown-receiver this)
    (let [params (get-params this)
          action-bar (.getActionBar this)
          view (views/get-gallery-view this params)]
      (.hide action-bar)
      (a/set-content-view! this view)))
  :on-destroy
  (fn [^Activity this]
    (stop-shutdown-receiver this))
  :on-activity-result
  actions/receive-result)

(a/defactivity
  net.nightweb.BasicPage
  :state (atom {})
  :on-create
  (fn [^Activity this bundle]
    (->> (fn [binder]
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
             (utils/set-state this :share params)
             (.setDisplayHomeAsUpEnabled action-bar true)
             (if-let [title (or (:title params) (:tag params))]
               (.setTitle action-bar (utils/get-string-at-runtime this title))
               (.setDisplayShowTitleEnabled action-bar false))
             (if view
               (a/set-content-view! this view)
               (thread/on-ui (notify/toast (r/get-string :nothing_here))))
             (when (:userhash params)
               (if-not (users/user-exists? (:userhash params))
                 (dialogs/show-new-user-dialog this params)
                 (when-not (users/user-has-content? (:userhash params))
                   (dialogs/show-pending-user-dialog this))))))
         (service/start-service this main/service-name)
         (swap! (.state this) assoc :service))
    (start-shutdown-receiver this))
  :on-destroy
  (fn [^Activity this]
    (stop-shutdown-receiver this)
    (service/stop-service (:service @(.state this))))
  :on-create-options-menu
  (fn [^Activity this menu]
    (menus/create-main-menu this menu true false))
  :on-options-item-selected
  (fn [^Activity this item]
    (actions/menu-action this item))
  :on-activity-result
  actions/receive-result)
