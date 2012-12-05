(ns net.nightweb.views
  (:use [neko.ui :only [make-ui]]
        [neko.resource :only [get-string get-resource]]
        [neko.ui.mapping :only [set-classname!]]))

(do
  (gen-class
    :name "net.nightweb.views.ExpandableGridView"
    :extends android.widget.GridView
    :state "state"
    :init "init"
    :prefix "expandable-grid-view-"
    :constructors {[android.content.Context] [android.content.Context]
                   [android.content.Context android.util.AttributeSet]
                   [android.content.Context android.util.AttributeSet]
                   [android.content.Context android.util.AttributeSet int]
                   [android.content.Context android.util.AttributeSet int]}
    :methods [[setExpandable [boolean] void]]
    :exposes-methods {onMeasure superOnMeasure})
  (defn expandable-grid-view-create-state
    [context]
    (atom {:context context
           :expandable false}))
  (defn expandable-grid-view-init
    ([context]
     [[context] (expandable-grid-view-create-state context)])
    ([context attrs]
     [[context attrs] (expandable-grid-view-create-state context)])
    ([context attrs def-style]
     [[context attrs def-style] (expandable-grid-view-create-state context)]))
  (defn expandable-grid-view-onMeasure
    [this width height]
    (if (get @(.state this) :expandable)
      (let [params (.getLayoutParams this)
            at-most android.view.View$MeasureSpec/AT_MOST
            spec (android.view.View$MeasureSpec/makeMeasureSpec
                                    (bit-shift-right
                                      java.lang.Integer/MAX_VALUE 2)
                                    at-most)]
        (.superOnMeasure this width spec)
        (set! (. params height) (.getMeasuredHeight this)))
      (.superOnMeasure this width height)))
  (defn expandable-grid-view-setExpandable
    [this is-expandable]
    (swap! (.state this) assoc-in [:expandable] is-expandable)))

(set-classname! :grid-view net.nightweb.views.ExpandableGridView)
(set-classname! :scroll-view android.widget.ScrollView)

(defn get-screen-width
  [context]
  (let [point (android.graphics.Point.)
        display (.getDefaultDisplay (.getWindowManager context))
        _ (.getSize display point)]
    (.x point)))

(defn get-grid-view
  [context content]
  (let [view (make-ui context
                      [:grid-view {:horizontal-spacing 0
                                   :vertical-spacing 0}])
        screen-width (get-screen-width context)
        density (.density (.getDisplayMetrics (.getResources context)))
        tile-view-min (* density 160)
        num-columns (int (/ screen-width tile-view-min))]
    (.setNumColumns view num-columns)
    (.setAdapter
      view
      (proxy [android.widget.BaseAdapter] []
        (getItem [position] nil)
        (getItemId [position] 0)
        (getCount [] (count content))
        (getView [position convert-view parent]
          (if convert-view
            convert-view
            (let [bottom android.view.Gravity/BOTTOM
                  black android.graphics.Color/BLACK
                  background (get-resource :drawable :border)
                  item (get content position)
                  tile-view (make-ui context [:linear-layout {:orientation 1}
                                              [:text-view {:layout-weight 3}]
                                              [:text-view {:layout-weight 1
                                                           :gravity bottom}]])
                  tile-view-width (if (> num-columns 0)
                                    (int (/ screen-width num-columns))
                                    tile-view-min)
                  params (android.widget.AbsListView$LayoutParams.
                                                     tile-view-width
                                                     tile-view-width)
                  subview1 (.getChildAt tile-view 0)
                  subview2 (.getChildAt tile-view 1)]
              (.setPadding tile-view 5 5 5 5)
              (.setBackgroundResource tile-view background)
              (.setLayoutParams tile-view params)
              (.setText subview1 (get-in content [position :title]))
              (.setText subview2 (get-in content [position :subtitle]))
              (.setShadowLayer subview1 10 0 0 black)
              (.setShadowLayer subview2 10 0 0 black)
              tile-view)))))
    (.setOnItemClickListener
      view
      (proxy [android.widget.AdapterView$OnItemClickListener] []
        (onItemClick [parent v position id]
          (let [item (get content position)]
            (println "click")))))
    view))

(defn get-new-post-view
  [context content]
  (let [view (make-ui context [:scroll-view {}
                               [:linear-layout {:orientation 1}
                                [:edit-text {:min-lines 10}]]])
        subview (.getChildAt view 0)
        grid-view (get-grid-view context content)]
    (.setExpandable grid-view true)
    (.addView subview grid-view)
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
  (let [view (make-ui context [:linear-layout {}
                               [:edit-text {:lines 1}]
                               [:edit-text {}]])
        subview1 (.getChildAt view 0)
        subview2 (.getChildAt view 1)]
    (.setHint subview1 (get-string :name))
    (.setHint subview2 (get-string :about_me))
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
