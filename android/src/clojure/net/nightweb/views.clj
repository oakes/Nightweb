(ns net.nightweb.views
  (:use [neko.ui :only [make-ui]]
        [neko.threading :only [on-ui]]
        [neko.resource :only [get-string get-resource]]
        [neko.ui.mapping :only [set-classname!]]
        [net.nightweb.actions :only [do-tile-action
                                     do-save-profile
                                     do-cancel
                                     show-dialog]]
        [nightweb.db :only [run-query
                            get-user-data
                            get-post-data
                            get-category-data]]))

(set-classname! :scroll-view android.widget.ScrollView)

(defn get-adjusted-tile-width
  ([context] (get-adjusted-tile-width context 160))
  ([context width]
   (int (* (.density (.getDisplayMetrics (.getResources context))) width))))

(defn set-grid-view-tiles
  [context content view]
  (let [tile-view-min (get-adjusted-tile-width context)]
    (.setAdapter
      view
      (proxy [android.widget.BaseAdapter] []
        (getItem [position] nil)
        (getItemId [position] 0)
        (getCount [] (count content))
        (getView [position convert-view parent]
          (let [not-initialized (= convert-view nil)
                bottom android.view.Gravity/BOTTOM
                bold android.graphics.Typeface/DEFAULT_BOLD
                tile-view (if not-initialized
                            (if (get-in content [position :add-emphasis?])
                              (make-ui context
                                       [:linear-layout {:orientation 1}
                                        [:text-view {:layout-weight 3
                                                     :typeface bold}]
                                        [:text-view {:layout-weight 1
                                                     :gravity bottom}]])
                              (make-ui context
                                       [:linear-layout {:orientation 1}
                                        [:text-view {:layout-weight 3}]
                                        [:text-view {:layout-weight 1
                                                     :gravity bottom}]]))
                            convert-view)
                num-columns (.getNumColumns view)
                width (.getWidth view)
                tile-view-width (if (and (> width 0) (> num-columns 0))
                                  (int (/ width num-columns))
                                  tile-view-min)
                layout-params (android.widget.AbsListView$LayoutParams.
                                                          tile-view-width
                                                          tile-view-width)]
            (.setLayoutParams tile-view layout-params)
            (if not-initialized
              (let [black android.graphics.Color/BLACK
                    border (get-resource :drawable :border)
                    item (get content position)
                    text-top (.getChildAt tile-view 0)
                    text-bottom (.getChildAt tile-view 1)]
                (.setPadding tile-view 5 5 5 5)
                (.setBackgroundResource tile-view border)
                (.setText text-top (get-in content [position :text]))
                (.setText text-bottom (get-in content [position :subtext]))
                (.setShadowLayer text-top 10 0 0 black)
                (.setShadowLayer text-bottom 10 0 0 black)))
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
   (let [tile-view-min (get-adjusted-tile-width context)
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
     view)))

(defn get-new-post-view
  [context content]
  (let [view (make-ui context [:edit-text {:min-lines 10}])]
    view))

(defn get-post-view
  [context content]
  (let [view (make-ui context [:linear-layout {}])]
    (.addView view (get-grid-view context content))
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
                                               :typeface bold}]
                                  [:text-view {:layout-width :fill}]]]))
        linear-layout (.getChildAt view 0)
        text-name (.getChildAt linear-layout 0)
        text-about (.getChildAt linear-layout 1)
        image-view (proxy [android.widget.ImageView] [context]
                     (onMeasure [width height]
                       (proxy-super onMeasure width width)))
        fill android.widget.LinearLayout$LayoutParams/FILL_PARENT
        layout-params (android.widget.LinearLayout$LayoutParams. fill 0)
        border (get-resource :drawable :border)]
    (.setPadding linear-layout 10 10 10 10)
    (.setHint text-name (get-string :name))
    (.setHint text-about (get-string :about_me))
    (if-let [name-str (get content :text)]
      (.setText text-name name-str))
    (if-let [about-str (get content :about)]
      (.setText text-about about-str))
    (.setLayoutParams image-view layout-params)
    (.setBackgroundResource image-view border)
    (.addView linear-layout image-view)
    view))

(defn get-user-view
  [context content]
  (let [grid-view (get-grid-view context [])]
    (future
      (let [user (run-query get-user-data content (fn [row] row))
            first-tiles [{:text (get-string :profile)
                          :add-emphasis? true
                          :content user
                          :type :custom-func
                          :func (fn [context content]
                                  (show-dialog
                                    context
                                    (get-profile-view context user)
                                    (if (get user :is-me?)
                                      {:positive-name (get-string :save)
                                       :positive-func do-save-profile
                                       :negative-name (get-string :cancel)
                                       :negative-func do-cancel}
                                      {:positive-name (get-string :ok)
                                       :positive-func do-cancel})))}
                         {:text (get-string :favorites)
                          :add-emphasis? true
                          :content user
                          :type :favorites}
                         (if (get user :is-me?)
                           {:text (get-string :transfers)
                            :add-emphasis? true
                            :content user
                            :type :transfers}
                           {:text (get-string :add_to_favorites)
                            :add-emphasis? true
                            :type :add-to-favorites})]
            posts (run-query get-post-data content (fn [rows] rows))
            grid-content (into [] (concat first-tiles posts))]
        (on-ui (set-grid-view-tiles context grid-content grid-view))))
    grid-view))

(defn get-category-view
  ([context content] (get-category-view context content false))
  ([context content show-tags?]
   (let [grid-view (get-grid-view context [])]
     (future
       (let [results (run-query get-category-data content (fn [rows] rows))
             tags (if show-tags?
                    [{:text (get-string :tags)
                      :add-emphasis? true
                      :type :tags}]
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
