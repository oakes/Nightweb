(defproject nightweb-desktop "0.0.1-SNAPSHOT"
  :license {:name "Public Domain"
            :url "http://unlicense.org/UNLICENSE"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.h2database/h2 "1.3.170"]
                 [seesaw "1.4.3"]
                 [ring "1.1.8"]
                 [hiccup "1.0.3"]]
  :main nightweb-desktop.core
  :source-paths ["src" "../common/clojure"]
  :java-source-paths ["../common/java"]
  :resource-paths ["resources"
                   "../android/res/values"])
