(ns net.nightweb.tabs
  (:use [neko.ui :only [make-ui]]
        [neko.resource :only [get-resource]]))

(defn create-tab
  [action-bar title]
  (let [tab (.newTab action-bar)
        fragment (proxy [android.app.Fragment] []
                   (onCreateView [layout-inflater viewgroup bundle]
                                 (make-ui [:linear-layout {}
                                           [:text-view {:text title}]])))
        listener (proxy [android.app.ActionBar$TabListener] []
                   (onTabSelected [tab ft]
                                  (.add ft (get-resource :id :android/content) fragment))
                   (onTabUnselected [tab ft]
                                    (.remove ft fragment))
                   (onTabReselected [tab ft]))]
    (.setText tab title)
    (.setTabListener tab listener)
    (.addTab action-bar tab)))