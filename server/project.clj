(defproject nightweb-server/Nightweb "0.0.24"
  :license {:name "Public Domain"
            :url "http://unlicense.org/UNLICENSE"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/java.jdbc "0.3.0"]
                 [com.h2database/h2 "1.3.174"]]
  :source-paths ["src" "../common/clojure"]
  :java-source-paths ["../common/java"]
  :aot [nightweb-server.core]
  :main nightweb-server.core)
