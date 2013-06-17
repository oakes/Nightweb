(ns nightweb-desktop.views
  (:use [markdown.core :only [md-to-html-string]]
        [nightweb.formats :only [url-encode
                                 tags-encode]]
        [nightweb.db :only [get-single-post-data
                            get-single-user-data]]
        [nightweb.db_tiles :only [get-post-tiles
                                  get-user-tiles
                                  get-category-tiles]]
        [nightweb.constants :only [is-me?]]
        [nightweb-desktop.utils :only [get-string
                                       get-pic]]
        [nightweb-desktop.dialogs :only [get-my-profile-dialog
                                         get-their-profile-dialog]]))

(defn get-tab-view
  [params show-me-tab?]
  (for [button [{:type nil :title "Me"}
                {:type :user :title "Users"}
                {:type :post :title "Posts"}]]
    (when (or show-me-tab? (not= nil (:type button)))
      (let [active-tab (or (:subtype params) (:type params))]
        [:li {:class (when (= active-tab (:type button)) "active")}
         [:a {:href (url-encode (if (:subtype params)
                                  (assoc params :subtype (:type button))
                                  button)
                                (if show-me-tab? "/?" "/c?"))}
          (:title button)]]))))

(defn get-menu-view
  []
  (for [button [{:class "foundicon-search"
                 :title (get-string :search)
                 :dialog "search-dialog"}
                {:class "foundicon-plus"
                 :title (get-string :new_post)
                 :dialog "new-post-dialog"}
                {:class "foundicon-page"
                 :title (get-string :share)
                 :dialog "link-dialog"}
                {:class "foundicon-people"
                 :title (get-string :switch_user)
                 :dialog "switch-user-dialog"}]]
    [:li [:a {:href "#"
              :onclick (format "showDialog('%s')" (:dialog button))}
          [:i {:class (:class button)}]]]))

(defn get-action-bar-view
  [params & {:keys [is-main? show-tabs? show-home-button?]}]
  [:div {:class "sticky"}
   [:nav {:class "top-bar"}
    [:section {:class "top-bar-section"}
     [:ul {:class "left"}
      (when show-home-button?
        [:a {:href "/" :class "home-button"}])
      (when-let [title (or (:title params) (:tag params))]
        [:div {:class "title"} title])
      (when show-tabs? (get-tab-view params is-main?))]
     [:ul {:class "right"} (get-menu-view)]]]
   [:div {:class "clear"}]])

(defn get-grid-view
  [content]
  (for [item content]
    (let [background (or (get-pic (:userhash item) (:pichash item))
                         (when-let [bg (:background item)]
                           (str "img/" (name bg) ".png")))
          title (get-string (or (:title item)
                                (:body item)
                                (:tag item)))
          add-emphasis? (:add-emphasis? item)
          is-pic? (= :pic (:type item))]
      [(if is-pic? :li :div)
       [:a {:href (if is-pic? background "#")
            :onclick (str "doAction('" (url-encode item "") "')")
            :class "grid-view-tile square-image"
            :style (format "background-image: url(%s); text-align: %s;"
                           (if is-pic? "" background)
                           (if add-emphasis? "center" "left"))}
        (if is-pic? [:img {:src background
                           :style "width: 100%; height: 100%;"}])
        (if add-emphasis? [:strong title] [:div title])]])))

(defn get-post-view
  [params]
  (let [post (get-single-post-data params)
        tiles (get-post-tiles post)
        text (tags-encode :post (:body post))
        html-text (md-to-html-string text)
        html-tiles (get-grid-view tiles)]
    [:div {:id "post"}
     [:div {:class "post-body"} html-text]
     (filter #(= :div (get % 0)) html-tiles)
     [:ul {:data-clearing true}
      (filter #(= :li (get % 0)) html-tiles)]]))

(defn get-user-view
  [params]
  (let [user (get-single-user-data params)
        tiles (get-user-tiles params user)]
    [:div
     (get-grid-view tiles)
     (if (is-me? (:userhash user))
       (get-my-profile-dialog user)
       (get-their-profile-dialog user))]))

(defn get-category-view
  [params]
  (let [tiles (get-category-tiles params)]
    (get-grid-view tiles)))
