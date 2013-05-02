(ns net.nightweb.pages
  (:use [neko.activity :only [set-content-view!]]
        [neko.threading :only [on-ui]]
        [neko.resource :only [get-resource get-string]]
        [neko.notify :only [toast]]
        [net.clandroid.activity :only [set-state
                                       defactivity]]
        [net.clandroid.service :only [start-service
                                      stop-service
                                      start-receiver
                                      stop-receiver]]
        [net.nightweb.main :only [service-name
                                  shutdown-receiver-name]]
        [net.nightweb.views :only [create-tab
                                   get-user-view
                                   get-post-view
                                   get-gallery-view
                                   get-category-view]]
        [net.nightweb.utils :only [get-string-at-runtime]]
        [net.nightweb.menus :only [create-main-menu]]
        [net.nightweb.actions :only [receive-result
                                     menu-action]]
        [net.nightweb.dialogs :only [show-new-user-dialog
                                     show-pending-user-dialog
                                     show-welcome-dialog
                                     show-import-dialog]]
        [nightweb.formats :only [base32-encode
                                 url-decode]]
        [nightweb.constants :only [my-hash-bytes]]
        [nightweb.router :only [is-first-boot?
                                user-exists?
                                user-has-content?]]))

(def show-welcome-message? true)

(defn shutdown-receiver-func
  [context intent]
  (.finish context))

(defn get-params
  [context]
  (into {} (.getSerializableExtra (.getIntent context) "params")))

(defactivity
  net.nightweb.MainPage
  :on-create
  (fn [this bundle]
    (start-service
      this
      service-name
      (fn [binder]
        (let [action-bar (.getActionBar this)]
          (.setNavigationMode action-bar 
                              android.app.ActionBar/NAVIGATION_MODE_TABS)
          (.setDisplayShowTitleEnabled action-bar false)
          (.setDisplayShowHomeEnabled action-bar false)
          (create-tab action-bar
                      (get-string :me)
                      #(let [content {:type :user :userhash my-hash-bytes}]
                         (set-state this :share content)
                         (get-user-view this content)))
          (create-tab action-bar
                      (get-string :users)
                      #(let [content {:type :user}]
                         (set-state this :share content)
                         (get-category-view this content)))
          (create-tab action-bar
                      (get-string :posts)
                      #(let [content {:type :post}]
                         (set-state this :share content)
                         (get-category-view this content)))
          (when (and is-first-boot? show-welcome-message?)
            (def show-welcome-message? false)
            (show-welcome-dialog this)))))
    (start-receiver this shutdown-receiver-name shutdown-receiver-func))
  :on-new-intent
  (fn [this intent]
    (.setIntent this intent)
    (when-let [action-bar (.getActionBar this)]
      (when-let [tab (.getSelectedTab action-bar)]
        (.select tab))))
  :on-resume
  (fn [this]
    (when-let [uri-str (.getDataString (.getIntent this))]
      (show-import-dialog this uri-str)
      (.setData (.getIntent this) nil)))
  :on-destroy
  (fn [this]
    (stop-receiver this shutdown-receiver-name)
    (stop-service this))
  :on-create-options-menu
  (fn [this menu]
    (create-main-menu this menu true))
  :on-activity-result
  receive-result)

(defactivity
  net.nightweb.CategoryPage
  :on-create
  (fn [this bundle]
    (start-receiver this shutdown-receiver-name shutdown-receiver-func)
    (let [params (get-params this)
          action-bar (.getActionBar this)]
      (.setNavigationMode action-bar android.app.ActionBar/NAVIGATION_MODE_TABS)
      (.setDisplayHomeAsUpEnabled action-bar true)
      (.setTitle action-bar (get-string-at-runtime this (:title params)))
      (create-tab action-bar
                  (get-string :users)
                  #(get-category-view this (assoc params :subtype :user)))
      (create-tab action-bar
                  (get-string :posts)
                  #(get-category-view this (assoc params :subtype :post)))))
  :on-destroy
  (fn [this]
    (stop-receiver this shutdown-receiver-name))
  :on-create-options-menu
  (fn [this menu]
    (create-main-menu this menu false))
  :on-options-item-selected
  (fn [this item]
    (menu-action this item))
  :on-activity-result
  receive-result)

(defactivity
  net.nightweb.GalleryPage
  :def gallery-page
  :on-create
  (fn [this bundle]
    (start-receiver this shutdown-receiver-name shutdown-receiver-func)
    (let [params (get-params this)
          action-bar (.getActionBar this)
          view (get-gallery-view this params)]
      (.hide action-bar)
      (set-content-view! gallery-page view)))
  :on-destroy
  (fn [this]
    (stop-receiver this shutdown-receiver-name))
  :on-activity-result
  receive-result)

(defactivity
  net.nightweb.BasicPage
  :def basic-page
  :on-create
  (fn [this bundle]
    (start-service
      this
      service-name
      (fn [binder]
        (let [params (if-let [url (.getDataString (.getIntent this))]
                       (url-decode url)
                       (get-params this))
              view (case (:type params)
                     :user (if (:userhash params)
                             (get-user-view this params)
                             (get-category-view this params))
                     :post (if (:time params)
                             (get-post-view this params)
                             (get-category-view this params))
                     :tag (get-category-view this params)
                     nil)
              action-bar (.getActionBar this)]
          (set-state this :share params)
          (.setDisplayHomeAsUpEnabled action-bar true)
          (if-let [title (or (:title params)
                             (:tag params))]
            (.setTitle action-bar (get-string-at-runtime this title))
            (.setDisplayShowTitleEnabled action-bar false))
          (if view
            (set-content-view! basic-page view)
            (on-ui (toast (get-string :nothing_here))))
          (when (:userhash params)
            (if-not (user-exists? (:userhash params))
              (show-new-user-dialog this params)
              (when-not (user-has-content? (:userhash params))
                (show-pending-user-dialog this)))))))
    (start-receiver this shutdown-receiver-name shutdown-receiver-func))
  :on-destroy
  (fn [this]
    (stop-receiver this shutdown-receiver-name)
    (stop-service this))
  :on-create-options-menu
  (fn [this menu]
    (create-main-menu this menu true))
  :on-options-item-selected
  (fn [this item]
    (menu-action this item))
  :on-activity-result
  receive-result)
