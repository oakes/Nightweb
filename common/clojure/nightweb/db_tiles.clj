(ns nightweb.db_tiles
  (:require [nightweb.constants :as c]
            [nightweb.db :as db]
            [nightweb.formats :as f]))

(defn add-last-tile
  [content results]
  (if (> (count results) db/limit)
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
         user (db/get-single-user-data post)
         user-pointer (when (and (:ptrhash post)
                                 (nil? (:ptrtime post)))
                        (db/get-single-user-data
                          {:userhash (:ptrhash post)}))
         post-pointer (when (:ptrtime post) 
                        (db/get-single-post-data
                          {:userhash (:ptrhash post)
                           :time (:ptrtime post)}))
         pics (db/get-pic-data post (:time post) false)
         fav (when-not (c/is-me? (:userhash post))
               (db/get-single-fav-data post))
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
         action-tile (if (c/is-me? (:userhash post))
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
                        :time (:time post)
                        :status (:status fav)})]
     (-> [user-tile
          user-pointer-tile
          post-pointer-tile
          action-tile]
          (concat (db/get-pic-data post (:time post) true))
          (f/remove-dupes-and-nils)
          (vec)))))

(defn get-user-tiles
  ([params user] (get-user-tiles params user nil))
  ([params user profile-func]
   (let [fav (when-not (c/is-me? (:userhash user))
               (db/get-single-fav-data {:userhash (:userhash user)}))
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
                         :type :fav
                         :subtype :user}
                        (when-not (c/is-me? (:userhash user))
                          {:title (if (= 1 (:status fav))
                                    :remove_from_favorites
                                    :add_to_favorites)
                           :add-emphasis? true
                           :background
                           (if (= 1 (:status fav))
                             :remove_fav
                             :add_fav)
                           :type :toggle-fav
                           :userhash (:userhash user)
                           :status (:status fav)})])
         posts (->> (for [tile (db/get-post-data params)]
                      (assoc tile :background :post))
                    (into [])
                    (add-last-tile params))]
     (-> first-tiles
         (concat posts)
         f/remove-dupes-and-nils
         vec))))

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
        results (->> (for [tile (db/get-category-data params)]
                       (case (:type tile)
                         :user (assoc tile :background :profile)
                         :post (assoc tile :background :post)
                         tile))
                     (into [])
                     (add-last-tile params))]
    (-> first-tiles
        (concat results)
        (f/remove-dupes-and-nils)
        (vec))))
