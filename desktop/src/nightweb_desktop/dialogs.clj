(ns nightweb-desktop.dialogs
  (:use [nightweb-desktop.utils :only [get-string
                                       get-pic
                                       pic-to-data-url]]))

(defn get-my-profile-dialog
  [user]
  [:div {:id "profile-dialog" :class "reveal-modal dark"}
   [:br]
   [:div {:id "profile-image"
          :style (str "background-image: url(" (get-pic (:pichash user)) ")")}
    [:input {:type "button"
             :id "profile-clear"
             :value (get-string :clear)}]
    [:input {:type "file"
             :id "profile-pick"
             :size "1"
             :onchange "profilePick(this)"}]]
   [:div {:id "profile-inputs"}
    [:input {:id "profile-name"
             :placeholder (get-string :name)
             :type "text"
             :value (:title user)}]
    [:textarea {:id "profile-about"
                :placeholder (get-string :about_me)}
     (:body user)]
    [:input {:id "profile-image-hidden"
             :type "hidden"
             :value (pic-to-data-url (:pichash user))}]]
   [:div {:class "dialog-buttons"}
    [:a {:href "#"
         :class "button"
         :onclick "$('#profile-dialog').foundation('reveal', 'close')"}
     (get-string :cancel)]
    [:a {:href "#"
         :class "button"
         :onclick "$('#import-dialog').foundation('reveal', 'open')"}
     (get-string :import_button)]
    [:a {:href "#"
         :class "button"
         :onclick "$('#export-dialog').foundation('reveal', 'open')"}
     (get-string :export_button)]
    [:a {:href "#" :class "button" :onclick "saveProfile()"}
     (get-string :save)]]
   [:a {:class "close-reveal-modal"} "&#215;"]])

(defn get-their-profile-dialog
  [user]
  [:div {:id "profile-dialog" :class "reveal-modal dark"}
   [:br]
   [:div {:class "profile-image"
          :style (when-let [pic (get-pic (:userhash user) (:pichash user))]
                   (str "background-image: url(" pic ")"))}]
   [:div {:id "profile-inputs"}
    [:div {:id "profile-name"} (:title user)]
    [:div {:id "profile-about"} (:body user)]]
   [:div {:class "dialog-buttons"}
    [:a {:href "#"
         :class "button"
         :onclick "$('#profile-dialog').foundation('reveal', 'close')"}
     (get-string :ok)]]
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

(defn get-import-dialog
  []
  [:div {:id "import-dialog" :class "reveal-modal dark"}
   [:div {:class "dialog-element"} (get-string :confirm_import)]
   [:input {:id "import-pick"
            :class "dialog-element"
            :type "file"}]
   [:input {:id "import-password"
            :class "dialog-element"
            :type "password"
            :placeholder (get-string :password)}]
   [:div {:class "dialog-buttons"}
    [:a {:href "#" :class "button" :onclick "importUser()"}
     (get-string :import_user)]]
   [:a {:class "close-reveal-modal"} "&#215;"]])

(defn get-export-dialog
  []
  [:div {:id "export-dialog" :class "reveal-modal dark"}
   [:a {:class "close-reveal-modal"} "&#215;"]])
