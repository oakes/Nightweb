(ns nightweb.io
  (:use [clojure.java.io :only [file
                                input-stream
                                output-stream]]
        [nightweb.crypto :only [create-signature]]
        [nightweb.constants :only [base-dir
                                   my-hash-bytes
                                   my-hash-str
                                   slash
                                   link-ext
                                   profile
                                   priv-node-key-file
                                   pub-node-key-file
                                   get-user-dir
                                   get-meta-dir
                                   get-post-dir]]))

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

(defn b-decode-integer
  [be-value]
  (try
    (.getLong be-value)
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

(defn long-decode
  [data-str]
  (try
    (Long/parseLong data-str)
    (catch java.lang.Exception e nil)))

(defn parse-url
  [url]
  (let [url-str (subs url (+ 1 (.indexOf url "#")))
        url-vec (clojure.string/split url-str #"[&=]")
        url-map (if (even? (count url-vec))
                  (apply hash-map url-vec)
                  {})
        {type-val "type" hash-val "hash"} url-map]
    {:type (if type-val (keyword type-val) nil)
     :hash (if hash-val (base32-decode hash-val) nil)}))

; read/write specific files

(defn write-key-file
  [file-path key-obj]
  (write-file file-path (b-encode {"sign_key" (.getData key-obj)
                                   "sign_algo" "DSA-SHA1"})))

(defn read-key-file
  [file-path]
  (if-let [key-map (b-decode (read-file file-path))]
    (if-let [sign-key-str (.get key-map "sign_key")]
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
  (let [unix-time (.getTime (java.util.Date.))
        args {"text" text}]
    (write-file (str (get-post-dir my-hash-str) slash unix-time)
                (b-encode args))))

(defn write-profile-file
  [name-text about-text]
  (let [args {"name" name-text
              "about" about-text}]
    (write-file (str (get-meta-dir my-hash-str) slash profile)
                (b-encode args))))

(defn write-link-file
  [link-hash]
  (let [args {"user_hash" my-hash-bytes
              "link_hash" link-hash
              "time" (.getTime (java.util.Date.))}
        signed-data (b-encode args)
        signature (create-signature signed-data)]
    (write-file (str (get-meta-dir my-hash-str) link-ext)
                (b-encode {"data" signed-data
                           "sig" signature}))))

(defn read-link-file
  [user-hash-str]
  (let [link-path (str (get-meta-dir user-hash-str) link-ext)]
    (if-let [link-bytes (read-file link-path)]
      (b-decode link-bytes)
      (doto (java.util.HashMap.)
        (.put "data" (b-encode {"user_hash" (base32-decode user-hash-str)}))))))
