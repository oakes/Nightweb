(ns nightweb_desktop.dialogs)

(defn get-profile-dialog
  []
  [:div {:id "profile-dialog" :class "reveal-modal"}
   [:h2 "Hello World!"]
   [:a {:class "close-reveal-modal"} "&#215;"]])
