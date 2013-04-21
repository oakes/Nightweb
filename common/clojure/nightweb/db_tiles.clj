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

(defn add-last-tile
  [content results]
  (if (> (count results) limit)
    (let [next-page (-> (get content :page)
                        (or 1)
                        (+ 1))]
      (-> results
          (pop)
          (conj (assoc content
                       :title :page
                       :background :next
                       :add-emphasis? true
                       :page next-page))))
    results))

(defn get-post-tiles
  ([post] (get-post-tiles post nil))
  ([post edit-func]
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
                          :background :profile
                          :add-emphasis? true
                          :title :author
                          :subtitle (get user :title))
         user-pointer-tile (when user-pointer
                             (assoc user-pointer
                                    :background :profile
                                    :add-emphasis? true
                                    :title :mentioned
                                    :subtitle (get user-pointer :title)))
         post-pointer-tile (when post-pointer
                             (assoc post-pointer
                                    :background :post
                                    :add-emphasis? true
                                    :title :in_reply_to))
         action-tile (if (is-me? (get post :userhash))
                       {:title :edit
                        :add-emphasis? true
                        :background :edit_post
                        :type :custom-func
                        :subtype :edit
                        :func (fn [context item]
                                (edit-func context post pics))}
                       {:title (if (= 1 (get fav :status))
                                 :remove_from_favorites
                                 :add_to_favorites)
                        :add-emphasis? true
                        :background (if (= 1 (get fav :status))
                                      :remove_fav
                                      :add_fav)
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
          (vec)))))

(defn get-user-tiles
  ([params user] (get-user-tiles params user nil nil nil))
  ([params user profile-func fav-func unfav-func]
   (let [fav (when-not (is-me? (get user :userhash))
               (get-single-fav-data {:userhash (get user :userhash)}))
         first-tiles (when (nil? (get params :page))
                       [{:title :profile
                         :add-emphasis? true
                         :background :profile
                         :userhash (get user :userhash)
                         :pichash (get user :pichash)
                         :type :custom-func
                         :subtype :profile
                         :func (fn [context item]
                                 (profile-func context user))}
                        {:title :favorites
                         :add-emphasis? true
                         :userhash (get user :userhash)
                         :background :favs
                         :type :fav}
                        (when-not (is-me? (get user :userhash))
                          {:title (if (= 1 (get fav :status))
                                    :remove_from_favorites
                                    :add_to_favorites)
                           :add-emphasis? true
                           :background
                           (if (= 1 (get fav :status))
                             :remove_fav
                             :add_fav)
                           :type :custom-func
                           :subtype :fav
                           :func
                           (fn [context item]
                             (if (= 1 (get fav :status))
                               (fav-func context item)
                               (unfav-func context item false)))
                           :userhash (get user :userhash)
                           :status (get fav :status)
                           :time (get fav :time)})])
         posts (->> (for [tile (get-post-data params)]
                      (assoc tile :background :post))
                    (into [])
                    (add-last-tile params))]
     (-> first-tiles
         (concat posts)
         (remove-dupes-and-nils)
         (vec)))))

(defn get-category-tiles
  [params]
  (let [first-tiles [(when (and (nil? (get params :subtype))
                                (nil? (get params :tag))
                                (nil? (get params :page)))
                       {:type :tag
                        :subtype (get params :type)
                        :title :tags
                        :add-emphasis? true
                        :background :tags})]
        results (->> (for [tile (get-category-data params)]
                       (case (get tile :type)
                         :user (assoc tile :background :profile)
                         :post (assoc tile :background :post)
                         tile))
                     (into [])
                     (add-last-tile params))]
    (-> first-tiles
        (concat results)
        (remove-dupes-and-nils)
        (vec))))
