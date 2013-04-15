(ns nightweb-desktop.views
  (:use [nightweb.formats :only [url-encode]]))

(defn get-menu-view
  []
  (for [button [{:class "foundicon-search"
                 :title "Search"}
                {:class "foundicon-plus"
                 :title "New Post"}
                {:class "foundicon-page"
                 :title "Link"}
                {:class "foundicon-settings"
                 :title "Settings"}]]
    [:li [:a {:href "#"}
          [:i {:class (get button :class)}]
          (str "&nbsp;" (get button :title))]]))

(defn get-tab-view
  [params show-me-tab?]
  (for [button (concat [(when show-me-tab?
                          {:type nil :title "Me"})]
                       [{:type :user :title "Users"}
                        {:type :post :title "Posts"}])]
    [:li {:class (when (= (get params :type) (get button :type)) "active")}
     [:a {:href (url-encode button (if show-me-tab? "/?" "/c?"))}
      (get button :title)]]))

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
  [:ul {:class "grid-view"}
   (for [item content]
     [:li])])

(defn get-user-view
  [params]
  (get-grid-view (range 10)))

(defn get-post-view
  [params]
  (get-grid-view []))

(defn get-category-view
  [params]
  (get-grid-view []))
