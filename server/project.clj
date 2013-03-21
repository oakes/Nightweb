(defproject nightweb-server/Nightweb "0.0.1-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Public Domain"
            :url "http://unlicense.org/UNLICENSE"}
  :dependencies [[org.clojure/clojure "1.5.0"]
                 [com.h2database/h2 "1.3.170"]]
  :main nightweb-server.core
  :source-paths ["src" "../common/clojure"]
  :java-source-paths ["../common/java"])
