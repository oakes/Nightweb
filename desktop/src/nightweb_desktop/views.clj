(ns nightweb-desktop.views
  (:use [nightweb.formats :only [url-encode]]
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
  (for [button (concat [(when show-me-tab?
                          {:type nil :title "Me"})]
                       [{:type :user :title "Users"}
                        {:type :post :title "Posts"}])]
    [:li {:class (when (= (:type params) (:type button)) "active")}
     [:a {:href (url-encode button (if show-me-tab? "/?" "/c?"))}
      (:title button)]]))

(defn get-menu-view
  []
  (for [button [{:class "foundicon-search"
                 :title "Search"
                 :dialog "search-dialog"}
                {:class "foundicon-plus"
                 :title "New Post"
                 :dialog "new-post-dialog"}
                {:class "foundicon-page"
                 :title "Link"
                 :dialog "link-dialog"}]]
    [:li [:a {:href "#"
              :onclick (str "$('#"
                            (:dialog button)
                            "').foundation('reveal', 'open');")}
          [:i {:class (:class button)}]]]))

(defn get-action-bar-view
  [tab-view]
  [:div {:class "sticky"}
   [:nav {:class "top-bar"}
    [:section {:class "top-bar-section"}
     (when tab-view
       [:ul {:class "left"} tab-view])
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
          add-emphasis? (:add-emphasis? item)]
      [:a {:href "#"
           :onclick (str "tileAction('" (url-encode item "") "')")
           :class "grid-view-tile square-image"
           :style (format "background: url(%s);
                           text-align: %s;"
                          background
                          (if add-emphasis? "center" "left"))}
       (if add-emphasis? [:strong title] [:div title])])))

(defn get-post-view
  [params]
  (let [post (get-single-post-data params)
        tiles (get-post-tiles post)]
    (get-grid-view tiles)))

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
