(ns nightweb-desktop.views
  (:use [hiccup.core :only [html]]))

(defn get-main-view
  []
  (html [:head
         [:title "Nightweb"]
         [:link {:rel "stylesheet" :href "foundation.min.css"}]]
        [:body
         [:div {:id "my-modal" :class "reveal-modal"}
          [:h2 "Hello World!"]
          [:a {:class "close-reveal-modal"} "&#215;"]]
         [:a {:href "#" :class "button" :data-reveal-id "my-modal"}
          "Hello World!"]
         [:script {:src "zepto.js"}]
         [:script {:src "foundation.min.js"}]
         [:script "$(document).foundation();"]]))
