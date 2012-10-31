(ns net.nightweb.views
  (:use [neko.ui :only [make-ui]]
        [neko.resource :only [get-resource]]))

(def grid-view
  [:linear-layout {}
   [:text-view {:text "grid"}]])

(def post-grid-view
  [:linear-layout {}
   [:text-view {:text "post-grid"}]])

(defn get-view
  [keyword]
  (case keyword
    :grid
    (make-ui grid-view)
    :post-grid
    (make-ui post-grid-view)
    :new-post
    (make-ui
      [:linear-layout {}
       [:text-view {:text "new-post"}]])
    :preview
    (make-ui
      [:linear-layout {}
       [:text-view {:text "preview"}]])
    :profile
    (make-ui
      [:linear-layout {}
       [:text-view {:text "profile"}]
       grid-view])
    :post
    (make-ui
      [:linear-layout {}
       [:text-view {:text "post"}]
       grid-view])
    :file
    (make-ui
      [:linear-layout {}
       [:text-view {:text "file"}]
       post-grid-view])))

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