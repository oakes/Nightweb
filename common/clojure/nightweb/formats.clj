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
    (Integer/parseInt string)
    true
    (catch Exception e false)))

(defn b-encode
  [data-value]
  (try
    (org.klomp.snark.bencode.BEncoder/bencode data-value)
    (catch Exception e nil)))

(defn b-decode
  [data-barray]
  (try
    (org.klomp.snark.bencode.BDecoder/bdecode
      (java.io.ByteArrayInputStream. data-barray))
    (catch Exception e nil)))

(defn b-decode-map
  [be-value]
  (try
    (.getMap be-value)
    (catch Exception e nil)))

(defn b-decode-list
  [be-value]
  (try
    (vec (.getList be-value))
    (catch Exception e nil)))

(defn b-decode-bytes
  [be-value]
  (try
    (.getBytes be-value)
    (catch Exception e nil)))

(defn b-decode-long
  [be-value]
  (try
    (.getLong be-value)
    (catch Exception e nil)))

(defn b-decode-string
  [be-value]
  (try
    (.getString be-value)
    (catch Exception e nil)))

(defn b-decode-byte-list
  [be-value]
  (vec
    (for [list-item (b-decode-list be-value)]
      (b-decode-bytes list-item))))

(defn base32-encode
  [data-barray]
  (try
    (net.i2p.data.Base32/encode data-barray)
    (catch Exception e nil)))

(defn base32-decode
  [data-str]
  (try
    (net.i2p.data.Base32/decode data-str)
    (catch Exception e nil)))

(defn long-decode
  [data-str]
  (try
    (Long/parseLong data-str)
    (catch Exception e nil)))

(defn url-encode
  ([content] (url-encode content "http://nightweb.net/#"))
  ([content path]
   (let [params (remove-dupes-and-nils
                  [(when-let [type-val (:type content)]
                     (str "type=" (name type-val)))
                   (when-let [subtype-val (:subtype content)]
                     (str "subtype=" (name subtype-val)))
                   (when-let [userhash-val (:userhash content)]
                     (str "userhash=" (base32-encode userhash-val)))
                   (when-let [time-val (:time content)]
                     (str "time=" time-val))
                   (when-let [tag-val (:tag content)]
                     (str "tag=" tag-val))])]
     (str path (clojure.string/join "&" params)))))

(defn url-decode
  ([url] (url-decode url true))
  ([url whitelist?]
   (when url
     (let [url-str (subs url (+ 1 (.indexOf url "#")))
           url-map (->> (clojure.string/split url-str #"&")
                        (map #(clojure.string/split % #"="))
                        (map (fn [[k v]] [(keyword k) v]))
                        (into {}))]
       (if whitelist?
         (let [{type-val :type
                subtype-val :subtype
                userhash-val :userhash
                time-val :time
                tag-val :tag} url-map]
           {:type (when type-val (keyword type-val))
            :subtype (when subtype-val (keyword subtype-val))
            :userhash (when userhash-val (base32-decode userhash-val))
            :time (when time-val (long-decode time-val))
            :tag tag-val})
         url-map)))))

(def ^:const min-tag-length 2)
(def ^:const max-tag-count 20)
(def ^:const ignore-chars #"[?,:;.!\(\)\t\r\n ]")

(defn get-tag
  [text-str]
  (when (and text-str (.startsWith text-str "#"))
    (let [tag (subs text-str 1)]
      (when (and (>= (count tag) min-tag-length)
                 (not (is-numeric? tag)))
        tag))))

(defn tags-encode
  [type-name text-str]
  (when text-str
    (reduce (fn [new-text-str word]
              (if-let [tag (get-tag word)]
                (let [lower-case-tag (clojure.string/lower-case tag)
                      url (url-encode {:type type-name :tag lower-case-tag})
                      link (str "#[" tag "](" url ")")]
                  (clojure.string/replace new-text-str (str "#" tag) link))
                new-text-str))
            text-str
            (clojure.string/split text-str ignore-chars))))

(defn tags-decode
  [text-str]
  (when text-str
    (->> (for [word (clojure.string/split text-str ignore-chars)]
           (when-let [tag (get-tag word)]
             (clojure.string/lower-case tag)))
         (remove-dupes-and-nils)
         (take max-tag-count))))

(defn key-encode
  [key-obj]
  (b-encode {"sign_key" (.getData key-obj)
             "sign_algo" "DSA-SHA1"}))

(defn post-encode
  [& {:keys [text pic-hashes status ptrhash ptrtime]}]
  (let [args (merge {"body" (or text "")
                     "mtime" (.getTime (java.util.Date.))
                     "pics" (remove-dupes-and-nils pic-hashes)
                     "status" status}
                    (if ptrhash {"ptrhash" ptrhash} {})
                    (if ptrtime {"ptrtime" ptrtime} {}))]
    (b-encode args)))

(defn profile-encode
  [name-text body-text image-hash]
  (let [args {"title" (or name-text "")
              "body" (or body-text "")
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
  (let [args {"user_hash" @my-hash-bytes
              "link_hash" link-hash
              "mtime" (.getTime (java.util.Date.))}
        signed-data (b-encode args)
        signature (create-signature signed-data)]
    (b-encode {"data" signed-data
               "sig" signature})))
