(ns nightweb-desktop.views
  (:use [nightweb.formats :only [url-encode]]
        [nightweb.db :only [get-single-post-data
                            get-single-user-data]]
        [nightweb.db_tiles :only [get-post-tiles
                                  get-user-tiles
                                  get-category-tiles]]
        [nightweb.constants :only [is-me?]]
        [nightweb-desktop.utils :only [get-string]]
        [nightweb-desktop.dialogs :only [get-my-profile-dialog
                                         get-their-profile-dialog]]))

(defn get-tab-view
  [params show-me-tab?]
  (for [button (concat [(when show-me-tab?
                          {:type nil :title "Me"})]
                       [{:type :user :title "Users"}
                        {:type :post :title "Posts"}])]
    [:li {:class (when (= (get params :type) (get button :type)) "active")}
     [:a {:href (url-encode button (if show-me-tab? "/?" "/c?"))}
      (get button :title)]]))

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
                            (get button :dialog)
                            "').foundation('reveal', 'open');")}
          [:i {:class (get button :class)}]]]))

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
    (let [bg (get item :background)
          title (get-string (or (get item :title)
                                (get item :body)
                                (get item :tag)))
          add-emphasis? (get item :add-emphasis?)]
      [:a {:href "#"
           :onclick (str "tileAction('" (url-encode item "") "')")
           :class "grid-view-tile"
           :style (format "background: url('img/%s.png') no-repeat;
                           background-size: 100%%;
                           text-align: %s;"
                          (if bg (name bg))
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
     (if (is-me? (get user :userhash))
       (get-my-profile-dialog user)
       (get-their-profile-dialog user))]))

(defn get-category-view
  [params]
  (let [tiles (get-category-tiles params)]
    (get-grid-view tiles)))
