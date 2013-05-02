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
    (let [next-page (-> (:page content)
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
         user-pointer (when (and (:ptrhash post)
                                 (nil? (:ptrtime post)))
                        (get-single-user-data
                          {:userhash (:ptrhash post)}))
         post-pointer (when (:ptrtime post) 
                        (get-single-post-data
                          {:userhash (:ptrhash post)
                           :time (:ptrtime post)}))
         pics (get-pic-data post (:time post) true)
         fav (when-not (is-me? (:userhash post))
               (get-single-fav-data post))
         ; create tiles based on the values
         user-tile (assoc user
                          :background :profile
                          :add-emphasis? true
                          :title :author
                          :subtitle (:title user))
         user-pointer-tile (when user-pointer
                             (assoc user-pointer
                                    :background :profile
                                    :add-emphasis? true
                                    :title :mentioned
                                    :subtitle (:title user-pointer)))
         post-pointer-tile (when post-pointer
                             (assoc post-pointer
                                    :background :post
                                    :add-emphasis? true
                                    :title :in_reply_to))
         action-tile (if (is-me? (:userhash post))
                       {:title :edit
                        :add-emphasis? true
                        :background :edit_post
                        :type :custom-func
                        :subtype :edit
                        :func (fn [context item]
                                (edit-func context post pics))}
                       {:title (if (= 1 (:status fav))
                                 :remove_from_favorites
                                 :add_to_favorites)
                        :add-emphasis? true
                        :background (if (= 1 (:status fav))
                                      :remove_fav
                                      :add_fav)
                        :type :toggle-fav
                        :userhash (:userhash post)
                        :ptrtime (:time post)
                        :status (:status fav)
                        :time (:time fav)})]
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
   (let [fav (when-not (is-me? (:userhash user))
               (get-single-fav-data {:userhash (:userhash user)}))
         first-tiles (when (nil? (:page params))
                       [{:title :profile
                         :add-emphasis? true
                         :background :profile
                         :userhash (:userhash user)
                         :pichash (:pichash user)
                         :type :custom-func
                         :subtype :profile
                         :func (fn [context item]
                                 (profile-func context user))}
                        {:title :favorites
                         :add-emphasis? true
                         :userhash (:userhash user)
                         :background :favs
                         :type :fav}
                        (when-not (is-me? (:userhash user))
                          {:title (if (= 1 (:status fav))
                                    :remove_from_favorites
                                    :add_to_favorites)
                           :add-emphasis? true
                           :background
                           (if (= 1 (:status fav))
                             :remove_fav
                             :add_fav)
                           :type :custom-func
                           :subtype :fav
                           :func
                           (fn [context item]
                             (if (= 1 (:status fav))
                               (fav-func context item)
                               (unfav-func context item false)))
                           :userhash (:userhash user)
                           :status (:status fav)
                           :time (:time fav)})])
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
  (let [first-tiles [(when (and (nil? (:subtype params))
                                (nil? (:tag params))
                                (nil? (:page params)))
                       {:type :tag
                        :subtype (:type params)
                        :title :tags
                        :add-emphasis? true
                        :background :tags})]
        results (->> (for [tile (get-category-data params)]
                       (case (:type tile)
                         :user (assoc tile :background :profile)
                         :post (assoc tile :background :post)
                         tile))
                     (into [])
                     (add-last-tile params))]
    (-> first-tiles
        (concat results)
        (remove-dupes-and-nils)
        (vec))))
