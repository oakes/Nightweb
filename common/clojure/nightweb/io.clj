(ns nightweb.io
  (:use [clojure.java.io :only [file
                                input-stream
                                output-stream]]))

(defn file-exists
  [path]
  (.exists (file path)))

(defn file-write
  [path data-barray]
  (if-let [parent-dir (.getParentFile (file path))]
    (.mkdirs parent-dir))
  (with-open [bos (output-stream path)]
    (.write bos data-barray 0 (alength data-barray))))

(defn file-read
  [path]
  (let [length (.length (file path))
        data-barray (byte-array length)]
    (with-open [bis (input-stream path)]
      (.read bis data-barray))
    data-barray))

(defn b-encode
  [data-map]
  (org.klomp.snark.bencode.BEncoder/bencode data-map))

(defn b-decode
  [data-barray]
  (try
    (.getMap (org.klomp.snark.bencode.BDecoder/bdecode
               (java.io.ByteArrayInputStream. data-barray)))
    (catch java.lang.Exception e nil)))

(defn base32-encode
  [data-barray]
  (net.i2p.data.Base32/encode data-barray))

(defn base32-decode
  [data-str]
  (net.i2p.data.Base32/decode data-str))
