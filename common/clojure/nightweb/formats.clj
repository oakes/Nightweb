(ns nightweb.formats
  (:use [nightweb.crypto :only [create-signature]]
        [nightweb.constants :only [my-hash-bytes]]))

(defn remove-dupes-and-nils
  [the-list]
  (->> the-list
       (distinct)
       (remove nil?)))

(defn is-numeric?
  [string]
  (try
    (java.lang.Integer/parseInt string)
    true
    (catch java.lang.Exception e false)))

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
    (vec (.getList be-value))
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
  (try
    (net.i2p.data.Base32/encode data-barray)
    (catch java.lang.Exception e nil)))

(defn base32-decode
  [data-str]
  (try
    (net.i2p.data.Base32/decode data-str)
    (catch java.lang.Exception e nil)))

(defn long-decode
  [data-str]
  (try
    (Long/parseLong data-str)
    (catch java.lang.Exception e nil)))

(defn url-encode
  [content]
  (let [params (remove-dupes-and-nils
                 [(when-let [type-val (get content :type)]
                    (str "type=" (name type-val)))
                  (when-let [subtype-val (get content :subtype)]
                    (str "subtype=" (name subtype-val)))
                  (when-let [userhash-val (get content :userhash)]
                    (str "userhash=" (base32-encode userhash-val)))
                  (when-let [time-val (get content :time)]
                    (str "time=" time-val))
                  (when-let [tag-val (get content :tag)]
                    (str "tag=" tag-val))])]
    (str "http://nightweb.net/#" (clojure.string/join "&" params))))

(defn url-decode
  [url]
  (let [url-str (subs url (+ 1 (.indexOf url "#")))
        url-vec (clojure.string/split url-str #"[&=]")
        url-map (if (even? (count url-vec))
                  (apply hash-map url-vec)
                  {})
        {type-val "type"
         subtype-val "subtype"
         userhash-val "userhash"
         time-val "time"
         tag-val "tag"} url-map]
    {:type (when type-val (keyword type-val))
     :subtype (when subtype-val (keyword subtype-val))
     :userhash (when userhash-val (base32-decode userhash-val))
     :time (when time-val (long-decode time-val))
     :tag tag-val}))

(def min-tag-length 2)
(def max-tag-count 20)

(defn get-tag
  [text-str]
  (when (and text-str (.startsWith text-str "#"))
    (let [tag (-> (clojure.string/replace text-str #"[\p{P}\t\r\n]" "")
                  (clojure.string/trim))]
      (when (and (>= (count tag) min-tag-length)
                 (not (is-numeric? tag)))
        (clojure.string/lower-case tag)))))

(defn tags-encode
  [type-name text-str]
  (when text-str
    (->> (for [word (clojure.string/split text-str #" ")]
           (if-let [tag (get-tag word)]
             (str "#[" tag "](" (url-encode {:type type-name :tag tag}) ")")
             word))
         (clojure.string/join " "))))

(defn tags-decode
  [text-str]
  (when text-str
    (->> (for [word (clojure.string/split text-str #" ")]
           (get-tag word))
         (remove-dupes-and-nils)
         (take max-tag-count))))

(defn key-encode
  [key-obj]
  (b-encode {"sign_key" (.getData key-obj)
             "sign_algo" "DSA-SHA1"}))

(defn post-encode
  [& {:keys [text pic-hashes status ptrhash ptrtime]}]
  (let [args (merge {"body" text
                     "mtime" (.getTime (java.util.Date.))
                     "pics" (remove-dupes-and-nils pic-hashes)
                     "status" status}
                    (if ptrhash {"ptrhash" ptrhash} {})
                    (if ptrtime {"ptrtime" ptrtime} {}))]
    (b-encode args)))

(defn profile-encode
  [name-text body-text image-hash]
  (let [args {"title" name-text
              "body" body-text
              "mtime" (.getTime (java.util.Date.))
              "pics" (if image-hash [image-hash] [])
              "status" 1}]
    (b-encode args)))

(defn fav-encode
  [ptr-hash ptr-time status]
  (let [args (merge {"ptrhash" ptr-hash
                     "mtime" (.getTime (java.util.Date.))
                     "status" status}
                    (if ptr-time {"ptrtime" ptr-time} {}))]
    (b-encode args)))

(defn link-encode
  [link-hash]
  (let [args {"user_hash" my-hash-bytes
              "link_hash" link-hash
              "mtime" (.getTime (java.util.Date.))}
        signed-data (b-encode args)
        signature (create-signature signed-data)]
    (b-encode {"data" signed-data
               "sig" signature})))
