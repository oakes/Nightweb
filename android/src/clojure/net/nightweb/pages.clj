(ns net.nightweb.pages
  (:use [neko.resource :only [get-resource get-string]]
        [neko.activity :only [set-content-view!]]
        [net.nightweb.clandroid.activity :only [defactivity]]
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
                                   get-category-view]]
        [net.nightweb.menus :only [create-main-menu]]
        [net.nightweb.actions :only [do-menu-action]]
        [nightweb.router :only [parse-url]]
        [nightweb.constants :only [my-hash-bytes]]))

(defn set-share-content
  [context content]
  (swap! (.state context) assoc :share-content content))

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
                      #(let [content {:type :users :hash my-hash-bytes}]
                         (set-share-content this content)
                         (get-user-view this content)))
          (create-tab action-bar
                      (get-string :users)
                      #(let [content {:type :users}]
                         (set-share-content this content)
                         (get-category-view this content true)))
          (create-tab action-bar
                      (get-string :posts)
                      #(let [content {:type :posts}]
                         (set-share-content this content)
                         (get-category-view this content true))))))
    (start-receiver this shutdown-receiver-name shutdown-receiver-func))
  :on-destroy
  (fn [this]
    (stop-receiver this shutdown-receiver-name)
    (stop-service this))
  :on-create-options-menu
  (fn [this menu]
    (create-main-menu this menu true)))

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
                                      (assoc params :type :users-favorites)))
      (create-tab action-bar
                  (get-string :posts)
                  #(get-category-view this
                                      (assoc params :type :posts-favorites)))))
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
  net.nightweb.TransfersPage
  :on-create
  (fn [this bundle]
    (start-receiver this shutdown-receiver-name shutdown-receiver-func)
    (let [action-bar (.getActionBar this)]
      (.setNavigationMode action-bar android.app.ActionBar/NAVIGATION_MODE_TABS)
      (.setDisplayHomeAsUpEnabled action-bar true)
      (.setTitle action-bar (get-string :transfers))
      (create-tab action-bar
                  (get-string :all)
                  #(get-category-view this {:type :all-transfers}))
      (create-tab action-bar
                  (get-string :photos)
                  #(get-category-view this {:type :photos-transfers}))
      (create-tab action-bar
                  (get-string :videos)
                  #(get-category-view this {:type :videos-transfers}))
      (create-tab action-bar
                  (get-string :audio)
                  #(get-category-view this {:type :audio-transfers}))))
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
  net.nightweb.GridPage
  :def grid-page
  :on-create
  (fn [this bundle]
    (start-service
      this
      service-name
      (fn [binder]
        (let [params (if-let [url (.getDataString (.getIntent this))]
                       (parse-url url)
                       (.getSerializableExtra (.getIntent this) "params"))
              grid-view (case (get params :type)
                          :users 
                          (do
                            (send-broadcast this params download-receiver-name)
                            (get-user-view this params))
                          (get-grid-view this []))
              action-bar (.getActionBar this)]
          (set-share-content this params)
          (.setDisplayHomeAsUpEnabled action-bar true)
          (if-let [title (get params :text)]
            (.setTitle action-bar title)
            (.setDisplayShowTitleEnabled action-bar false))
          (set-content-view! grid-page grid-view))))
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
