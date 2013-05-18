(ns net.nightweb.utils
  (:use [markdown.core :only [md-to-html-string]]
        [clojure.java.io :only [input-stream file copy]]
        [neko.resource :only [get-resource]]
        [neko.threading :only [on-ui]]
        [nightweb.formats :only [base32-encode
                                 url-decode]]
        [nightweb.constants :only [slash
                                   get-pic-dir]]))

(def full-size 1024)
(def thumb-size 256)

(defn get-resample-ratio
  "Provides a ratio to resize an image at if it exceeds max-length."
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
        (int (java.lang.Math/ceil (max height-ratio width-ratio)))
        1))))

(defn input-stream-to-bitmap
  "Reads from an input stream into a bitmap."
  [is ratio]
  (let [options (android.graphics.BitmapFactory$Options.)]
    (set! (. options inSampleSize) ratio)
    (android.graphics.BitmapFactory/decodeStream is nil options)))

(defn uri-to-bitmap
  "Reads from a URI into a bitmap, resizing if necessary."
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
  "Reads from a file path into a bitmap, resizing if necessary."
  [path max-length]
  (try
    (when-let [ratio (with-open [is (input-stream path)]
                       (get-resample-ratio is max-length))]
      (with-open [is (input-stream path)]
        (input-stream-to-bitmap is ratio)))
    (catch java.lang.Exception e nil)))

(defn bitmap-to-byte-array
  "Compresses a bitmap into a specific image format as a byte array."
  [image-bitmap]
  (when image-bitmap
    (let [out (java.io.ByteArrayOutputStream.)
          image-format android.graphics.Bitmap$CompressFormat/WEBP]
      (.compress image-bitmap image-format 90 out)
      (.toByteArray out))))

(defn copy-uri-to-path
  [context uri-str path]
  "Copies the contents of a URI to a path."
  (try
    (let [cr (.getContentResolver context)
          uri (android.net.Uri/parse uri-str)]
      (with-open [is (.openInputStream cr uri)]
        (copy is (file path))))
    (catch java.lang.Exception e nil)))

(defn get-pic-path
  "Gets the full path for the given user and image hash combination."
  [user-hash-bytes image-hash-bytes]
  (when (and user-hash-bytes image-hash-bytes)
    (str (get-pic-dir (base32-encode user-hash-bytes))
         slash
         (base32-encode image-hash-bytes))))

(defn create-highlight
  "Creates a Drawable that highlights when pressed."
  ([context] (create-highlight context nil))
  ([context drawable]
   (let [states (android.graphics.drawable.StateListDrawable.)
         blue (->> (get-resource :color :android/holo_blue_light)
                   (.getDrawable (.getResources context)))
         transparent (->> (get-resource :color :android/transparent)
                          (.getDrawable (.getResources context)))
         pressed (get-resource :attr :android/state_pressed)
         selected (get-resource :attr :android/state_selected)]
     (.addState states (int-array [pressed]) blue)
     (.addState states (int-array [selected]) transparent)
     (when drawable (.addState states (int-array []) drawable))
     states)))

(defn create-tile-image
  "Creates a Drawable ready to set in a tile."
  [context user-hash pic-hash]
  (create-highlight context
                    (when pic-hash
                      (-> (get-pic-path user-hash pic-hash)
                          (path-to-bitmap thumb-size)
                          (android.graphics.drawable.BitmapDrawable.)))))

(defn make-dip
  "Converts the given number into density-independent pixels."
  [context number]
  (-> (.getResources context)
      (.getDisplayMetrics)
      (.density)
      (* number)
      (int)))

(defn show-page
  "Shows a new activity of the specified type."
  [context class-name params]
  (let [class-symbol (java.lang.Class/forName class-name)
        intent (android.content.Intent. context class-symbol)]
    (.putExtra intent "params" params)
    (.startActivity context intent)))

(defn show-categories
  "Shows the Category page."
  [context content]
  (show-page context "net.nightweb.CategoryPage" content))

(defn show-gallery
  "Shows the Gallery page."
  [context content]
  (show-page context "net.nightweb.GalleryPage" content))

(defn show-basic
  "Shows the Basic page."
  [context content]
  (show-page context "net.nightweb.BasicPage" content))

(defn show-home
  "Shows the Main page."
  [context content]
  (show-page context "net.nightweb.MainPage" content))

(def default-text-size 20)
(def large-text-size 30)

(defn set-text-size
  "Sets the given view's text size in density-independent pixels."
  [view size]
  (.setTextSize view android.util.TypedValue/COMPLEX_UNIT_DIP size))

(defn set-text-max-length
  "Limits the text length for the given TextView."
  [view max-length]
  (->> [(android.text.InputFilter$LengthFilter. max-length)]
       (into-array android.text.InputFilter)
       (.setFilters view)))

; subclass LinkMovementMethod because it doesn't support text selection
(do
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
      (android.text.Selection/setSelection text 0)
      (catch java.lang.Exception e nil)))
  (defn movement-method-onTakeFocus
    [this view text dir]
    (if (->> (bit-or android.view.View/FOCUS_FORWARD
                     android.view.View/FOCUS_DOWN)
             (bit-and dir)
             (not= 0))
      (when (nil? (.getLayout view))
        (android.text.Selection/setSelection text (.length text)))
      (android.text.Selection/setSelection text (.length text)))))

(defn set-text-content
  "Sets the content of a TextView and formats it if necessary."
  [context view content]
  (let [html-text (md-to-html-string content)
        markdown-text (try
                        (android.text.Html/fromHtml html-text)
                        (catch java.lang.Exception e ""))
        spannable android.widget.TextView$BufferType/SPANNABLE]
    (.setText view markdown-text spannable)
    (.setMovementMethod view (net.nightweb.utils.MovementMethod.))
    (doseq [old-span (.getUrls view)]
      (let [text (.getText view)
            start (.getSpanStart text old-span)
            end (.getSpanEnd text old-span)
            new-span (proxy [android.text.style.ClickableSpan] []
                       (onClick [widget]
                         (when-let [params (url-decode (.getURL old-span))]
                           (show-basic context params))))]
        (.removeSpan text old-span)
        (.setSpan text new-span start end 0)))))

(defn get-resource-at-runtime
  [context res-type res-name]
  (.getIdentifier (.getResources context)
                  (name res-name)
                  (name res-type)
                  (.getPackageName context)))

(defn get-drawable-at-runtime
  [context res-name]
  (get-resource-at-runtime context :drawable res-name))

(defn get-string-at-runtime
  [context res-name]
  (if (keyword? res-name)
    (.getString context (get-resource-at-runtime context :string res-name))
    res-name))
