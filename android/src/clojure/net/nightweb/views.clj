(ns net.nightweb.views
  (:use [markdown.core :only [md-to-html-string]]
        [neko.ui :only [make-ui]]
        [neko.ui.mapping :only [set-classname!]]
        [neko.threading :only [on-ui]]
        [neko.resource :only [get-string get-resource]]
        [net.nightweb.utils :only [full-size
                                   thumb-size
                                   uri-to-bitmap
                                   path-to-bitmap
                                   make-dip
                                   default-text-size
                                   set-text-size]]
        [net.nightweb.actions :only [request-files
                                     clear-attachments
                                     send-post
                                     tile-action
                                     save-profile
                                     cancel
                                     toggle-fav]]
        [net.nightweb.dialogs :only [show-lost-post-dialog
                                     show-delete-post-dialog
                                     show-remove-user-dialog
                                     show-edit-post-dialog
                                     show-profile-dialog]]
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
(set-classname! :relative-layout android.widget.RelativeLayout)
(set-classname! :image-view android.widget.ImageView)
(set-classname! :view-pager android.support.v4.view.ViewPager)

(def default-tile-width 160)

(defn get-pic-path
  "Gets the full path for the given user and image hash combination."
  [user-hash-bytes image-hash-bytes]
  (when (and user-hash-bytes image-hash-bytes)
    (str (get-pic-dir (base32-encode user-hash-bytes))
         slash
         (base32-encode image-hash-bytes))))

(defn add-last-tile
  "Adds a tile to take you to the next page if necessary."
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
  "Sets the content in the given grid view."
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
          (tile-action context (get content position)))))
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

(defn get-post-view
  [context content]
  (let [view (make-ui context [:scroll-view {}
                               [:linear-layout {:orientation 1}
                                [:text-view {:layout-width :fill
                                             :text-is-selectable true}]
                                [:text-view {:layout-width :fill}]]])
        linear-layout (.getChildAt view 0)
        text-view (.getChildAt linear-layout 0)
        date-view (.getChildAt linear-layout 1)
        grid-view (get-grid-view context [] true)
        pad (make-dip context 10)]
    (.setPadding text-view pad pad pad pad)
    (.setPadding date-view pad pad pad pad)
    (set-text-size text-view default-text-size)
    (set-text-size date-view default-text-size)
    (.addView linear-layout grid-view)
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
                      :func (fn [context item]
                              (show-edit-post-dialog context post pics))}
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
          (on-ui (let [html-text (md-to-html-string (get post :body))
                       markdown-text (android.text.Html/fromHtml html-text)
                       date-format (java.text.DateFormat/getDateTimeInstance
                                     java.text.DateFormat/MEDIUM
                                     java.text.DateFormat/SHORT)
                       spannable android.widget.TextView$BufferType/SPANNABLE]
                   (.setText text-view markdown-text spannable)
                   (.setText date-view (->> (get post :time)
                                            (java.util.Date.)
                                            (.format date-format))))
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
                (let [image-view (android.widget.ImageView. context)
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

(defn get-profile-view
  [context content]
  (let [bold android.graphics.Typeface/DEFAULT_BOLD
        view (if (is-me? (get content :userhash))
               (make-ui context [:scroll-view {}
                                 [:linear-layout {:orientation 1}
                                  [:edit-text {:single-line true
                                               :layout-width :fill
                                               :tag "profile-title"}]
                                  [:edit-text {:layout-width :fill
                                               :tag "profile-body"}]
                                  [:relative-layout {}]]])
               (make-ui context [:scroll-view {}
                                 [:linear-layout {:orientation 1}
                                  [:text-view {:single-line true
                                               :layout-width :fill
                                               :text-is-selectable true
                                               :typeface bold}]
                                  [:text-view {:layout-width :fill
                                               :text-is-selectable true}]
                                  [:relative-layout {}]]]))
        linear-layout (.getChildAt view 0)
        text-name (.getChildAt linear-layout 0)
        text-body (.getChildAt linear-layout 1)
        relative-layout (.getChildAt linear-layout 2)
        image-view (proxy [android.widget.ImageButton] [context]
                     (onMeasure [width height]
                       (proxy-super onMeasure width width)))
        clear-btn (android.widget.Button. context)]
    ; set padding and text size
    (.setPadding linear-layout 10 10 10 10)
    (set-text-size text-name default-text-size)
    (set-text-size text-body default-text-size)
    ; set text content
    (when (is-me? (get content :userhash))
      (.setHint text-name (get-string :name))
      (.setHint text-body (get-string :about_me)))
    (.setText text-name (get content :title))
    (.setText text-body (get content :body))
    (.setText clear-btn (get-string :clear))
    ; set layout params for image view and clear button
    (let [fill android.widget.RelativeLayout$LayoutParams/FILL_PARENT
          params (android.widget.RelativeLayout$LayoutParams. fill 0)]
      (.setLayoutParams image-view params))
    (let [wrap android.widget.RelativeLayout$LayoutParams/WRAP_CONTENT
          params (android.widget.RelativeLayout$LayoutParams. wrap wrap)]
      (.addRule params android.widget.RelativeLayout/ALIGN_PARENT_TOP)
      (.addRule params android.widget.RelativeLayout/ALIGN_PARENT_RIGHT)
      (.setLayoutParams clear-btn params))
    ; set image view and clear button parameters
    (.setTag image-view "profile-image")
    (.setScaleType image-view android.widget.ImageView$ScaleType/CENTER_CROP)
    (.setBackgroundResource image-view (get-resource :drawable :profile))
    (let [bitmap (-> (get-pic-path (get content :userhash)
                                   (get content :pichash))
                     (path-to-bitmap thumb-size))]
      (.setImageBitmap image-view bitmap))
    (.addView relative-layout image-view)
    (when (is-me? (get content :userhash))
      (.setOnClickListener
        image-view
        (proxy [android.view.View$OnClickListener] []
          (onClick [v]
            (request-files context
                           "image/*"
                           (fn [uri]
                             (let [pic (uri-to-bitmap context (.toString uri))]
                               (.setImageBitmap image-view pic)))))))
      (.setOnClickListener clear-btn
                           (proxy [android.view.View$OnClickListener] []
                             (onClick [v]
                               (.setImageBitmap image-view nil))))
      (.addView relative-layout clear-btn))
    ; return the parent view
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
                            :func (fn [context item]
                                    (->> (get-profile-view context user)
                                         (show-profile-dialog context user)))}
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
                              :type :custom-func
                              :func
                              (fn [context item]
                                (if (= 1 (get fav :status))
                                  (show-remove-user-dialog context item)
                                  (toggle-fav context item false)))
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
