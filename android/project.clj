(defproject nightweb-android/Nightweb "0.0.17"
  :license {:name "Public Domain"
            :url "http://unlicense.org/UNLICENSE"}
  :min-lein-version "2.0.0"

  :warn-on-reflection true

  :source-paths ["src/clojure" "../common/clojure"]
  :java-source-paths ["src/java" "../common/java" "gen"]
  :resource-paths ["libs/android-support-v13.jar" "libs/jsr166y.jar"]
  ;; The following two definitions are optional. The default
  ;; target-path is "target", but you can change it to whatever you like.
  ;; :target-path "bin"
  ;; :compile-path "bin/classes"

  :dependencies [[android/clojure "1.5.0"]
                 [neko/neko "2.0.0-beta3"]
                 [com.h2database/h2 "1.3.170"]
                 [markdown-clj "0.9.19"]]
  :profiles {:dev {:dependencies [[android/tools.nrepl "0.2.0-bigstack"]]
                   :android {:aot :all-with-unused}}
             :release {:android
                       {;; Specify the path to your private
                        ;; keystore and the the alias of the
                        ;; key you want to sign APKs with.
                        ;; :keystore-path "/home/user/.android/private.keystore"
                        ;; :key-alias "mykeyalias"
                        :aot :all}}}

  :android {;; Specify the path to the Android SDK directory either
            ;; here or in your ~/.lein/profiles.clj file.
            ;; :sdk-path "/home/user/path/to/android-sdk/"
            :target-version "15"
            :aot-exclude-ns ["clojure.parallel"]
            :dex-opts ["-JXmx2048M"]})
