(ns net.nightweb.views
  (:use [neko.ui :only [make-ui]]
        [neko.ui.mapping :only [set-classname!]]
        [neko.threading :only [on-ui]]
        [neko.resource :only [get-string get-resource]]
        [net.nightweb.actions :only [request-files
                                     clear-attachments
                                     show-dialog
                                     do-tile-action
                                     do-save-profile
                                     do-cancel]]
        [nightweb.io :only [read-prev-file]]
        [nightweb.db :only [run-query
                            get-user-data
                            get-post-data
                            get-single-post-data
                            get-category-data]]
        [nightweb.torrents :only [is-connecting?]]))

(set-classname! :scroll-view android.widget.ScrollView)
(set-classname! :frame-layout android.widget.FrameLayout)
(set-classname! :image-view android.widget.ImageView)

(def default-text-size 20)
(def default-tile-width 160)

(defn set-text-size
  [view size]
  (.setTextSize view android.util.TypedValue/COMPLEX_UNIT_DIP size))

(defn set-image-uri
  [context image-view uri]
  (let [cr (.getContentResolver context)
        bitmap (android.provider.MediaStore$Images$Media/getBitmap cr uri)]
    (.setImageBitmap image-view bitmap)))

(defn make-dip
  [context width]
  (int (* (.density (.getDisplayMetrics (.getResources context))) width)))

(defn set-grid-view-tiles
  [context content view]
  (.setAdapter
    view
    (proxy [android.widget.BaseAdapter] []
      (getItem [position] nil)
      (getItemId [position] 0)
      (getCount [] (count content))
      (getView [position convert-view parent]
        (let [not-initialized (= convert-view nil)
              white android.graphics.Color/WHITE
              bottom android.view.Gravity/BOTTOM
              tile-view (if not-initialized
                          (make-ui context
                                   [:frame-layout {}
                                    [:image-view {}]
                                    [:linear-layout {:orientation 1}
                                     [:text-view {:text-color white}]
                                     [:text-view {:text-color white
                                                  :gravity bottom}]]])
                          convert-view)
              num-columns (.getNumColumns view)
              width (.getWidth view)
              tile-view-width (if (and (> width 0) (> num-columns 0))
                                (int (/ width num-columns))
                                (make-dip context default-tile-width))
              layout-params (android.widget.AbsListView$LayoutParams.
                                                        tile-view-width
                                                        tile-view-width)]
          (if not-initialized
            (let [black android.graphics.Color/BLACK
                  item (get content position)
                  image-view (.getChildAt tile-view 0)
                  linear-layout (.getChildAt tile-view 1)
                  text-top (.getChildAt linear-layout 0)
                  text-bottom (.getChildAt linear-layout 1)
                  pad (make-dip context 5)
                  radius (make-dip context 10)]
              (if (get item :add-emphasis?)
                (.setTypeface text-top
                              android.graphics.Typeface/DEFAULT_BOLD))
              (.setImageBitmap image-view
                               (read-prev-file (get item :userhash)
                                                (get item :prevhash)))
              (set-text-size text-top default-text-size)
              (set-text-size text-bottom 14)
              (.setPadding linear-layout pad pad pad pad)
              (.setMaxLines text-top
                            (int (- (/ (- tile-view-width pad pad)
                                       (make-dip context default-text-size))
                                    1)))
              (.setBackgroundResource tile-view
                                      (get-resource :drawable :border))
              (if-let [title (get item :title)]
                (.setText text-top title)
                (.setText text-top (get item :body)))
              (.setText text-bottom (get item :subtitle))
              (.setShadowLayer text-top radius 0 0 black)
              (.setShadowLayer text-bottom radius 0 0 black)))
          (.setLayoutParams tile-view layout-params)
          tile-view))))
  (.setOnItemClickListener
    view
    (proxy [android.widget.AdapterView$OnItemClickListener] []
      (onItemClick [parent v position id]
        (do-tile-action context (get content position)))))
  (.notifyDataSetChanged (.getAdapter view)))

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
     (if (> (count content) 0)
       (set-grid-view-tiles context content view))
     view)))

(defn get-new-post-view
  [context content]
  (let [view (make-ui context [:linear-layout {:orientation 1}
                               [:edit-text {:min-lines 10}]])
        text-view (.getChildAt view 0)]
    (set-text-size text-view default-text-size)
    (clear-attachments context)
    view))

(defn get-post-view
  [context content]
  (let [view (make-ui context [:scroll-view {}
                               [:linear-layout {:orientation 1}
                                [:text-view {:layout-width :fill
                                             :text-is-selectable true}]]])
        linear-layout (.getChildAt view 0)
        text-body (.getChildAt linear-layout 0)]
    (.setPadding linear-layout 10 10 10 10)
    (set-text-size text-body default-text-size)
    (future
      (let [post (if (get content :body)
                   content
                   (run-query get-single-post-data content))]
        (on-ui (.setText text-body (get post :body)))))
    view))

(defn get-file-view
  [context content]
  (let [view (make-ui context [:linear-layout {}])]
    (.addView view (get-grid-view context content))
    view))

(defn get-search-view
  [context content]
  (let [view (make-ui context [:linear-layout {}])]
    view))

(defn get-profile-view
  [context content]
  (let [bold android.graphics.Typeface/DEFAULT_BOLD
        view (if (get content :is-me?)
               (make-ui context [:scroll-view {}
                                 [:linear-layout {:orientation 1}
                                  [:edit-text {:lines 1
                                               :layout-width :fill}]
                                  [:edit-text {:layout-width :fill}]]])
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
    (.setHint text-name (get-string :name))
    (.setHint text-body (get-string :about_me))
    (.setText text-name (get content :title))
    (.setText text-body (get content :body))
    (.setLayoutParams image-view layout-params)
    (.setBackgroundResource image-view
                            (get-resource :drawable :border))
    (.setScaleType image-view android.widget.ImageView$ScaleType/CENTER_CROP)
    (.setImageBitmap image-view (read-prev-file (get content :userhash)
                                                 (get content :prevhash)))
    (if (get content :is-me?)
      (.setOnClickListener image-view
                           (proxy [android.view.View$OnClickListener] []
                             (onClick [v]
                               (request-files context
                                              "image/*"
                                              #(set-image-uri context v %1))))))
    (.addView linear-layout image-view)
    view))

(defn get-user-view
  [context content]
  (let [grid-view (get-grid-view context [])]
    (future
      (let [user (run-query get-user-data content)
            first-tiles [{:title (get-string :profile)
                          :add-emphasis? true
                          :content user
                          :userhash (get user :userhash)
                          :prevhash (get user :prevhash)
                          :type :custom-func
                          :func
                          (fn [context item]
                            (if (is-connecting?)
                              (show-dialog context (get-string :connecting))
                              (show-dialog
                                context
                                (get-profile-view context user)
                                (if (get user :is-me?)
                                  {:positive-name (get-string :save)
                                   :positive-func do-save-profile
                                   :negative-name (get-string :cancel)
                                   :negative-func do-cancel}
                                  {:positive-name (get-string :ok)
                                   :positive-func do-cancel}))))}
                         {:title (get-string :favorites)
                          :add-emphasis? true
                          :content user
                          :type :fav}
                         (if (get user :is-me?)
                           {:title (get-string :transfers)
                            :add-emphasis? true
                            :content user
                            :type :tran}
                           {:title (get-string :add_to_favorites)
                            :add-emphasis? true
                            :type :add-to-fav})]
            posts (run-query get-post-data content)
            grid-content (into [] (concat first-tiles posts))]
        (on-ui (set-grid-view-tiles context grid-content grid-view))))
    grid-view))

(defn get-category-view
  ([context content] (get-category-view context content false))
  ([context content show-tags?]
   (let [grid-view (get-grid-view context [])]
     (future
       (let [results (run-query get-category-data content)
             tags (if show-tags?
                    [{:title (get-string :tags)
                      :add-emphasis? true
                      :type :tag}]
                    [])
             grid-content (into [] (concat tags results))]
         (on-ui (set-grid-view-tiles context grid-content grid-view))))
     grid-view)))

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
