(ns net.nightweb.views
  (:use [neko.ui :only [make-ui]]
        [neko.resource :only [get-resource]]))

(defn get-profile-view
  []
  (make-ui
    [:linear-layout {}
     [:text-view {:text "profile"}]]))

(defn get-new-post-view
  []
  (make-ui
    [:linear-layout {}
     [:text-view {:text "new-post"}]]))

(defn get-post-view
  []
  (make-ui
    [:linear-layout {}
     [:text-view {:text "post"}]]))

(defn get-preview-view
  []
  (make-ui
    [:linear-layout {}
     [:text-view {:text "preview"}]]))

(defn get-grid-view
  []
  (make-ui
    [:linear-layout {}
     [:text-view {:text "grid"}]]))

(defn get-post-grid-view
  []
  (make-ui
    [:linear-layout {}
     [:text-view {:text "post-grid"}]]))

(defn get-file-view
  []
  (make-ui
    [:linear-layout {}
     [:text-view {:text "file"}]]))

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