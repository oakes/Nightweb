(ns net.nightweb.views
  (:use [neko.context :only [context]]
        [neko.ui :only [make-ui-element]]
        [neko.resource :only [get-resource]]))

(defn make-ui-runtime
  "A non-macro version of neko's make-ui."
  ([tree]
     (eval (make-ui-element tree {} `context)))
  ([custom-context tree]
     (eval (make-ui-element tree {} custom-context))))

(def grid-view
  [:linear-layout {}
   [:text-view {:text "grid"}]])

(def post-grid-view
  [:linear-layout {}
   [:text-view {:text "post-grid"}]])

(def new-post-view
  [:linear-layout {:orientation android.widget.LinearLayout/VERTICAL}
   [:text-view {:text "new-post"}]])

(def preview-view
  [:linear-layout {:orientation android.widget.LinearLayout/VERTICAL}
   [:text-view {:text "preview"}]])

(def profile-view
  [:linear-layout {:orientation android.widget.LinearLayout/VERTICAL}
   [:text-view {:text "profile"}]
   grid-view])

(def post-view
  [:linear-layout {:orientation android.widget.LinearLayout/VERTICAL}
   [:text-view {:text "post"}]
   grid-view])

(def file-view
  [:linear-layout {:orientation android.widget.LinearLayout/VERTICAL}
   [:text-view {:text "file"}]
   post-grid-view])

(defn get-view
  [view-type]
  (case view-type
    :grid grid-view
    :post-grid post-grid-view
    :new-post new-post-view
    :preview preview-view
    :profile profile-view
    :post post-view
    :file file-view))

(defn create-view
  [context sub-view-type]
  (let [res (.getResources context)
        config (.getConfiguration res)
        orientation (if (= (. config orientation) android.content.res.Configuration/ORIENTATION_PORTRAIT)
                      android.widget.LinearLayout/VERTICAL
                      android.widget.LinearLayout/HORIZONTAL)]
    (make-ui-runtime
      [:linear-layout {:orientation orientation}
       (get-view sub-view-type)])))

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
