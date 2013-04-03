(defproject nightweb-desktop "0.1.0-SNAPSHOT"
  :license {:name "Public Domain"
            :url "http://unlicense.org/UNLICENSE"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.h2database/h2 "1.3.170"]
                 [splendid/jfx "0.5.0"]
                 [ring "1.1.8"]]
  :main nightweb-desktop.core
  :source-paths ["src" "../common/clojure"]
  :java-source-paths ["../common/java"])
