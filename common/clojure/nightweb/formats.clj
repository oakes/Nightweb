(ns nightweb.formats)

(defn b-encode
  [data-value]
  (try
    (org.klomp.snark.bencode.BEncoder/bencode data-value)
    (catch java.lang.Exception e nil)))

(defn b-decode
  [data-barray]
  (try
    (org.klomp.snark.bencode.BDecoder/bdecode
      (java.io.ByteArrayInputStream. data-barray))
    (catch java.lang.Exception e nil)))

(defn b-decode-map
  [be-value]
  (try
    (.getMap be-value)
    (catch java.lang.Exception e nil)))

(defn b-decode-list
  [be-value]
  (try
    (.getList be-value)
    (catch java.lang.Exception e nil)))

(defn b-decode-bytes
  [be-value]
  (try
    (.getBytes be-value)
    (catch java.lang.Exception e nil)))

(defn b-decode-long
  [be-value]
  (try
    (.getLong be-value)
    (catch java.lang.Exception e nil)))

(defn b-decode-string
  [be-value]
  (try
    (.getString be-value)
    (catch java.lang.Exception e nil)))

(defn b-decode-byte-list
  [be-value]
  (vec
    (for [list-item (b-decode-list be-value)]
      (b-decode-bytes list-item))))

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

(defn url-encode
  [content]
  (let [params (concat
                 (if-let [type-val (get content :type)]
                   [(str "type=" (name type-val))])
                 (if-let [userhash-val (get content :userhash)]
                   [(str "userhash=" (base32-encode userhash-val))])
                 (if-let [time-val (get content :time)]
                   [(str "time=" time-val)]))]
    (str "http://nightweb.net#" (clojure.string/join "&" params))))

(defn url-decode
  [url]
  (let [url-str (subs url (+ 1 (.indexOf url "#")))
        url-vec (clojure.string/split url-str #"[&=]")
        url-map (if (even? (count url-vec))
                  (apply hash-map url-vec)
                  {})
        {type-val "type"
         userhash-val "userhash"
         time-val "time"} url-map]
    {:type (if type-val (keyword type-val))
     :userhash (if userhash-val (base32-decode userhash-val))
     :time (if time-val (long-decode time-val))}))

(defn remove-dupes-and-nils
  [the-list]
  (->> the-list
       (distinct)
       (filter identity)))
