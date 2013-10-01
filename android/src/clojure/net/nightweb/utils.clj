(ns net.nightweb.utils
  (:require [markdown.core :as markdown]
            [clojure.java.io :as java.io]
            [neko.resource :as r]
            [nightweb.constants :as c]
            [nightweb.formats :as f])
  (:import [android.app Activity]
           [android.graphics Bitmap BitmapFactory Point]
           [android.graphics.drawable BitmapDrawable StateListDrawable]
           [android.net Uri]
           [android.text Html InputFilter Selection]
           [android.util TypedValue]
           [android.view View]
           [android.widget TextView]))

(def ^:const full-size 1024)
(def ^:const thumb-size 256)
(def ^:const default-text-size 20)
(def ^:const large-text-size 30)

(defn get-resample-ratio
  "Provides a ratio to resize an image at if it exceeds max-length."
  [is max-length]
  (let [options (android.graphics.BitmapFactory$Options.)
        _ (set! (. options inJustDecodeBounds) true)
        _ (BitmapFactory/decodeStream is nil options)
        height-ratio (/ (. options outHeight) max-length)
        width-ratio (/ (. options outWidth) max-length)
        is-valid? (and (> height-ratio 0) (> width-ratio 0))
        is-too-big? (or (> height-ratio 1) (> width-ratio 1))]
    (when is-valid?
      (if is-too-big?
        (int (Math/ceil (max height-ratio width-ratio)))
        1))))

(defn input-stream-to-bitmap
  "Reads from an input stream into a bitmap."
  [is ratio]
  (let [options (android.graphics.BitmapFactory$Options.)]
    (set! (. options inSampleSize) ratio)
    (BitmapFactory/decodeStream is nil options)))

(defn uri-to-bitmap
  "Reads from a URI into a bitmap, resizing if necessary."
  [^Activity context uri-str]
  (try
    (let [cr (.getContentResolver context)
          uri (Uri/parse uri-str)]
      (when-let [ratio (with-open [is (.openInputStream cr uri)]
                         (get-resample-ratio is full-size))]
        (with-open [is (.openInputStream cr uri)]
          (input-stream-to-bitmap is ratio))))
    (catch Exception e nil)))

(defn path-to-bitmap
  "Reads from a file path into a bitmap, resizing if necessary."
  [path max-length]
  (try
    (when-let [ratio (with-open [is (java.io/input-stream path)]
                       (get-resample-ratio is max-length))]
      (with-open [is (java.io/input-stream path)]
        (input-stream-to-bitmap is ratio)))
    (catch Exception e nil)))

(defn bitmap-to-byte-array
  "Compresses a bitmap into a specific image format as a byte array."
  [^Bitmap image-bitmap]
  (when image-bitmap
    (let [out (java.io.ByteArrayOutputStream.)
          image-format android.graphics.Bitmap$CompressFormat/WEBP]
      (.compress image-bitmap image-format 90 out)
      (.toByteArray out))))

(defn copy-uri-to-path
  [^Activity context uri-str path]
  "Copies the contents of a URI to a path."
  (try
    (let [cr (.getContentResolver context)
          uri (Uri/parse uri-str)]
      (with-open [is (.openInputStream cr uri)]
        (java.io/copy is (java.io/file path))))
    (catch Exception e nil)))

(defn get-pic-path
  "Gets the full path for the given user and image hash combination."
  [user-hash-bytes image-hash-bytes]
  (when (and user-hash-bytes image-hash-bytes)
    (-> (c/get-pic-dir (f/base32-encode user-hash-bytes))
        (java.io/file (f/base32-encode image-hash-bytes))
        .getCanonicalPath)))

(defn create-highlight
  "Creates a Drawable that highlights when pressed."
  ([^Activity context]
   (create-highlight context nil))
  ([^Activity context drawable]
   (let [states (StateListDrawable.)
         blue (->> (r/get-resource :color :android/holo_blue_light)
                   (.getDrawable (.getResources context)))
         transparent (->> (r/get-resource :color :android/transparent)
                          (.getDrawable (.getResources context)))
         pressed (r/get-resource :attr :android/state_pressed)
         selected (r/get-resource :attr :android/state_selected)]
     (.addState states (int-array [pressed]) blue)
     (.addState states (int-array [selected]) transparent)
     (when drawable (.addState states (int-array []) drawable))
     states)))

(defn create-tile-image
  "Creates a Drawable ready to set in a tile."
  [^Activity context user-hash pic-hash]
  (create-highlight context
                    (when pic-hash
                      (-> (get-pic-path user-hash pic-hash)
                          ^Bitmap (path-to-bitmap thumb-size)
                          BitmapDrawable.))))

(defn make-dip
  "Converts the given number into density-independent pixels."
  [^Activity context number]
  (-> (.getResources context)
      .getDisplayMetrics
      .density
      (* number)
      int))

(defn get-screen-width
  "Gets the width of the screen."
  [^Activity context]
  (let [point (Point.)]
    (-> (.getWindowManager context)
        .getDefaultDisplay
        (.getSize point))
    (. point x)))

(defn set-text-size
  "Sets the given view's text size in density-independent pixels."
  [^TextView view ^double size]
  (.setTextSize view TypedValue/COMPLEX_UNIT_DIP size))

(defn set-text-max-length
  "Limits the text length for the given TextView."
  [^TextView view max-length]
  (->> [(android.text.InputFilter$LengthFilter. max-length)]
       (into-array InputFilter)
       (.setFilters view)))

(do
  ; subclass LinkMovementMethod because it doesn't support text selection
  (gen-class
    :name "net.nightweb.utils.MovementMethod"
    :extends android.text.method.LinkMovementMethod
    :prefix "movement-method-"
    :exposes-methods {canSelectArbitrarily superCanSelectArbitrarily
                      initialize superInitialize
                      onTakeFocus superOnTakeFocus
                      getInstance superGetInstance})
  (defn movement-method-canSelectArbitrarily
    [this]
    true)
  (defn movement-method-initialize
    [this widget text]
    (try
      (Selection/setSelection text 0)
      (catch Exception e nil)))
  (defn movement-method-onTakeFocus
    [this view text dir]
    (if (->> (bit-or View/FOCUS_FORWARD View/FOCUS_DOWN)
             (bit-and dir)
             (not= 0))
      (when (nil? (.getLayout view))
        (Selection/setSelection text (.length text)))
      (Selection/setSelection text (.length text)))))

(defn set-text-content
  "Sets the content of a TextView and formats it if necessary."
  [^Activity context ^TextView view on-tap content]
  (let [html-text (markdown/md-to-html-string content)
        markdown-text (try (Html/fromHtml html-text)
                        (catch Exception e ""))
        spannable android.widget.TextView$BufferType/SPANNABLE]
    (.setText view markdown-text spannable)
    (.setMovementMethod view (net.nightweb.utils.MovementMethod.))
    (doseq [old-span (.getUrls view)]
      (let [text (.getText view)
            start (.getSpanStart text old-span)
            end (.getSpanEnd text old-span)
            new-span (proxy [android.text.style.ClickableSpan] []
                       (onClick [widget]
                         (when-let [params (f/url-decode (.getURL old-span))]
                           (on-tap context params))))]
        (.removeSpan text old-span)
        (.setSpan text new-span start end 0)))))

(defn get-resource-at-runtime
  "Gets the specified resource at runtime."
  [^Activity context res-type res-name]
  (.getIdentifier (.getResources context)
                  (name res-name)
                  (name res-type)
                  (.getPackageName context)))

(defn get-drawable-at-runtime
  "Gets the specified drawable at runtime."
  [^Activity context res-name]
  (get-resource-at-runtime context :drawable res-name))

(defn get-string-at-runtime
  "Gets the specified string at runtime."
  [^Activity context res-name]
  (if (keyword? res-name)
    (.getString context (get-resource-at-runtime context :string res-name))
    res-name))

(defn set-state
  "Sets the given key/value pair to the given activity's state."
  [context content-key content-val]
  (swap! (.state context) assoc content-key content-val))

(defn get-state
  "Gets the value for the given key in the given activity's state."
  [context content-key]
  (get @(.state context) content-key))
