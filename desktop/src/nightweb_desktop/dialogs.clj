(ns nightweb-desktop.dialogs
  (:use [nightweb.formats :only [base32-encode
                                 url-encode]]
        [nightweb.constants :only [is-me?]]
        [nightweb-desktop.utils :only [get-string
                                       get-pic
                                       pic-to-data-url]]))

(defn get-my-profile-dialog
  [user]
  [:div {:id "profile-dialog" :class "reveal-modal dark"}
   [:br]
   [:form {:id "profile-image"
           :class "square-image"
           :style (str "background-image: url(" (get-pic (:pichash user)) ")")}
    [:input {:type "button"
             :id "profile-clear"
             :value (get-string :clear)
             :onclick "clearProfilePic()"}]
    [:input {:type "file"
             :id "profile-picker"
             :size "1"
             :onchange "profilePicker(this)"}]]
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
         :onclick "$('#import-dialog').foundation('reveal', 'open')"}
     (get-string :import_start)]
    [:a {:href "#"
         :class "button"
         :onclick "$('#export-dialog').foundation('reveal', 'open')"}
     (get-string :export_start)]
    [:a {:href "#" :class "button" :onclick "saveProfile()"}
     (get-string :save)]]
   [:a {:class "close-reveal-modal"} "&#215;"]])

(defn get-their-profile-dialog
  [user]
  [:div {:id "profile-dialog" :class "reveal-modal dark"}
   [:br]
   [:div {:id "profile-image"
          :class "square-image"
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
  [params]
  [:div {:id "search-dialog" :class "reveal-modal dark"}
   [:input {:type "text" :id "search-text"}]
   [:div {:class "dialog-buttons"}
    [:a {:href "#" :class "button" :onclick "openSearch()"} (get-string :go)]]
   [:a {:class "close-reveal-modal"} "&#215;"]])

(defn get-new-post-dialog
  [params]
  (let [ptr-hash (when (and (:userhash params)
                            (or (= :post (:type params))
                                (-> (:userhash params)
                                    (is-me?)
                                    (not))))
                   (base32-encode (:userhash params)))
        ptr-time (when (= :post (:type params))
                       (:time params))]
    [:form {:id "new-post-dialog"
            :class "reveal-modal dark"
            :action "/"
            :method "POST"
            :enctype "multipart/form-data"}
     [:input {:type "hidden" :id "new-post-ptr-hash" :value ptr-hash}]
     [:input {:type "hidden" :id "new-post-ptr-time" :value ptr-time}]
     [:br]
     [:textarea {:id "new-post-body"}]
     [:div {:class "dialog-buttons"}
      [:span {:id "attach-count"}]
      [:input {:type "file"
               :id "attach-picker"
               :size "1"
               :multiple "multiple"
               :onchange "attachPicker(this)"}]
      [:a {:href "#" :class "button" :onclick "clearPost()"}
       (get-string :clear)]
      [:a {:href "#" :class "button" :onclick "newPost()"}
       (if ptr-time (get-string :send_reply) (get-string :send))]]
     [:a {:class "close-reveal-modal"} "&#215;"]]))

(defn get-link-dialog
  [params]
  [:div {:id "link-dialog" :class "reveal-modal dark"}
   [:input {:type "text" :id "link-text" :value (url-encode params)}]
   [:div {:class "dialog-buttons"}
    [:a {:href "#" :class "button" :onclick "openLink()"} (get-string :go)]]
   [:a {:class "close-reveal-modal"} "&#215;"]])

(defn get-import-dialog
  [params]
  [:div {:id "import-dialog" :class "reveal-modal dark"}
   [:div {:class "dialog-element"} (get-string :import_desc)]
   [:input {:id "import-picker"
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
  [params]
  [:div {:id "export-dialog" :class "reveal-modal dark"}
   [:div {:class "dialog-element"} (get-string :export_desc)]
   [:input {:id "export-password"
            :class "dialog-element"
            :type "password"
            :placeholder (get-string :password)}]
   [:div {:class "dialog-buttons"}
    [:a {:href "#" :class "button" :onclick "exportUser()"}
     (get-string :save)]]
   [:a {:class "close-reveal-modal"} "&#215;"]])
