(ns nightweb-desktop.views)

(defn get-top-bar
  [tab-selected]
  [:div {:class "sticky"}
   [:nav {:class "top-bar"}
    [:section {:class "top-bar-section"}
     [:ul {:class "left"}
      (for [title ["Me" "Users" "Posts"]]
        [:li {:class (when (= title tab-selected) "active")}
         [:a {:href "#"} title]])]
     [:ul {:class "right"}
      (for [button [{:class "foundicon-search"
                     :title "Search"}
                    {:class "foundicon-plus"
                     :title "New Post"}
                    {:class "foundicon-page"
                     :title "Link"}]]
        [:li [:a {:href "#"}
              [:i {:class (get button :class)}]
              (str "&nbsp;" (get button :title))]])]]]])
