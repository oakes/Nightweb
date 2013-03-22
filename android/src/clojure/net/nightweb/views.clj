(ns net.nightweb.views
  (:use [neko.ui :only [make-ui]]
        [neko.ui.mapping :only [set-classname!]]
        [neko.threading :only [on-ui]]
        [neko.resource :only [get-string get-resource]]
        [net.nightweb.utils :only [full-size
                                   thumb-size
                                   uri-to-bitmap
                                   path-to-bitmap]]
        [net.nightweb.actions :only [request-files
                                     clear-attachments
                                     show-dialog
                                     show-lost-post-dialog
                                     do-send-post
                                     do-delete-post
                                     do-tile-action
                                     do-save-profile
                                     do-cancel]]
        [nightweb.db :only [limit
                            get-user-data
                            get-post-data
                            get-pic-data
                            get-single-post-data
                            get-single-fav-data
                            get-category-data]]
        [nightweb.formats :only [remove-dupes-and-nils
                                 base32-encode]]
        [nightweb.constants :only [is-me?
                                   slash
                                   get-pic-dir]]))

(set-classname! :scroll-view android.widget.ScrollView)
(set-classname! :frame-layout android.widget.FrameLayout)
(set-classname! :image-view android.widget.ImageView)
(set-classname! :view-pager android.support.v4.view.ViewPager)

(def default-text-size 20)
(def default-tile-width 160)

(defn set-text-size
  [view size]
  (.setTextSize view android.util.TypedValue/COMPLEX_UNIT_DIP size))

(defn make-dip
  [context number]
  (-> (.getResources context)
      (.getDisplayMetrics)
      (.density)
      (* number)
      (int)))

(defn get-pic-path
  [user-hash-bytes image-hash-bytes]
  (when (and user-hash-bytes image-hash-bytes)
    (str (get-pic-dir (base32-encode user-hash-bytes))
         slash
         (base32-encode image-hash-bytes))))

(defn add-last-tile
  [content results]
  (if (> (count results) limit)
    (let [next-page (-> (get content :page)
                        (or 1)
                        (+ 1))]
      (-> results
          (pop)
          (conj (assoc content
                       :title (str (get-string :page) " " next-page)
                       :add-emphasis? true
                       :page next-page))))
    results))

(defn set-grid-view-tiles
  [context content view]
  (let [num-columns (.getNumColumns view)
        width (.getWidth view)
        tile-view-width (if (and (> width 0) (> num-columns 0))
                          (int (/ width num-columns))
                          (make-dip context default-tile-width))
        layout-params (android.widget.AbsListView$LayoutParams.
                                                  tile-view-width
                                                  tile-view-width)]
    (.setAdapter
      view
      (proxy [android.widget.BaseAdapter] []
        (getItem [position] (get content position))
        (getItemId [position] 0)
        (getCount [] (count content))
        (getView [position convert-view parent]
          (let [white android.graphics.Color/WHITE
                not-initialized (nil? convert-view)
                tile-view (if not-initialized
                            (make-ui context
                                     [:frame-layout {}
                                      [:image-view {}]
                                      [:linear-layout {:orientation 1}
                                       [:text-view {:text-color white
                                                    :layout-height 0
                                                    :layout-width :fill
                                                    :layout-weight 1}]
                                       [:text-view {:text-color white}]]])
                            convert-view)
                item (get content position)
                image-view (.getChildAt tile-view 0)
                linear-layout (.getChildAt tile-view 1)
                text-top (.getChildAt linear-layout 0)
                text-bottom (.getChildAt linear-layout 1)]
            (when not-initialized
              (.setScaleType image-view
                             android.widget.ImageView$ScaleType/CENTER_CROP)
              (set-text-size text-top default-text-size)
              (set-text-size text-bottom default-text-size)
              (.setLayoutParams tile-view layout-params)
              (let [pad (make-dip context 5)
                    radius (make-dip context 10)
                    black android.graphics.Color/BLACK]
                (.setPadding linear-layout pad pad pad pad)
                (.setShadowLayer text-top radius 0 0 black)
                (.setShadowLayer text-bottom radius 0 0 black)))
            (when (get item :add-emphasis?)
              (.setTypeface text-top android.graphics.Typeface/DEFAULT_BOLD)
              (.setGravity text-top android.view.Gravity/CENTER_HORIZONTAL))
            (when-not (get item :add-emphasis?)
              (.setTypeface text-top android.graphics.Typeface/DEFAULT)
              (.setGravity text-top android.view.Gravity/LEFT))
            (if-let [background (get item :background)]
              (.setBackgroundResource image-view background)
              (.setBackground image-view nil))
            (let [pic-hash-str (base32-encode (get item :pichash))]
              (.setTag image-view pic-hash-str)
              (.setImageBitmap image-view nil)
              (when pic-hash-str
                (future
                  (let [image-bitmap (-> (get-pic-path (get item :userhash)
                                                       (get item :pichash))
                                         (path-to-bitmap thumb-size))]
                    (on-ui
                      (when (= pic-hash-str (.getTag image-view))
                        (.setImageBitmap image-view image-bitmap)))))))
            (if-let [title (get item :title)]
              (.setText text-top title)
              (.setText text-top (get item :body)))
            (if-let [subtitle (get item :subtitle)]
              (.setText text-bottom subtitle)
              (.setVisibility text-bottom android.view.View/GONE))
            tile-view))))
    (.setOnItemClickListener
      view
      (proxy [android.widget.AdapterView$OnItemClickListener] []
        (onItemClick [parent v position id]
          (do-tile-action context (get content position)))))
    (.notifyDataSetChanged (.getAdapter view))))

(defn get-grid-view
  ([context content] (get-grid-view context content false))
  ([context content make-height-fit-content?]
   (let [tile-view-min (make-dip context default-tile-width)
         view (proxy [android.widget.GridView] [context]
                (onMeasure [width-spec height-spec]
                  (let [w (android.view.View$MeasureSpec/getSize width-spec)
                        num-columns (int (/ w tile-view-min))]
                    (.setNumColumns this num-columns))
                  (if make-height-fit-content?
                    (let [params (.getLayoutParams this)
                          size (bit-shift-right java.lang.Integer/MAX_VALUE 2)
                          mode android.view.View$MeasureSpec/AT_MOST
                          h-spec (android.view.View$MeasureSpec/makeMeasureSpec
                                                    size mode)]
                      (proxy-super onMeasure width-spec h-spec))
                    (proxy-super onMeasure width-spec height-spec))))]
     (when (> (count content) 0)
       (set-grid-view-tiles context content view))
     view)))

(defn get-new-post-view
  [context content]
  (let [view (make-ui context [:linear-layout {:orientation 1}
                               [:edit-text {:min-lines 10
                                            :tag "post-body"}]])
        text-view (.getChildAt view 0)]
    (set-text-size text-view default-text-size)
    (.setText text-view (get content :body))
    (clear-attachments context)
    view))

(defn get-post-view
  [context content]
  (let [view (make-ui context [:scroll-view {}
                               [:linear-layout {:orientation 1}
                                [:text-view {:layout-width :fill
                                             :text-is-selectable true}]]])
        linear-layout (.getChildAt view 0)
        text-view (.getChildAt linear-layout 0)
        grid-view (get-grid-view context [] true)
        pad (make-dip context 10)]
    (.setPadding text-view pad pad pad pad)
    (.addView linear-layout grid-view)
    (set-text-size text-view default-text-size)
    (future
      (let [post (get-single-post-data content)
            user (assoc (get-user-data content)
                        :background (get-resource :drawable :profile)
                        :subtitle (get-string :author))
            pics (get-pic-data content (get content :time))
            fav (when-not (is-me? (get content :userhash))
                  (get-single-fav-data content))
            action (if (is-me? (get content :userhash))
                     {:title (get-string :edit)
                      :add-emphasis? true
                      :background (get-resource :drawable :edit_post)
                      :type :custom-func
                      :func
                      (fn [context item]
                        (show-dialog context
                                     nil
                                     (get-new-post-view context post)
                                     {:positive-name (get-string :send)
                                      :positive-func
                                      (fn [context dialog-view button-view]
                                        (do-send-post context
                                                      dialog-view
                                                      button-view
                                                      (get content :time)
                                                      (for [pic pics]
                                                        (get pic :pichash))
                                                      1))
                                      :neutral-name (get-string :delete)
                                      :neutral-func
                                      (fn [context dialog-view button-view]
                                        (do-delete-post context
                                                        dialog-view
                                                        button-view
                                                        (get content :time)))
                                      :negative-name (get-string :cancel)
                                      :negative-func do-cancel}))}
                     {:title (if (= 1 (get fav :status))
                               (get-string :remove_from_favorites)
                               (get-string :add_to_favorites))
                      :add-emphasis? true
                      :background (if (= 1 (get fav :status))
                                    (get-resource :drawable :remove_fav)
                                    (get-resource :drawable :add_fav))
                      :type :toggle-fav
                      :userhash (get content :userhash)
                      :ptrtime (get content :time)
                      :status (get fav :status)
                      :time (get fav :time)})
            total-results (vec (concat [user action] pics))]
        (if (nil? (get post :body))
          (show-lost-post-dialog context)
          (on-ui (.setText text-view (get post :body))
                 (set-grid-view-tiles context total-results grid-view)))))
    view))

(defn get-gallery-view
  [context content]
  (let [view (make-ui context [:view-pager {}])]
    (future
      (let [pics (get-pic-data content (get content :ptrtime))]
        (on-ui
          (.setAdapter
            view
            (proxy [android.support.v4.view.PagerAdapter] []
              (destroyItem [container position object]
                (.removeView container object))
              (getCount [] (count pics))
              (instantiateItem [container pos]
                (let [image-view (net.nightweb.TouchImageView. context)
                      bitmap (-> (get-pic-path (get-in pics [pos :userhash])
                                               (get-in pics [pos :pichash]))
                                 (path-to-bitmap full-size))]
                  (.setImageBitmap image-view bitmap)
                  (.addView container image-view)
                  image-view))
              (isViewFromObject [view object] (= view object))
              (setPrimaryItem [container position object])))
          (.setCurrentItem view
                           (->> pics
                                (filter (fn [pic]
                                          (java.util.Arrays/equals
                                            (get pic :pichash)
                                            (get content :pichash))))
                                (first)
                                (.indexOf pics))))))
    view))

(defn get-search-view
  [context content]
  (let [view (make-ui context [:linear-layout {}])]
    view))

(defn get-profile-view
  [context content]
  (let [bold android.graphics.Typeface/DEFAULT_BOLD
        view (if (is-me? (get content :userhash))
               (make-ui context [:scroll-view {}
                                 [:linear-layout {:orientation 1}
                                  [:edit-text {:lines 1
                                               :layout-width :fill
                                               :tag "profile-title"}]
                                  [:edit-text {:layout-width :fill
                                               :tag "profile-body"}]]])
               (make-ui context [:scroll-view {}
                                 [:linear-layout {:orientation 1}
                                  [:text-view {:lines 1
                                               :layout-width :fill
                                               :text-is-selectable true
                                               :typeface bold}]
                                  [:text-view {:layout-width :fill
                                               :text-is-selectable true}]]]))
        linear-layout (.getChildAt view 0)
        text-name (.getChildAt linear-layout 0)
        text-body (.getChildAt linear-layout 1)
        image-view (proxy [android.widget.ImageButton] [context]
                     (onMeasure [width height]
                       (proxy-super onMeasure width width)))
        fill android.widget.LinearLayout$LayoutParams/FILL_PARENT
        layout-params (android.widget.LinearLayout$LayoutParams. fill 0)]
    (.setPadding linear-layout 10 10 10 10)
    (set-text-size text-name default-text-size)
    (set-text-size text-body default-text-size)
    (when (is-me? (get content :userhash))
      (.setHint text-name (get-string :name))
      (.setHint text-body (get-string :about_me)))
    (.setText text-name (get content :title))
    (.setText text-body (get content :body))
    (.setLayoutParams image-view layout-params)
    (.setTag image-view "profile-image")
    (.setScaleType image-view android.widget.ImageView$ScaleType/CENTER_CROP)
    (.setBackgroundResource image-view (get-resource :drawable :profile))
    (let [bitmap (-> (get-pic-path (get content :userhash)
                                   (get content :pichash))
                     (path-to-bitmap thumb-size))]
      (.setImageBitmap image-view bitmap))
    (when (is-me? (get content :userhash))
      (.setOnClickListener
        image-view
        (proxy [android.view.View$OnClickListener] []
          (onClick [v]
            (request-files context
                           "image/*"
                           (fn [uri]
                             (let [pic (uri-to-bitmap context (.toString uri))]
                               (.setImageBitmap image-view pic))))))))
    (.addView linear-layout image-view)
    view))

(defn get-user-view
  [context content]
  (let [grid-view (get-grid-view context [])]
    (future
      (let [user (get-user-data content)
            fav (when-not (is-me? (get user :userhash))
                  (get-single-fav-data {:userhash (get user :userhash)}))
            first-tiles (when (nil? (get content :page))
                          [{:title (get-string :profile)
                            :add-emphasis? true
                            :background (get-resource :drawable :profile)
                            :userhash (get user :userhash)
                            :pichash (get user :pichash)
                            :type :custom-func
                            :func
                            (fn [context item]
                              (show-dialog
                                context
                                nil
                                (get-profile-view context user)
                                (if (is-me? (get user :userhash))
                                  {:positive-name (get-string :save)
                                   :positive-func do-save-profile
                                   :negative-name (get-string :cancel)
                                   :negative-func do-cancel}
                                  {:positive-name (get-string :ok)
                                   :positive-func do-cancel})))}
                           {:title (get-string :favorites)
                            :add-emphasis? true
                            :userhash (get user :userhash)
                            :background (get-resource :drawable :favs)
                            :type :fav}
                           (when-not (is-me? (get user :userhash))
                             {:title (if (= 1 (get fav :status))
                                       (get-string :remove_from_favorites)
                                       (get-string :add_to_favorites))
                              :add-emphasis? true
                              :background (if (= 1 (get fav :status))
                                            (get-resource :drawable :remove_fav)
                                            (get-resource :drawable :add_fav))
                              :type :toggle-user-fav
                              :userhash (get user :userhash)
                              :status (get fav :status)
                              :time (get fav :time)})])
            posts (->> (for [tile (get-post-data content)]
                         (assoc tile
                                :background
                                (get-resource :drawable :post)))
                       (into [])
                       (add-last-tile content))
            grid-content (-> first-tiles
                             (concat posts)
                             (remove-dupes-and-nils)
                             (vec))]
        (on-ui (set-grid-view-tiles context grid-content grid-view))))
    grid-view))

(defn get-category-view
  [context content]
  (let [grid-view (get-grid-view context [])]
    (future
      (let [results (for [tile (get-category-data content)]
                      (case (get tile :type)
                        :user (assoc tile
                                     :background
                                     (get-resource :drawable :profile))
                        :post (assoc tile
                                     :background
                                     (get-resource :drawable :post))
                        tile))
            grid-content (add-last-tile content (into [] results))]
        (on-ui (set-grid-view-tiles context grid-content grid-view))))
    grid-view))

(defn create-tab
  [action-bar title create-view]
  (let [tab (.newTab action-bar)
        fragment (proxy [android.app.Fragment] []
                   (onCreateView [layout-inflater viewgroup bundle]
                     (create-view)))
        listener (proxy [android.app.ActionBar$TabListener] []
                   (onTabSelected [tab ft]
                                  (.add ft
                                        (get-resource :id :android/content)
                                        fragment))
                   (onTabUnselected [tab ft]
                                    (.remove ft fragment))
                   (onTabReselected [tab ft]))]
    (.setText tab title)
    (.setTabListener tab listener)
    (.addTab action-bar tab)))
