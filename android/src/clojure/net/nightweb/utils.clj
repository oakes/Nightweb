(ns net.nightweb.utils
  (:use [markdown.core :only [md-to-html-string]]
        [clojure.java.io :only [input-stream file copy]]
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
        (int (max height-ratio width-ratio))
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
    (android.text.Selection/setSelection text 0))
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
        markdown-text (android.text.Html/fromHtml html-text)
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
