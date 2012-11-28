(ns net.nightweb.views
  (:use [neko.context :only [context]]
        [neko.ui :only [make-ui]]
        [neko.resource :only [get-resource]]
        [neko.ui.mapping :only [set-classname!]]))

(set-classname! :grid-view android.widget.GridView)

(defn get-grid-view
  [content]
  (let [view (make-ui [:grid-view {:horizontal-spacing 0
                                   :vertical-spacing 0}])]
    (.setAdapter view
                 (proxy [android.widget.BaseAdapter] []
                   (getItem [position] nil)
                   (getItemId [position] 0)
                   (getCount [] (count content))
                   (getView [position convert-view parent]
                     (let [bottom android.view.Gravity/BOTTOM
                           black android.graphics.Color/BLACK
                           background (get-resource :drawable :border)
                           tile-view
                           (make-ui [:linear-layout {:orientation 1}
                                     [:text-view {:layout-weight 3}]
                                     [:text-view {:layout-weight 1
                                                  :gravity bottom}]])
                           tile-view-min 200
                           tile-view-padding 5
                           parent-width (.getWidth parent)
                           num-columns (int (/ parent-width tile-view-min))
                           tile-view-width (if (> num-columns 0)
                                             (int (/ parent-width num-columns))
                                             tile-view-min)
                           params (android.widget.AbsListView$LayoutParams.
                                                              tile-view-width
                                                              tile-view-width)
                           title (.getChildAt tile-view 0)
                           author (.getChildAt tile-view 1)]
                       (.setNumColumns view num-columns)
                       (.setPadding tile-view
                                    tile-view-padding tile-view-padding
                                    tile-view-padding tile-view-padding)
                       (.setBackgroundResource tile-view background)
                       (.setLayoutParams tile-view params)
                       (.setText title (get-in content [position :title]))
                       (.setText author (get-in content [position :author]))
                       (.setShadowLayer title 10 0 0 black)
                       (.setShadowLayer author 10 0 0 black)
                       tile-view))))
    view))

(defn get-new-post-view
  [content]
  (let [view (make-ui [:linear-layout {}])]
    view))

(defn get-post-view
  [content]
  (let [view (make-ui [:linear-layout {}])]
    (.addView view (get-grid-view content))
    view))

(defn create-view
  [view-type content]
  (case view-type
    :grid (get-grid-view content)
    :new-post (get-new-post-view content)
    :post (get-post-view content)))

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
