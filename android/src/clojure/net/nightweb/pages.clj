(ns net.nightweb.pages
  (:use [neko.resource :only [get-resource get-string]]
        [neko.activity :only [set-content-view!]]
        [net.nightweb.clandroid.activity :only [set-state
                                                defactivity]]
        [net.nightweb.clandroid.service :only [start-service
                                               stop-service
                                               start-receiver
                                               stop-receiver
                                               send-broadcast]]
        [net.nightweb.main :only [service-name
                                  shutdown-receiver-name
                                  download-receiver-name]]
        [net.nightweb.views :only [create-tab
                                   get-grid-view
                                   get-user-view
                                   get-post-view
                                   get-gallery-view
                                   get-category-view]]
        [net.nightweb.menus :only [create-main-menu]]
        [net.nightweb.actions :only [receive-result
                                     do-menu-action]]
        [nightweb.formats :only [url-decode]]
        [nightweb.constants :only [my-hash-bytes]]))

(defn shutdown-receiver-func
  [context intent]
  (.finish context))

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
                         (get-category-view this content))))))
    (start-receiver this shutdown-receiver-name shutdown-receiver-func))
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
  net.nightweb.FavoritesPage
  :on-create
  (fn [this bundle]
    (start-receiver this shutdown-receiver-name shutdown-receiver-func)
    (let [params (into {} (.getSerializableExtra (.getIntent this) "params"))
          action-bar (.getActionBar this)]
      (.setNavigationMode action-bar android.app.ActionBar/NAVIGATION_MODE_TABS)
      (.setDisplayHomeAsUpEnabled action-bar true)
      (.setTitle action-bar (get-string :favorites))
      (create-tab action-bar
                  (get-string :users)
                  #(get-category-view this
                                      (assoc params :type :user-fav)))
      (create-tab action-bar
                  (get-string :posts)
                  #(get-category-view this
                                      (assoc params :type :post-fav)))))
  :on-destroy
  (fn [this]
    (stop-receiver this shutdown-receiver-name))
  :on-create-options-menu
  (fn [this menu]
    (create-main-menu this menu false))
  :on-options-item-selected
  (fn [this item]
    (do-menu-action this item)))

(defactivity
  net.nightweb.GalleryPage
  :def gallery-page
  :on-create
  (fn [this bundle]
    (start-receiver this shutdown-receiver-name shutdown-receiver-func)
    (let [params (into {} (.getSerializableExtra (.getIntent this) "params"))
          action-bar (.getActionBar this)
          view (get-gallery-view this params)]
      (.hide action-bar)
      (set-content-view! gallery-page view)))
  :on-destroy
  (fn [this]
    (stop-receiver this shutdown-receiver-name)))

(defactivity
  net.nightweb.BasicPage
  :def basic-page
  :on-create
  (fn [this bundle]
    (start-service
      this
      service-name
      (fn [binder]
        (let [url (.getDataString (.getIntent this))
              params (if url
                       (url-decode url)
                       (.getSerializableExtra (.getIntent this) "params"))
              view (case (get params :type)
                     :user (if (get params :userhash)
                             (get-user-view this params)
                             (get-category-view this params))
                     :post (if (get params :posthash)
                             (get-post-view this params)
                             (get-category-view this params))
                     (get-grid-view this []))
              action-bar (.getActionBar this)]
          (set-state this :share params)
          (.setDisplayHomeAsUpEnabled action-bar true)
          (if-let [title (get params :title)]
            (.setTitle action-bar title)
            (.setDisplayShowTitleEnabled action-bar false))
          (set-content-view! basic-page view)
          (if url (send-broadcast this params download-receiver-name)))))
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
    (do-menu-action this item)))
