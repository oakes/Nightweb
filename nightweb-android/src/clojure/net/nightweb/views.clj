(ns net.nightweb.views
  (:use [neko.ui :only [make-ui]]
        [neko.resource :only [get-string get-resource]]
        [neko.ui.mapping :only [set-classname!]]
        [net.nightweb.actions :only [do-save-profile
                                     do-cancel
                                     show-dialog
                                     show-favorites
                                     show-downloads
                                     show-grid]]
        [nightweb.db :only [run-query
                            get-user-data
                            get-category-data]]))

(set-classname! :scroll-view android.widget.ScrollView)

(defn get-profile-view
  [context content]
  (let [bold android.graphics.Typeface/DEFAULT_BOLD
        view (if (get content :is-me)
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
    (if-let [name-str (get content :title)]
      (.setText text-name name-str))
    (if-let [about-str (get content :about)]
      (.setText text-about about-str))
    (.setLayoutParams image-view layout-params)
    (.setBackgroundResource image-view border)
    (.addView linear-layout image-view)
    view))

(defn get-grid-view
  ([context content] (get-grid-view context content false 160))
  ([context content make-height-fit-content? min-width]
   (let [density (.density (.getDisplayMetrics (.getResources context)))
         tile-view-min (int (* density min-width))
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
     (.setAdapter
       view
       (proxy [android.widget.BaseAdapter] []
         (getItem [position] nil)
         (getItemId [position] 0)
         (getCount [] (count content))
         (getView [position convert-view parent]
           (let [not-initialized (= convert-view nil)
                 tile-view (if not-initialized
                             (let [bottom android.view.Gravity/BOTTOM]
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
                 (.setText text-top (get-in content [position :title]))
                 (.setText text-bottom (get-in content [position :subtitle]))
                 (.setShadowLayer text-top 10 0 0 black)
                 (.setShadowLayer text-bottom 10 0 0 black)))
             tile-view))))
     (.setOnItemClickListener
       view
       (proxy [android.widget.AdapterView$OnItemClickListener] []
         (onItemClick [parent v position id]
           (let [item (get content position)
                 data-type (get item :type)
                 func (case data-type
                        :tags show-grid
                        :users show-grid
                        :profile (fn [context content]
                                   (show-dialog
                                     context
                                     (get-profile-view context
                                                       (get item :content))
                                     {:positive-name (get-string :save)
                                      :positive-func do-save-profile
                                      :negative-name (get-string :cancel)
                                      :negative-func do-cancel}))
                        :favorites show-favorites
                        :downloads show-downloads
                        (fn [context content]))]
             (func context item)))))
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

(defn get-user-view
  [context content]
  (run-query
    get-user-data
    content
    (fn [row]
      (get-grid-view context
                     [{:title (get-string :profile)
                       :content row
                       :type :profile}
                      {:title (get-string :favorites)
                       :content row
                       :type :favorites}
                      {:title (get-string :downloads)
                       :content row
                       :type :downloads}]))))

(defn get-category-view
  ([context content] (get-category-view context content false))
  ([context content show-tags?]
   (let [results (run-query
                   get-category-data
                   content
                   (fn [rows] rows))
         tags (if show-tags?
                [{:title (get-string :tags)
                  :type :tags}]
                [])
         grid-content (into [] (concat tags results))]
     (get-grid-view context grid-content))))

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
