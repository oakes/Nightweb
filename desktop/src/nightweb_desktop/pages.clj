(ns nightweb-desktop.pages
  (:use [hiccup.core :only [html]]))

(defn get-main-page
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

(defn get-page
  [params]
  (if (empty? params)
    (get-main-page)
    (html [:h2 "Not found."])))
