(ns net.nightweb.views
  (:use [neko.ui :only [make-ui]]
        [neko.resource :only [get-string get-resource]]
        [neko.ui.mapping :only [set-classname!]]))

(set-classname! :scroll-view android.widget.ScrollView)

(defn get-grid-view
  ([context content] (get-grid-view context content false 160))
  ([context content make-height-fit-content min-width]
   (let [density (.density (.getDisplayMetrics (.getResources context)))
         tile-view-min (int (* density min-width))
         view (proxy [android.widget.GridView] [context]
                (onMeasure [width-spec height-spec]
                  (let [w (android.view.View$MeasureSpec/getSize width-spec)
                        num-columns (int (/ w tile-view-min))]
                    (.setNumColumns this num-columns))
                  (if make-height-fit-content
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
           (let [item (get content position)]
             (if-let [func (get item :func)]
               (func context item))))))
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

(defn get-profile-view
  [context content]
  (let [view (make-ui context [:scroll-view {}
                               [:linear-layout {:orientation 1}
                                [:edit-text {:lines 1
                                             :layout-width :fill}]
                                [:edit-text {:layout-width :fill}]]])
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
    (.setLayoutParams image-view layout-params)
    (.setBackgroundResource image-view border)
    (.addView linear-layout image-view)
    view))

(defn get-search-view
  [context content]
  (let [view (make-ui context [:linear-layout {}])]
    view))

(defn create-tab
  [action-bar title first-view]
  (let [tab (.newTab action-bar)
        fragment (proxy [android.app.Fragment] []
                   (onCreateView [layout-inflater viewgroup bundle]
                                 first-view))
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
