(ns nightweb.db_tiles
  (:use [nightweb.db :only [limit
                            get-pic-data
                            get-post-data
                            get-category-data
                            get-single-user-data
                            get-single-post-data
                            get-single-fav-data]]
        [nightweb.formats :only [remove-dupes-and-nils]]
        [nightweb.constants :only [is-me?]]))

(def get-resource nil)
(def get-string nil)

(defn set-resource-funcs
  [res-func str-func]
  (def get-resource res-func)
  (def get-string str-func))

(defn add-last-tile
  [content results]
  (if (> (count results) limit)
    (let [next-page (-> (get content :page)
                        (or 1)
                        (+ 1))]
      (-> results
          (pop)
          (conj (assoc content
                       :title (get-string :page)
                       :background (get-resource :next)
                       :add-emphasis? true
                       :page next-page))))
    results))

(defn get-post-tiles
  [post edit-func]
  (let [; read values from the database
        user (get-single-user-data post)
        user-pointer (when (and (get post :ptrhash)
                                (nil? (get post :ptrtime)))
                       (get-single-user-data
                         {:userhash (get post :ptrhash)}))
        post-pointer (when (get post :ptrtime) 
                       (get-single-post-data
                         {:userhash (get post :ptrhash)
                          :time (get post :ptrtime)}))
        pics (get-pic-data post (get post :time) true)
        fav (when-not (is-me? (get post :userhash))
              (get-single-fav-data post))
        ; create tiles based on the values
        user-tile (assoc user
                         :background (get-resource :profile)
                         :add-emphasis? true
                         :title (get-string :author)
                         :subtitle (get user :title))
        user-pointer-tile (when user-pointer
                            (assoc user-pointer
                                   :background (get-resource :profile)
                                   :add-emphasis? true
                                   :title (get-string :mentioned)
                                   :subtitle (get user-pointer :title)))
        post-pointer-tile (when post-pointer
                            (assoc post-pointer
                                   :background (get-resource :post)
                                   :add-emphasis? true
                                   :title (get-string :in_reply_to)))
        action-tile (if (is-me? (get post :userhash))
                      {:title (get-string :edit)
                       :add-emphasis? true
                       :background (get-resource :edit_post)
                       :type :custom-func
                       :func (fn [context item]
                               (edit-func context post pics))}
                      {:title (if (= 1 (get fav :status))
                                (get-string :remove_from_favorites)
                                (get-string :add_to_favorites))
                       :add-emphasis? true
                       :background (if (= 1 (get fav :status))
                                     (get-resource :remove_fav)
                                     (get-resource :add_fav))
                       :type :toggle-fav
                       :userhash (get post :userhash)
                       :ptrtime (get post :time)
                       :status (get fav :status)
                       :time (get fav :time)})]
    (-> [user-tile
         user-pointer-tile
         post-pointer-tile
         action-tile]
         (concat pics)
         (remove-dupes-and-nils)
         (vec))))

(defn get-user-tiles
  [content profile-func fav-func unfav-func]
  (let [user (get-single-user-data content)
        fav (when-not (is-me? (get user :userhash))
              (get-single-fav-data {:userhash (get user :userhash)}))
        first-tiles (when (nil? (get content :page))
                      [{:title (get-string :profile)
                        :add-emphasis? true
                        :background (get-resource :profile)
                        :userhash (get user :userhash)
                        :pichash (get user :pichash)
                        :type :custom-func
                        :func (fn [context item]
                                (profile-func context user))}
                       {:title (get-string :favorites)
                        :add-emphasis? true
                        :userhash (get user :userhash)
                        :background (get-resource :favs)
                        :type :fav}
                       (when-not (is-me? (get user :userhash))
                         {:title (if (= 1 (get fav :status))
                                   (get-string :remove_from_favorites)
                                   (get-string :add_to_favorites))
                          :add-emphasis? true
                          :background
                          (if (= 1 (get fav :status))
                            (get-resource :remove_fav)
                            (get-resource :add_fav))
                          :type :custom-func
                          :func
                          (fn [context item]
                            (if (= 1 (get fav :status))
                              (fav-func context item)
                              (unfav-func context item false)))
                          :userhash (get user :userhash)
                          :status (get fav :status)
                          :time (get fav :time)})])
        posts (->> (for [tile (get-post-data content)]
                     (assoc tile :background (get-resource :post)))
                   (into [])
                   (add-last-tile content))]
    (-> first-tiles
        (concat posts)
        (remove-dupes-and-nils)
        (vec))))

(defn get-category-tiles
  [content]
  (let [first-tiles [(when (and (nil? (get content :subtype))
                                (nil? (get content :tag))
                                (nil? (get content :page)))
                       {:type :tag
                        :subtype (get content :type)
                        :title (get-string :tags)
                        :add-emphasis? true
                        :background (get-resource :tags)})]
        results (->> (for [tile (get-category-data content)]
                       (case (get tile :type)
                         :user (assoc tile :background (get-resource :profile))
                         :post (assoc tile :background (get-resource :post))
                         tile))
                     (into [])
                     (add-last-tile content))]
    (-> first-tiles
        (concat results)
        (remove-dupes-and-nils)
        (vec))))
