(ns net.nightweb.actions
  (:use [neko.resource :only [get-resource get-string]]
        [neko.threading :only [on-ui]]
        [neko.find-view :only [find-view]]
        [net.nightweb.clandroid.activity :only [set-state get-state]]
        [nightweb.router :only [create-meta-torrent]]
        [nightweb.io :only [list-files-in-uri
                            write-post-file
                            write-profile-file
                            write-internal-file]]
        [nightweb.formats :only [base32-encode
                                 url-encode]]))

(defn share-url
  [context]
  (let [intent (android.content.Intent.
                 android.content.Intent/ACTION_SEND)
        url (url-encode (get-state context :share))]
    (.setType intent "text/plain")
    (.putExtra intent android.content.Intent/EXTRA_TEXT url)
    (.startActivity context intent)))

(defn request-files
  [context file-type callback]
  (set-state context :file-request callback)
  (let [intent (android.content.Intent.
                 android.content.Intent/ACTION_GET_CONTENT)]
    (.setType intent file-type)
    (.addCategory intent android.content.Intent/CATEGORY_OPENABLE)
    (.startActivityForResult context intent 1)))

(defn receive-result
  [context request-code result-code intent]
  (let [callback (get-state context :file-request)
        data-result (if intent (.getData intent))]
    (if (and callback data-result)
      (callback data-result))))

(defn receive-attachments
  [context uri button-view]
  (let [uri-str (.toString uri)
        new-attachments (if (.startsWith uri-str "file://")
                          (list-files-in-uri uri-str)
                          (if (.startsWith uri-str "content://")
                            #{uri-str}))
        attachments (set (concat (get-state context :attachments)
                                 new-attachments))]
    (set-state context :attachments attachments)
    (.setText button-view (str (get-string :attach)
                               " (" (count attachments) ")"))))

(defn clear-attachments
  [context]
  (set-state context :attachments nil))

(defn show-page
  [context class-name params]
  (let [class-symbol (java.lang.Class/forName class-name)
        intent (android.content.Intent. context class-symbol)]
    (.putExtra intent "params" params)
    (.startActivity context intent)))

(defn show-dialog
  ([context message]
   (let [builder (android.app.AlertDialog$Builder. context)]
     (.setPositiveButton builder (get-string :ok) nil)
     (let [dialog (.create builder)]
       (.setMessage dialog message)
       (.setCanceledOnTouchOutside dialog false)
       (.show dialog))))
  ([context view buttons]
   (let [builder (android.app.AlertDialog$Builder. context)]
     (if-let [positive-name (get buttons :positive-name)]
       (.setPositiveButton builder positive-name nil))
     (if-let [neutral-name (get buttons :neutral-name)]
       (.setNeutralButton builder neutral-name nil))
     (if-let [negative-name (get buttons :negative-name)]
       (.setNegativeButton builder negative-name nil))
     (.setView builder view)
     (let [dialog (.create builder)
           positive-type android.app.AlertDialog/BUTTON_POSITIVE
           neutral-type android.app.AlertDialog/BUTTON_NEUTRAL
           negative-type android.app.AlertDialog/BUTTON_NEGATIVE
           btn-action (fn [dialog button func]
                        (proxy [android.view.View$OnClickListener] []
                          (onClick [v]
                            (if (func context view button)
                              (.dismiss dialog)))))]
       (.setOnShowListener
         dialog
         (proxy [android.content.DialogInterface$OnShowListener] []
           (onShow [d]
             (if-let [positive-btn (.getButton d positive-type)]
               (.setOnClickListener
                 positive-btn (btn-action d
                                          positive-btn
                                          (get buttons :positive-func))))
             (if-let [neutral-btn (.getButton d neutral-type)]
               (.setOnClickListener
                 neutral-btn (btn-action d
                                         neutral-btn
                                         (get buttons :neutral-func))))
             (if-let [negative-btn (.getButton d negative-type)]
               (.setOnClickListener
                 negative-btn (btn-action d
                                          negative-btn
                                          (get buttons :negative-func)))))))
       (.setCanceledOnTouchOutside dialog false)
       (.show dialog)))))

(defn show-spinner
  [context message func]
  (on-ui
    (let [spinner (android.app.ProgressDialog/show context nil message true)]
      (future
        (func)
        (on-ui
          (.dismiss spinner)
          (.recreate context))))))

(defn show-message
  [context message]
  (show-dialog context))

(defn show-favorites
  [context content]
  (show-page context "net.nightweb.FavoritesPage" (get content :content)))

(defn show-transfers
  [context content]
  (show-page context "net.nightweb.TransfersPage" (get content :content)))

(defn show-basic
  [context content]
  (show-page context "net.nightweb.BasicPage" content))

(defn do-send-new-post
  [context dialog-view button-view]
  (let [text (.toString (.getText dialog-view))]
    (write-post-file text))
  (show-spinner context (get-string :sending) create-meta-torrent)
  true)

(defn do-attach-to-new-post
  [context dialog-view button-view]
  (request-files context
                 "*/*"
                 (fn [uri] (receive-attachments context uri button-view)))
  false)

(defn do-save-profile
  [context dialog-view button-view]
  (let [linear-layout (.getChildAt dialog-view 0)
        name-field (.getChildAt linear-layout 0)
        body-field (.getChildAt linear-layout 1)
        image-view (.getChildAt linear-layout 2)
        name-text (.toString (.getText name-field))
        body-text (.toString (.getText body-field))
        image-bitmap (if-let [drawable (.getDrawable image-view)]
                       (.getBitmap drawable))]
    (write-profile-file name-text body-text image-bitmap))
  (show-spinner context (get-string :saving) create-meta-torrent)
  true)

(defn do-cancel
  [context dialog-view button-view]
  (println "cancel")
  true)

(defn do-menu-action
  [context item]
  (if (= (.getItemId item) (get-resource :id :android/home))
    (show-page context "net.nightweb.MainPage" {})))

(defn do-tile-action
  [context item]
  (if-let [func (case (get item :type)
                  :fav show-favorites
                  :tran show-transfers
                  :custom-func (get item :func)
                  show-basic)]
    (func context item)))
