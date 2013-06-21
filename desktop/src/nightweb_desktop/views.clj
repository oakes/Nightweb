(ns nightweb-desktop.views
  (:require [markdown.core :as markdown]
            [nightweb.constants :as c]
            [nightweb.db :as db]
            [nightweb.db_tiles :as tiles]
            [nightweb.formats :as f]
            [nightweb-desktop.dialogs :as dialogs]
            [nightweb-desktop.utils :as utils]))

(defn get-tab-view
  [params show-me-tab?]
  (for [button [{:type nil :title "Me"}
                {:type :user :title "Users"}
                {:type :post :title "Posts"}]]
    (when (or show-me-tab? (not= nil (:type button)))
      (let [active-tab (or (:subtype params) (:type params))]
        [:li {:class (when (= active-tab (:type button)) "active")}
         [:a {:href (f/url-encode (if (:subtype params)
                                    (assoc params :subtype (:type button))
                                    button)
                                (if show-me-tab? "/?" "/c?"))}
          (:title button)]]))))

(defn get-menu-view
  []
  (for [button [{:class "foundicon-search"
                 :title (utils/get-string :search)
                 :dialog "search-dialog"}
                {:class "foundicon-plus"
                 :title (utils/get-string :new_post)
                 :dialog "new-post-dialog"}
                {:class "foundicon-flag"
                 :title (utils/get-string :share)
                 :dialog "link-dialog"}
                {:class "foundicon-people"
                 :title (utils/get-string :switch_user)
                 :dialog "switch-user-dialog"}]]
    [:li [:a {:href "#"
              :onclick (format "showDialog('%s')" (:dialog button))}
          [:i {:class (:class button)}]]]))

(defn get-action-bar-view
  [params & {:keys [is-main? show-tabs? show-home-button?]}]
  [:div {:class "sticky"}
   [:nav {:class "top-bar"}
    [:section {:class "top-bar-section"}
     (when show-home-button?
       [:a {:href "/" :class "home-button"}])
     [:ul {:class "left"}
      (when-let [title (or (:title params) (:tag params))]
        [:li [:div {:class "title"} title]])
      (when show-tabs? (get-tab-view params is-main?))]
     [:ul {:class "right"} (get-menu-view)]]]
   [:div {:class "clear"}]])

(defn get-grid-view
  [content]
  (for [item content]
    (let [background (or (utils/get-pic (:userhash item) (:pichash item))
                         (when-let [bg (:background item)]
                           (str (name bg) ".png")))
          title (utils/get-string (or (:title item)
                                      (:body item)
                                      (:tag item)))
          add-emphasis? (:add-emphasis? item)
          is-pic? (= :pic (:type item))]
      [(if is-pic? :li :div)
       [:a {:href (if is-pic? background "#")
            :onclick (format "doAction(\"%s\", \"%s\")"
                             (f/url-encode item "")
                             (or (utils/get-string (:confirm item)) ""))
            :class "grid-view-tile square-image"
            :style (format "background-image: url(%s); text-align: %s;"
                           (if is-pic? "" background)
                           (if add-emphasis? "center" "left"))}
        (if is-pic? [:img {:src background
                           :style "width: 100%; height: 100%;"}])
        (if add-emphasis? [:strong title] [:div title])]])))

(defn get-post-view
  [params]
  (let [post (db/get-single-post-data params)
        tiles (tiles/get-post-tiles post)
        html-text (->> (:body post)
                       (f/tags-encode :post)
                       markdown/md-to-html-string)
        html-tiles (get-grid-view tiles)]
    [:div {:id "post"}
     [:div {:id "post-body" :class "contains-links"} html-text]
     (filter #(= :div (get % 0)) html-tiles)
     [:ul {:data-clearing true}
      (filter #(= :li (get % 0)) html-tiles)]]))

(defn get-user-view
  [params]
  (let [user (db/get-single-user-data params)
        tiles (tiles/get-user-tiles params user)]
    [:div
     (get-grid-view tiles)
     (if (c/is-me? (:userhash user))
       (dialogs/get-my-profile-dialog user)
       (dialogs/get-their-profile-dialog user))]))

(defn get-category-view
  [params]
  (let [tiles (tiles/get-category-tiles params)]
    (get-grid-view tiles)))
