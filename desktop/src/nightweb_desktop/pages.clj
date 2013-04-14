(ns nightweb-desktop.pages
  (:use [hiccup.core :only [html]]
        [nightweb-desktop.views :only [get-top-bar]]))

(defn get-main-page
  []
  (html [:head
         [:title "Nightweb"]
         [:link {:rel "stylesheet" :href "foundation.min.css"}]
         [:link {:rel "stylesheet" :href "foundation.override.css"}]
         [:link {:rel "stylesheet" :href "fonts/general_foundicons.css"}]]
        [:body
         (get-top-bar "Me")
         [:script {:src "zepto.js"}]
         [:script {:src "foundation.min.js"}]
         [:script "$(document).foundation();"]]))

(defn get-page
  [params]
  (if (empty? params)
    (get-main-page)
    (html [:h2 "Not found."])))
