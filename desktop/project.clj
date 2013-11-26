(defproject nightweb-desktop "0.0.23"
  :license {:name "Public Domain"
            :url "http://unlicense.org/UNLICENSE"}
  :dependencies [[com.github.insubstantial/substance "7.2.1"]
                 [com.h2database/h2 "1.3.174"]
                 [hiccup "1.0.4"]
                 [markdown-clj "0.9.35"]
                 [org.clojure/clojure "1.5.1"]
                 [org.clojure/java.jdbc "0.3.0-beta2"]
                 [ring "1.2.1"]
                 [seesaw "1.4.4"]]
  :source-paths ["src" "../common/clojure"]
  :java-source-paths ["../common/java"]
  :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:-options"]
  :resource-paths ["resources"
                   "../android/res"
                   "../android/res/drawable"]
  :aot [nightweb-desktop.core]
  :main nightweb-desktop.core)
