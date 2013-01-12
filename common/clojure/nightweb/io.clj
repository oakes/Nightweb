(ns nightweb.io
  (:use [clojure.java.io :only [file writer]]))

(defn file-exists
  [path]
  (.exists (file path)))

(defn file-write
  [path data]
  (with-open [wrtr (writer path)]
    (.write wrtr data)))

(defn file-read
  [path]
  (slurp path))

(defn b-encode
  [data-map]
  (org.klomp.snark.bencode.BEncoder/bencode data-map))

(defn b-decode
  [data-barray]
  (try
    (.getMap (org.klomp.snark.bencode.BDecoder/bdecode
               (java.io.ByteArrayInputStream. data-barray)))
    (catch java.lang.Exception e nil)))
