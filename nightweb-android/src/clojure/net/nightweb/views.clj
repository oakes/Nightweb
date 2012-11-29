(ns net.nightweb.views
  (:use [neko.ui :only [make-ui]]
        [neko.resource :only [get-resource]]
        [neko.ui.mapping :only [set-classname!]]))

(set-classname! :grid-view android.widget.GridView)

(defn get-grid-view
  [context content]
  (let [view (make-ui context
                      [:grid-view {:horizontal-spacing 0
                                   :vertical-spacing 0}])
        point (android.graphics.Point.)
        display (.getDefaultDisplay (.getWindowManager context))
        _ (.getSize display point)
        parent-width (.x point)
        density (.density (.getDisplayMetrics (.getResources context)))
        tile-view-min (* density 160)
        num-columns (int (/ parent-width tile-view-min))]
    (.setNumColumns view num-columns)
    (.setAdapter view
                 (proxy [android.widget.BaseAdapter] []
                   (getItem [position] nil)
                   (getItemId [position] 0)
                   (getCount [] (count content))
                   (getView [position convert-view parent]
                     (let [center android.view.Gravity/CENTER
                           bottom android.view.Gravity/BOTTOM
                           black android.graphics.Color/BLACK
                           background (get-resource :drawable :border)
                           item (get content position)
                           tile-view
                           (if (contains? item :is-split)
                             (make-ui context
                                      [:linear-layout {:orientation 1}
                                       [:button {:layout-weight 1
                                                 :layout-width :fill
                                                 :gravity center}]
                                       [:button {:layout-weight 1
                                                 :layout-width :fill
                                                 :gravity center}]])
                             (make-ui context
                                      [:linear-layout {:orientation 1}
                                       [:text-view {:layout-weight 3}]
                                       [:text-view {:layout-weight 1
                                                    :gravity bottom}]]))
                           tile-view-width (if (> num-columns 0)
                                             (int (/ parent-width num-columns))
                                             tile-view-min)
                           params (android.widget.AbsListView$LayoutParams.
                                                              tile-view-width
                                                              tile-view-width)
                           subview1 (.getChildAt tile-view 0)
                           subview2 (.getChildAt tile-view 1)]
                       (.setPadding tile-view 5 5 5 5)
                       (.setBackgroundResource tile-view background)
                       (.setLayoutParams tile-view params)
                       (.setText subview1 (get-in content [position :title1]))
                       (.setText subview2 (get-in content [position :title2]))
                       (.setShadowLayer subview1 10 0 0 black)
                       (.setShadowLayer subview2 10 0 0 black)
                       tile-view))))
    view))

(defn get-new-post-view
  [context content]
  (let [view (make-ui context [:linear-layout {}])]
    view))

(defn get-post-view
  [context content]
  (let [view (make-ui context [:linear-layout {}])]
    (.addView view (get-grid-view content))
    view))

(defn get-file-view
  [context content]
  (let [view (make-ui context [:linear-layout {}])]
    (.addView view (get-grid-view content))
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
