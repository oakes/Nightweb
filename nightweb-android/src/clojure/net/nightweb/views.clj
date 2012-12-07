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
                  border (get-resource :drawable :border)
                  item (get content position)
                  tile-view (make-ui context [:linear-layout {:orientation 1}
                                              [:text-view {:layout-weight 3}]
                                              [:text-view {:layout-weight 1
                                                           :gravity bottom}]])
                  tile-view-width (if (> num-columns 0)
                                    (int (/ screen-width num-columns))
                                    tile-view-min)
                  layout-params (android.widget.AbsListView$LayoutParams.
                                                            tile-view-width
                                                            tile-view-width)
                  text-top (.getChildAt tile-view 0)
                  text-bottom (.getChildAt tile-view 1)]
              (.setPadding tile-view 5 5 5 5)
              (.setBackgroundResource tile-view border)
              (.setLayoutParams tile-view layout-params)
              (.setText text-top (get-in content [position :title]))
              (.setText text-bottom (get-in content [position :subtitle]))
              (.setShadowLayer text-top 10 0 0 black)
              (.setShadowLayer text-bottom 10 0 0 black)
              tile-view)))))
    (.setOnItemClickListener
      view
      (proxy [android.widget.AdapterView$OnItemClickListener] []
        (onItemClick [parent v position id]
          (let [item (get content position)]
            (if-let [func (get item :func)]
              (func context item))))))
    view))

(defn get-new-post-view
  [context content]
  (let [view (make-ui context [:scroll-view {}
                               [:linear-layout {:orientation 1}
                                [:edit-text {:min-lines 10}]]])
        linear-layout (.getChildAt view 0)
        grid-view (get-grid-view context content)]
    (.setExpandable grid-view true)
    (.addView linear-layout grid-view)
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
