(ns net.nightweb.views
  (:use [neko.context :only [context]]
        [neko.ui :only [make-ui-element]]
        [neko.resource :only [get-resource]]
        [neko.ui.mapping :only [set-classname!]]))

(set-classname! :grid-view android.widget.GridView)

(defn make-ui-runtime
  "A non-macro version of neko's make-ui."
  ([tree]
     (eval (make-ui-element tree {} `context)))
  ([custom-context tree]
     (eval (make-ui-element tree {} custom-context))))

(defn get-grid-view
  [content]
  [:grid-view {:num-columns android.widget.GridView/AUTO_FIT
               :stretch-mode android.widget.GridView/STRETCH_COLUMN_WIDTH}])

(defn get-post-grid-view
  [content]
  [:linear-layout {}
   [:text-view {:text "post-grid"}]])

(defn get-new-post-view
  [content]
  [:linear-layout {:orientation android.widget.LinearLayout/VERTICAL}
   [:text-view {:text "new-post"}]])

(defn get-preview-view
  [content]
  [:linear-layout {:orientation android.widget.LinearLayout/VERTICAL}
   [:text-view {:text "preview"}]])

(defn get-post-view
  [content]
  [:linear-layout {:orientation android.widget.LinearLayout/VERTICAL}
   [:text-view {:text "post"}]
   (get-grid-view content)])

(defn get-file-view
  [content]
  [:linear-layout {:orientation android.widget.LinearLayout/VERTICAL}
   [:text-view {:text "file"}]
   (get-post-grid-view content)])

(defn get-view
  [view-type content]
  (case view-type
    :grid (get-grid-view content)
    :post-grid (get-post-grid-view content)
    :new-post (get-new-post-view content)
    :preview (get-preview-view content)
    :post (get-post-view content)
    :file (get-file-view content)))

(defn create-view
  [context sub-view-type sub-view-content]
  (let [res (.getResources context)
        config (.getConfiguration res)
        orientation (if (= (. config orientation) android.content.res.Configuration/ORIENTATION_PORTRAIT)
                      android.widget.LinearLayout/VERTICAL
                      android.widget.LinearLayout/HORIZONTAL)]
    (make-ui-runtime
      [:linear-layout {:orientation orientation
                       :layout-width :fill
                       :layout-height :fill}
       (get-view sub-view-type sub-view-content)])))

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
