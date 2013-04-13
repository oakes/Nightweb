(ns nightweb-desktop.pages
  (:use [hiccup.core :only [html]]))

(defn get-main-page
  []
  (html [:h1 "Hello World!"]))
