(ns nightweb.db
  (:use [nightweb.jdbc :only [with-connection
                              transaction
                              drop-table
                              create-table-if-not-exists
                              update-or-insert-values
                              with-query-results]]
        [clojure.java.io :only [file]]
        [nightweb.io :only [read-file
                            base32-decode
                            b-decode
                            b-decode-string
                            long-decode]]
        [nightweb.constants :only [slash
                                   meta-dir
                                   db-file
                                   my-hash-bytes]]))

(def spec nil)

(defn run-query
  ([f params] (run-query f params (fn [results] results)))
  ([f params callback]
   (with-connection
     spec
     (transaction (f params callback)))))

; initialization

(defn drop-tables
  [params callback]
  (try
    (do
      (drop-table :user)
      (drop-table :post)
      (drop-table :file)
      (drop-table :prev)
      (drop-table :fav))
    (catch java.lang.Exception e (println "Tables don't exist"))))

(defn create-tables
  [params callback]
  (create-table-if-not-exists
    :user
    [:hash "BINARY"]
    [:text "VARCHAR"]
    [:about "VARCHAR"])
  (create-table-if-not-exists
    :post
    [:userhash "BINARY"]
    [:text "VARCHAR"]
    [:time "BIGINT"])
  (create-table-if-not-exists
    :file
    [:hash "BINARY"]
    [:userhash "BINARY"]
    [:time "BIGINT"]
    [:name "VARCHAR"]
    [:ext "VARCHAR"]
    [:bytes "BIGINT"])
  (create-table-if-not-exists
    :prev
    [:hash "BINARY"]
    [:userhash "BINARY"]
    [:filehash "BINARY"])
  (create-table-if-not-exists
    :fav
    [:hash "BINARY"]
    [:userhash "BINARY"]))

(defn init-db
  [base-dir]
  (when (nil? spec)
    (def spec
      {:classname "org.h2.Driver"
       :subprotocol "h2"
       :subname (str base-dir db-file)})
    ;(run-query drop-tables nil)
    (run-query create-tables nil)))

; insertion

(defn insert-profile
  [user-hash args]
  (with-connection
    spec
    (update-or-insert-values
      :user
      ["hash = ?" user-hash]
      {:hash user-hash 
       :text (b-decode-string (get args "name"))
       :about (b-decode-string (get args "about"))})))

(defn insert-post
  [user-hash time-str args]
  (if-let [time-long (long-decode time-str)]
    (with-connection
      spec
      (update-or-insert-values
        :post
        ["userhash = ? AND time = ?" user-hash time-long]
        {:userhash user-hash
         :text (b-decode-string (get args "text"))
         :time time-long}))))

(defn insert-fav
  [user-hash fav-hash]
  (with-connection
    spec
    (update-or-insert-values
      :fav
      ["hash = ? AND userhash = ?" fav-hash user-hash]
      {:hash fav-hash
       :userhash user-hash})))

(defn insert-data
  [user-dir path-leaves]
  (let [end-path (str slash (clojure.string/join slash path-leaves))
        full-path (str (.getAbsolutePath user-dir) meta-dir end-path)
        user-hash-str (.getName user-dir)
        user-hash-bytes (base32-decode user-hash-str)
        rev-leaves (reverse path-leaves)
        file-name (nth rev-leaves 0 nil)
        type-name (nth rev-leaves 1 nil)]
    (if-let [args (b-decode (read-file full-path))]
      (case type-name
        "post" (insert-post user-hash-bytes file-name args)
        nil (case file-name
              "user.profile" (insert-profile user-hash-bytes args))))))

; retrieval

(defn get-user-data
  [params callback]
  (let [user-hash (get params :hash)
        is-me? (java.util.Arrays/equals user-hash my-hash-bytes)]
    (with-query-results
      rs
      ["SELECT * FROM user WHERE hash = ?" user-hash]
      (if-let [user (first rs)]
        (callback (assoc user :is-me? is-me?))
        (callback (assoc params :is-me? is-me?))))))

(defn get-post-data
  [params callback]
  (let [user-hash (get params :hash)]
    (with-query-results
      rs
      ["SELECT * FROM post WHERE userhash = ? ORDER BY time DESC" user-hash]
      (callback (doall
                  (for [row rs]
                    (assoc row :type :post)))))))

(defn get-category-data
  [params callback]
  (let [data-type (get params :type)
        user-hash (get params :hash)
        statement (case data-type
                    :user ["SELECT * FROM user"]
                    :post ["SELECT * FROM post ORDER BY time DESC"]
                    :user-fav
                    [(str "SELECT * FROM user "
                          "INNER JOIN fav ON user.hash = fav.pointer "
                          "WHERE fav.user = ?")
                     user-hash]
                    :post-fav
                    [(str "SELECT * FROM post "
                          "INNER JOIN fav ON post.userhash = fav.hash "
                          "WHERE fav.userhash = ? "
                          "ORDER BY time DESC")
                     user-hash]
                    :all-tran ["SELECT * FROM file"]
                    :photos-tran ["SELECT * FROM file"]
                    :videos-tran ["SELECT * FROM file"]
                    :audio-tran ["SELECT * FROM file"])]
    (with-query-results
      rs
      statement
      (callback (doall
                  (for [row rs]
                    (assoc row :type data-type)))))))
