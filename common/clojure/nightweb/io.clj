(ns nightweb.io
  (:use [clojure.java.io :only [file
                                input-stream
                                output-stream]]))

(defn file-exists
  [path]
  (.exists (file path)))

(defn file-write
  [path data-barray]
  (with-open [bos (output-stream path)]
    (.write bos data-barray 0 (alength data-barray))))

(defn file-read
  [path]
  (let [length (.length (file path))
        data-bytes (byte-array length)]
    (with-open [bis (input-stream path)]
      (.read bis data-bytes))
    data-bytes))

(defn b-encode
  [data-map]
  (org.klomp.snark.bencode.BEncoder/bencode data-map))

(defn b-decode
  [data-barray]
  (try
    (.getMap (org.klomp.snark.bencode.BDecoder/bdecode
               (java.io.ByteArrayInputStream. data-barray)))
    (catch java.lang.Exception e nil)))
