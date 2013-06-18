(ns nightweb-desktop.dialogs
  (:require [nightweb.constants :as c]
            [nightweb.formats :as f]
            [nightweb.io :as io]
            [nightweb.db :as db]
            [nightweb-desktop.utils :as utils]))

(defn get-my-profile-dialog
  [user]
  [:div {:id "profile-dialog" :class "reveal-modal dark"}
   [:br]
   [:form {:id "profile-image"
           :class "square-image"
           :style (when-let [url (utils/get-pic (:pichash user))]
                    (str "background-image: url(" url ")"))}
    [:input {:type "button"
             :id "profile-clear"
             :value (utils/get-string :clear)
             :onclick "clearProfilePic()"}]
    [:input {:type "file"
             :id "profile-picker"
             :size "1"
             :onchange "profilePicker(this)"}]]
   [:div {:id "profile-inputs"}
    [:input {:id "profile-name"
             :placeholder (utils/get-string :name)
             :type "text"
             :value (:title user)}]
    [:textarea {:id "profile-about"
                :placeholder (utils/get-string :about_me)}
     (:body user)]
    [:input {:id "profile-image-hidden"
             :type "hidden"
             :value (utils/pic-to-data-uri (:pichash user))}]]
   [:div {:class "dialog-buttons"}
    [:a {:href "#"
         :class "button"
         :onclick "$('#import-dialog').foundation('reveal', 'open')"}
     (utils/get-string :import_start)]
    [:a {:href "#"
         :class "button"
         :onclick "$('#export-dialog').foundation('reveal', 'open')"}
     (utils/get-string :export_start)]
    [:a {:href "#" :class "button" :onclick "saveProfile()"}
     (utils/get-string :save)]]
   [:a {:class "close-reveal-modal"} "&#215;"]])

(defn get-their-profile-dialog
  [user]
  [:div {:id "profile-dialog" :class "reveal-modal dark"}
   [:br]
   [:div {:id "profile-image"
          :class "square-image"
          :style (when-let [pic (utils/get-pic (:userhash user)
                                               (:pichash user))]
                   (str "background-image: url(" pic ")"))}]
   [:div {:id "profile-inputs"}
    [:div {:id "profile-name"} (:title user)]
    [:div {:id "profile-about"} (:body user)]]
   [:div {:class "dialog-buttons"}
    [:a {:href "#"
         :class "button"
         :onclick "$('#profile-dialog').foundation('reveal', 'close')"}
     (utils/get-string :ok)]]
   [:a {:class "close-reveal-modal"} "&#215;"]])

(defn get-search-dialog
  [params]
  [:div {:id "search-dialog" :class "reveal-modal dark"}
   [:input {:type "text" :id "search-text"}]
   [:div {:class "dialog-buttons"}
    [:a {:href "#" :class "button" :onclick "openSearch()"}
     (utils/get-string :search)]]
   [:a {:class "close-reveal-modal"} "&#215;"]])

(defn get-new-post-dialog
  [params]
  (let [ptr-hash (when (and (:userhash params)
                            (or (= :post (:type params))
                                (-> (:userhash params)
                                    c/is-me?
                                    not)))
                   (f/base32-encode (:userhash params)))
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
       (utils/get-string :clear)]
      [:a {:href "#" :class "button" :onclick "newPost()"}
       (if ptr-time (utils/get-string :send_reply) (utils/get-string :send))]]
     [:a {:class "close-reveal-modal"} "&#215;"]]))

(defn get-edit-post-dialog
  [params]
  (let [post (db/get-single-post-data params)
        pics (db/get-pic-data post (:time post) false)
        pic-hashes (-> (for [pic pics]
                         (f/base32-encode (:pichash pic)))
                       vec
                       pr-str)
        ptr-hash (f/base32-encode (:ptrhash post))
        ptr-time (:ptrtime post)]
    [:form {:id "edit-dialog"
            :class "reveal-modal dark"
            :action "/"
            :method "POST"
            :enctype "multipart/form-data"}
     [:input {:type "hidden" :id "edit-post-ptr-hash" :value ptr-hash}]
     [:input {:type "hidden" :id "edit-post-ptr-time" :value ptr-time}]
     [:input {:type "hidden" :id "edit-post-time" :value (:time post)}]
     [:input {:type "hidden" :id "edit-post-pic-hashes" :value pic-hashes}]
     [:br]
     [:textarea {:id "edit-post-body"} (:body post)]
     [:div {:class "dialog-buttons"}
      [:a {:href "#"
           :class "button"
           :onclick (format "deletePost(\"%s\")" (utils/get-string :confirm_delete))}
       (utils/get-string :delete)]
      [:a {:href "#" :class "button" :onclick "editPost()"}
       (if ptr-time (utils/get-string :send_reply) (utils/get-string :send))]]
     [:a {:class "close-reveal-modal"} "&#215;"]]))

(defn get-link-dialog
  [params]
  [:div {:id "link-dialog" :class "reveal-modal dark"}
   [:input {:type "text"
            :id "link-text"
            :value (f/url-encode (if-not (:type params)
                                 (assoc params
                                        :type :user
                                        :userhash @c/my-hash-bytes)
                                 params))}]
   [:div {:class "dialog-buttons"}
    [:a {:href "#" :class "button" :onclick "openLink()"} (utils/get-string :go)]]
   [:a {:class "close-reveal-modal"} "&#215;"]])

(defn get-import-dialog
  [params]
  [:div {:id "import-dialog" :class "reveal-modal dark"}
   [:div {:class "dialog-element"} (utils/get-string :import_desc)]
   [:input {:id "import-picker"
            :class "dialog-element"
            :type "file"}]
   [:input {:id "import-password"
            :class "dialog-element"
            :type "password"
            :placeholder (utils/get-string :password)}]
   [:div {:class "dialog-buttons"}
    [:a {:href "#" :class "button" :onclick "importUser()"}
     (utils/get-string :import_user)]]
   [:a {:class "close-reveal-modal"} "&#215;"]])

(defn get-export-dialog
  [params]
  [:div {:id "export-dialog" :class "reveal-modal dark"}
   [:div {:class "dialog-element"} (utils/get-string :export_desc)]
   [:input {:id "export-password"
            :class "dialog-element"
            :type "password"
            :placeholder (utils/get-string :password)}]
   [:div {:class "dialog-buttons"}
    [:a {:href "#" :class "button" :onclick "exportUser()"}
     (utils/get-string :save)]]
   [:a {:class "close-reveal-modal"} "&#215;"]])

(defn get-switch-user-dialog
  [params]
  [:div {:id "switch-user-dialog" :class "reveal-modal dark"}
   (let [items (for [user-hash (io/read-user-list-file)]
                 (db/get-single-user-data {:userhash user-hash}))]
     (for [item items]
       [:div
        [:a {:href "#"
             :class (if (c/is-me? (:userhash item)) "button disabled" "button")
             :onclick (when-not (c/is-me? (:userhash item))
                        (format "switchUser('%s')"
                                (f/base32-encode (:userhash item))))}
         (if (= 0 (count (:title item)))
           (utils/get-string :no_name)
           (:title item))]
        [:a {:href "#"
             :class "button"
             :onclick (format "deleteUser('%s', \"%s\")"
                              (f/base32-encode (:userhash item))
                              (utils/get-string :confirm_delete))}
         (utils/get-string :delete)]]))
   [:div {:class "dialog-buttons"}
    [:a {:href "#" :class "button" :onclick "createUser()"}
     (utils/get-string :create_user)]]
   [:a {:class "close-reveal-modal"} "&#215;"]])
