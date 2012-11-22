(ns net.nightweb.views
  (:use [neko.context :only [context]]
        [neko.ui :only [make-ui]]
        [neko.resource :only [get-resource]]
        [neko.ui.mapping :only [set-classname!]]))

(defn get-grid-view
  [content]
  (let [view (android.widget.GridView. context)]
    (.setNumColumns view android.widget.GridView/AUTO_FIT)
    (.setStretchMode view android.widget.GridView/STRETCH_COLUMN_WIDTH)
    (.setAdapter view
                 (proxy [android.widget.BaseAdapter] []
                   (getItem [position] nil)
                   (getItemId [position] 0)
                   (getCount [] 1)
                   (getView [position convert-view parent]
                     (let [image-view (android.widget.ImageView. context)]
                       (.setBackgroundColor image-view android.graphics.Color/BLUE)
                       (.setLayoutParams image-view (android.widget.AbsListView$LayoutParams. 200 200))
                       image-view))))
    view))

(defn get-post-grid-view
  [content]
  (let [view (android.widget.LinearLayout. context)]
    view))

(defn get-new-post-view
  [content]
  (let [view (android.widget.LinearLayout. context)]
    view))

(defn get-preview-view
  [content]
  (let [view (android.widget.LinearLayout. context)]
    view))

(defn get-post-view
  [content]
  (let [view (android.widget.LinearLayout. context)]
    (.addView view (get-grid-view content))
    view))

(defn get-file-view
  [content]
  (let [view (android.widget.LinearLayout. context)]
    (.addView view (get-post-grid-view content))
    view))

(defn create-view
  [view-type content]
  (case view-type
    :grid (get-grid-view content)
    :post-grid (get-post-grid-view content)
    :new-post (get-new-post-view content)
    :preview (get-preview-view content)
    :post (get-post-view content)
    :file (get-file-view content)))

(defn create-tab
  [action-bar title first-view]
  (let [tab (.newTab action-bar)
        fragment (proxy [android.app.Fragment] []
                   (onCreateView [layout-inflater viewgroup bundle]
                                 first-view))
        listener (proxy [android.app.ActionBar$TabListener] []
                   (onTabSelected [tab ft]
                                  (.add ft (get-resource :id :android/content) fragment))
                   (onTabUnselected [tab ft]
                                    (.remove ft fragment))
                   (onTabReselected [tab ft]))]
    (.setText tab title)
    (.setTabListener tab listener)
    (.addTab action-bar tab)))
