(ns nightweb.io
  (:use [clojure.java.io :only [file
                                input-stream
                                output-stream]]
        [nightweb.constants :only [base-dir
                                   my-hash-bytes
                                   my-hash-str
                                   slash
                                   post-ext
                                   link-ext
                                   get-user-dir
                                   get-meta-dir
                                   get-posts-dir
                                   priv-node-key-file
                                   pub-node-key-file
                                   profile-file]]))

; basic file operations

(defn file-exists?
  [path]
  (.exists (file path)))

(defn write-file
  [path data-barray]
  (if-let [parent-dir (.getParentFile (file path))]
    (.mkdirs parent-dir))
  (with-open [bos (output-stream path)]
    (.write bos data-barray 0 (alength data-barray))))

(defn read-file
  [path]
  (when (file-exists? path)
    (let [length (.length (file path))
          data-barray (byte-array length)]
      (with-open [bis (input-stream path)]
        (.read bis data-barray))
      data-barray)))

(defn make-dir
  [path]
  (.mkdirs (file path)))

(defn iterate-dir
  [path func]
  (doseq [f (.listFiles (file path))]
    (if (.isDirectory f)
      (func (.getName f)))))

; encodings/decodings

(defn b-encode
  [data-map]
  (org.klomp.snark.bencode.BEncoder/bencode data-map))

(defn b-decode
  [data-barray]
  (try
    (.getMap (org.klomp.snark.bencode.BDecoder/bdecode
               (java.io.ByteArrayInputStream. data-barray)))
    (catch java.lang.Exception e nil)))

(defn b-decode-bytes
  [be-value]
  (try
    (.getBytes be-value)
    (catch java.lang.Exception e nil)))

(defn b-decode-number
  [be-value]
  (try
    (.getNumber be-value)
    (catch java.lang.Exception e nil)))

(defn b-decode-string
  [be-value]
  (try
    (.getString be-value)
    (catch java.lang.Exception e nil)))

(defn base32-encode
  [data-barray]
  (if data-barray
    (net.i2p.data.Base32/encode data-barray)))

(defn base32-decode
  [data-str]
  (if data-str
    (net.i2p.data.Base32/decode data-str)))

; read/write specific files

(defn write-key-file
  [file-path key-data]
  (write-file file-path (b-encode {"sign_key" key-data
                                   "sign_algo" "DSA-SHA1"})))

(defn read-key-file
  [file-path]
  (if-let [priv-key-map (b-decode (read-file file-path))]
    (if-let [sign-key-str (.get priv-key-map "sign_key")]
      (.getBytes sign-key-str))))

(defn write-priv-node-key-file
  [priv-node]
  (let [priv-node-file (net.i2p.data.PrivateKeyFile.
                         (file (str base-dir priv-node-key-file))
                         priv-node)]
    (.write priv-node-file)))

(defn read-priv-node-key-file
  []
  (let [path (str base-dir priv-node-key-file)]
    (if (file-exists? path)
      (input-stream (file path)))))

(defn write-pub-node-key-file
  [pub-node]
  (write-file (str base-dir pub-node-key-file)
              (.getBytes pub-node)))

(defn read-pub-node-key-file
  []
  (let [path (str base-dir pub-node-key-file)]
    (if (file-exists? path)
      (org.klomp.snark.dht.NodeInfo. (apply str (map char (read-file path)))))))

(defn write-post-file
  [text]
  (write-file (str (get-posts-dir my-hash-str)
                   slash (.getTime (java.util.Date.)) post-ext)
              (b-encode {"text" text})))

(defn write-profile-file
  [name-text about-text]
  (write-file (str (get-meta-dir my-hash-str) profile-file)
              (b-encode {"name" name-text
                         "about" about-text})))

(defn write-link-file
  [file-path link-hash sign]
  (let [signed-data (b-encode {"user_hash" my-hash-bytes
                               "link_hash" link-hash
                               "time" (.getTime (java.util.Date.))})
        signature (sign signed-data)]
    (write-file (str file-path link-ext)
                (b-encode {"data" signed-data
                           "sig" signature}))))

(defn read-link-file
  [file-path]
  (let [link-path (str file-path link-ext)]
    (if (file-exists? link-path)
      (if-let [link-bytes (read-file link-path)]
        (b-decode link-bytes)))))
