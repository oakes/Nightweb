(ns net.nightweb.utils
  (:use [clojure.java.io :only [input-stream]]
        [nightweb.formats :only [base32-encode]]
        [nightweb.constants :only [slash
                                   get-pic-dir]]))

(def full-size 1024)
(def thumb-size 256)

(defn get-resample-ratio
  [is max-length]
  (let [options (android.graphics.BitmapFactory$Options.)
        _ (set! (. options inJustDecodeBounds) true)
        _ (android.graphics.BitmapFactory/decodeStream is nil options)
        height-ratio (/ (. options outHeight) max-length)
        width-ratio (/ (. options outWidth) max-length)
        is-valid? (and (> height-ratio 0) (> width-ratio 0))
        is-too-big? (or (> height-ratio 1) (> width-ratio 1))]
    (when is-valid?
      (if is-too-big?
        (int (max height-ratio width-ratio))
        1))))

(defn input-stream-to-bitmap
  [is ratio]
  (let [options (android.graphics.BitmapFactory$Options.)]
    (set! (. options inSampleSize) ratio)
    (android.graphics.BitmapFactory/decodeStream is nil options)))

(defn uri-to-bitmap
  [context uri-str]
  (try
    (let [cr (.getContentResolver context)
          uri (android.net.Uri/parse uri-str)]
      (when-let [ratio (with-open [is (.openInputStream cr uri)]
                         (get-resample-ratio is full-size))]
        (with-open [is (.openInputStream cr uri)]
          (input-stream-to-bitmap is ratio))))
    (catch java.lang.Exception e nil)))

(defn path-to-bitmap
  [path max-length]
  (try
    (when-let [ratio (with-open [is (input-stream path)]
                       (get-resample-ratio is max-length))]
      (with-open [is (input-stream path)]
        (input-stream-to-bitmap is ratio)))
    (catch java.lang.Exception e nil)))

(defn bitmap-to-byte-array
  [image-bitmap]
  (when image-bitmap
    (let [out (java.io.ByteArrayOutputStream.)
          image-format android.graphics.Bitmap$CompressFormat/WEBP]
      (.compress image-bitmap image-format 90 out)
      (.toByteArray out))))

(defn get-pic-path
  "Gets the full path for the given user and image hash combination."
  [user-hash-bytes image-hash-bytes]
  (when (and user-hash-bytes image-hash-bytes)
    (str (get-pic-dir (base32-encode user-hash-bytes))
         slash
         (base32-encode image-hash-bytes))))

(defn make-dip
  "Converts the given number into density-independent pixels."
  [context number]
  (-> (.getResources context)
      (.getDisplayMetrics)
      (.density)
      (* number)
      (int)))

(def default-text-size 20)
(def large-text-size 30)

(defn set-text-size
  "Sets the given view's text size in density-independent pixels."
  [view size]
  (.setTextSize view android.util.TypedValue/COMPLEX_UNIT_DIP size))

(defn set-text-max-length
  [view max-length]
  (->> [(android.text.InputFilter$LengthFilter. max-length)]
       (into-array android.text.InputFilter)
       (.setFilters view)))
