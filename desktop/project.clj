(defproject nightweb-desktop "0.0.17"
  :license {:name "Public Domain"
            :url "http://unlicense.org/UNLICENSE"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.h2database/h2 "1.3.170"]
                 [seesaw "1.4.3"]
                 [com.github.insubstantial/substance "7.1"]
                 [ring "1.1.8"]
                 [hiccup "1.0.3"]
                 [markdown-clj "0.9.19"]]
  :main nightweb-desktop.core
  :source-paths ["src" "../common/clojure"]
  :java-source-paths ["../common/java"]
  :resource-paths ["resources"
                   "libs/jwrapper_utils.jar"
                   "../android/res/values"
                   "../android/res/drawable"])
