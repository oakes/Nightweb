(ns nightweb-desktop.pages
  (:use [hiccup.core :only [html]]))

(defn get-main-page
  []
  (html [:head
         [:title "Nightweb"]]
        [:body
         [:h1 "Hello World!"]]))
