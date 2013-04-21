(ns nightweb-desktop.dialogs
  (:use [nightweb-desktop.utils :only [get-string]]))

(defn get-my-profile-dialog
  [user]
  [:div {:id "profile-dialog" :class "reveal-modal dark"}
   [:form
    [:br]
    [:a {:href "#"}
     [:img {:class "profile-image"}]]
    [:div {:class "profile-text"}
     [:input {:type "text" :placeholder (get-string :name)}]
     [:textarea {:placeholder (get-string :about_me)}]]
    [:div {:class "profile-buttons"}
     [:a {:href "#"
          :class "button"
          :onclick "$('#profile-dialog').foundation('reveal', 'close')"}
      (get-string :cancel)]
     [:a {:href "#"
          :class "button"
          :onclick "$('#export-dialog').foundation('reveal', 'open')"}
      (get-string :export)]
     [:a {:href "#" :class "button"} (get-string :save)]]]
   [:a {:class "close-reveal-modal"} "&#215;"]])

(defn get-their-profile-dialog
  [user]
  [:div {:id "profile-dialog" :class "reveal-modal dark"}
   [:form
    [:br]
    [:img {:class "profile-image"}]
    [:div {:class "profile-text"}]
    [:div {:class "profile-buttons"}
     [:a {:href "#"
          :class "button"
          :onclick "$('#profile-dialog').foundation('reveal', 'close')"}
      (get-string :ok)]]]
   [:a {:class "close-reveal-modal"} "&#215;"]])

(defn get-search-dialog
  []
  [:div {:id "search-dialog" :class "reveal-modal dark"}
   [:a {:class "close-reveal-modal"} "&#215;"]])

(defn get-new-post-dialog
  []
  [:div {:id "new-post-dialog" :class "reveal-modal dark"}
   [:a {:class "close-reveal-modal"} "&#215;"]])

(defn get-link-dialog
  []
  [:div {:id "link-dialog" :class "reveal-modal dark"}
   [:a {:class "close-reveal-modal"} "&#215;"]])

(defn get-settings-dialog
  []
  [:div {:id "settings-dialog" :class "reveal-modal dark"}
   [:a {:class "close-reveal-modal"} "&#215;"]])

(defn get-export-dialog
  []
  [:div {:id "export-dialog" :class "reveal-modal dark"}
   [:a {:class "close-reveal-modal"} "&#215;"]])

(defn get-import-dialog
  []
  [:div {:id "import-dialog" :class "reveal-modal dark"}
   [:a {:class "close-reveal-modal"} "&#215;"]])
