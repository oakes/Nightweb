(defproject nightweb-android/Nightweb "0.0.25"
  :license {:name "Public Domain"
            :url "http://unlicense.org/UNLICENSE"}
  :min-lein-version "2.0.0"

  :warn-on-reflection true

  :source-paths ["src/clojure" "../common/clojure"]
  :java-source-paths ["src/java" "../common/java" "gen"]
  :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:-options"]

  :dependencies [[com.h2database/h2 "1.3.174"]
                 [markdown-clj "0.9.36"]
                 [neko/neko "3.0.0"]
                 [org.clojure/java.jdbc "0.3.2"]
                 [org.clojure-android/clojure "1.5.1-jb" :use-resources true]]
  :profiles {:dev {:dependencies [[android/tools.nrepl "0.2.0-bigstack"]]
                   :android {:aot :all-with-unused}}
             :release {:android
                       {;; Specify the path to your private
                        ;; keystore and the the alias of the
                        ;; key you want to sign APKs with.
                        ;; :keystore-path "/home/user/.android/private.keystore"
                        ;; :key-alias "mykeyalias"
                        :aot :all}}}

  :android {:support-libraries ["v13"]
            :target-version "15"
            :aot-exclude-ns ["clojure.parallel" "clojure.core.reducers"]
            :dex-opts ["-JXmx4096M"]})
