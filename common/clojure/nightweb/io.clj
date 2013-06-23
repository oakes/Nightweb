(ns nightweb.io
  (:require [clojure.java.io :as java.io]
            [nightweb.constants :as c]
            [nightweb.crypto :as crypto]
            [nightweb.db :as db]
            [nightweb.formats :as f])
  (:import [java.io File]
           [net.i2p.data PrivateKeyFile]))

; basic file operations

(defn file-exists?
  [path]
  (.exists (java.io/file path)))

(defn write-file
  [path data-barray]
  (when-let [parent-dir (.getParentFile (java.io/file path))]
    (.mkdirs parent-dir))
  (with-open [bos (java.io/output-stream path)]
    (.write bos data-barray 0 (alength ^bytes data-barray))))

(defn read-file
  [path]
  (when (file-exists? path)
    (let [length (.length (java.io/file path))]
      (when (< length 500000)
        (let [data-barray (byte-array length)]
          (with-open [bis (java.io/input-stream path)]
            (.read bis data-barray))
          data-barray)))))

(defn delete-file
  [path]
  (.delete (java.io/file path)))

(defn delete-file-recursively
  [path]
  (let [^File f (java.io/file path)]
    (when (.isDirectory f)
      (doseq [child (.listFiles f)]
        (delete-file-recursively child)))
    (delete-file f)))

(defn make-dir
  [path]
  (.mkdirs (java.io/file path)))

(defn iterate-dir
  [path func]
  (doseq [^File f (.listFiles (java.io/file path))]
    (when (.isDirectory f)
      (func (.getName f)))))

(defn list-dir
  [path]
  (for [^File f (.listFiles (java.io/file path))]
    (when (.isDirectory f)
      (.getName f))))

(defn get-files-in-uri
  [uri-str]
  (let [java-uri (java.net.URI/create uri-str)
        files (for [^File uri-file (file-seq (java.io/file java-uri))]
                (when (.isFile uri-file)
                  (.getCanonicalPath uri-file)))]
    (f/remove-dupes-and-nils files)))

; read/write specific files

(defn write-key-file
  [file-path key-obj]
  (write-file file-path (f/key-encode key-obj)))

(defn read-key-file
  [file-path]
  (when-let [key-map (f/b-decode-map (f/b-decode (read-file file-path)))]
    (when-let [sign-key-str (.get key-map "sign_key")]
      (.getBytes sign-key-str))))

(defn write-priv-node-key-file
  [priv-node]
  (let [priv-node-file (PrivateKeyFile.
                         (java.io/file (str @c/base-dir c/priv-node-key-file))
                         priv-node)]
    (.write priv-node-file)))

(defn read-priv-node-key-file
  []
  (let [path (str @c/base-dir c/priv-node-key-file)]
    (when (file-exists? path)
      (java.io/input-stream (java.io/file path)))))

(defn write-pub-node-key-file
  [pub-node]
  (write-file (str @c/base-dir c/pub-node-key-file)
              (.getBytes pub-node)))

(defn read-pub-node-key-file
  []
  (let [path (str @c/base-dir c/pub-node-key-file)]
    (when (file-exists? path)
      (org.klomp.snark.dht.NodeInfo. (apply str (map char (read-file path)))))))

(defn write-pic-file
  [data-barray]
  (when data-barray
    (let [image-hash (crypto/create-hash data-barray)
          file-name (f/base32-encode image-hash)]
      (write-file (str (c/get-pic-dir @c/my-hash-str) c/slash file-name)
                  data-barray)
      image-hash)))

(defn write-post-file
  [create-time encoded-args]
  (write-file (str (c/get-post-dir @c/my-hash-str)
                   c/slash
                   create-time)
              encoded-args))

(defn write-profile-file
  [encoded-args]
  (write-file (str (c/get-meta-dir @c/my-hash-str)
                   c/slash
                   c/profile)
              encoded-args))

(defn write-fav-file
  [create-time encoded-args]
  (write-file (str (c/get-fav-dir @c/my-hash-str)
                   c/slash
                   create-time)
              encoded-args))

(defn write-link-file
  [link-hash]
  (write-file (str (c/get-meta-dir @c/my-hash-str) c/link-ext)
              (f/link-encode link-hash)))

(defn read-user-list-file
  []
  (-> (c/get-user-list-file)
      (read-file)
      (f/b-decode)
      (f/b-decode-byte-list)))

(defn write-user-list-file
  [user-list]
  (write-file (c/get-user-list-file) (f/b-encode user-list)))

(defn read-link-file
  [user-hash-str]
  (let [link-path (str (c/get-meta-dir user-hash-str) c/link-ext)]
    (if-let [link-bytes (read-file link-path)]
      (f/b-decode-map (f/b-decode link-bytes))
      (doto (java.util.HashMap.)
        (.put "data" (f/b-encode {"user_hash"
                                  (f/base32-decode user-hash-str)}))))))

(defn read-meta-file
  ([user-dir path-leaves]
   (read-meta-file (->> (clojure.string/join c/slash path-leaves)
                        (str c/slash)
                        (str (.getAbsolutePath user-dir) c/meta-dir))))
  ([path]
   (let [path-parts (reverse (clojure.string/split path (re-pattern c/slash)))]
     {:file-name (nth path-parts 0 nil)
      :dir-name (nth path-parts 1 nil)
      :contents (f/b-decode-map (f/b-decode (read-file path)))})))

(defn delete-orphaned-pics
  [user-hash]
  (when user-hash
    (doseq [^File pic (-> (f/base32-encode user-hash)
                          c/get-pic-dir
                          java.io/file
                          file-seq)]
      (when (and (.isFile pic)
                 (-> {:userhash user-hash
                      :pichash (f/base32-decode (.getName pic))}
                     (db/get-pic-data)
                     (count)
                     (= 0)))
        (.delete pic)))))

(defn delete-orphaned-files
  [user-hash file-list]
  (when user-hash
    (let [meta-dir (c/get-meta-dir (f/base32-encode user-hash))]
      (doseq [^File meta-file (file-seq (java.io/file meta-dir))]
        (when (and (.isFile meta-file)
                   (->> file-list
                        (filter #(= (.getCanonicalPath meta-file)
                                    (->> (clojure.string/join c/slash %)
                                         (java.io/file meta-dir)
                                         .getCanonicalPath)))
                        (count)
                        (= 0)))
          (.delete meta-file))))))
