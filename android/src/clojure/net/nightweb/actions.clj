(ns net.nightweb.actions
  (:use [neko.resource :only [get-resource get-string]]
        [neko.threading :only [on-ui]]
        [neko.find-view :only [find-view]]
        [net.clandroid.activity :only [set-state get-state]]
        [net.clandroid.service :only [send-broadcast]]
        [net.nightweb.main :only [download-receiver-name]]
        [nightweb.router :only [create-meta-torrent]]
        [nightweb.io :only [read-file
                            get-files-in-uri
                            write-pic-file
                            write-post-file
                            write-profile-file]]
        [nightweb.db :only [insert-post
                            insert-profile]]
        [nightweb.formats :only [b-decode
                                 b-decode-map
                                 base32-encode
                                 url-encode
                                 post-encode
                                 profile-encode
                                 remove-dupes-and-nils]]
        [nightweb.torrents :only [is-connecting?]]
        [nightweb.constants :only [my-hash-bytes]]))

(defn uri-to-bitmap
  [context uri-str]
  (try
    (let [cr (.getContentResolver context)
          uri (android.net.Uri/parse uri-str)]
      (android.provider.MediaStore$Images$Media/getBitmap cr uri))
    (catch java.lang.Exception e nil)))

(defn byte-array-to-bitmap
  [ba]
  (if ba
    (try
      (android.graphics.BitmapFactory/decodeByteArray ba 0 (alength ba))
      (catch java.lang.Exception e nil))))

(defn bitmap-to-byte-array
  [image-bitmap]
  (if image-bitmap
    (let [out (java.io.ByteArrayOutputStream.)
          image-format android.graphics.Bitmap$CompressFormat/WEBP]
      (.compress image-bitmap image-format 90 out)
      (.toByteArray out))))

(defn share-url
  [context]
  (let [intent (android.content.Intent. android.content.Intent/ACTION_SEND)
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
  [context uri]
  (let [uri-str (.toString uri)
        attachments (get-state context :attachments)
        new-attachments (if (.startsWith uri-str "file://")
                          (for [path (get-files-in-uri uri-str)]
                            (if (byte-array-to-bitmap (read-file path)) path))
                          (if (.startsWith uri-str "content://")
                            [uri-str]))
        total-attachments (remove-dupes-and-nils
                            (concat attachments new-attachments))]
    (set-state context :attachments total-attachments)
    total-attachments))

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
  ([context title message]
   (let [builder (android.app.AlertDialog$Builder. context)]
     (.setPositiveButton builder (get-string :ok) nil)
     (let [dialog (.create builder)]
       (.setTitle dialog title)
       (.setMessage dialog message)
       (.setCanceledOnTouchOutside dialog false)
       (.show dialog))))
  ([context message view buttons]
   (let [builder (android.app.AlertDialog$Builder. context)]
     (if-let [positive-name (get buttons :positive-name)]
       (.setPositiveButton builder positive-name nil))
     (if-let [neutral-name (get buttons :neutral-name)]
       (.setNeutralButton builder neutral-name nil))
     (if-let [negative-name (get buttons :negative-name)]
       (.setNegativeButton builder negative-name nil))
     (.setMessage builder message)
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

(defn show-new-user-dialog
  [context params]
  (show-dialog context
               (get-string :new_user)
               nil
               {:positive-name (get-string :download_user)
                :positive-func
                (fn [context dialog-view button-view]
                  (send-broadcast context params download-receiver-name)
                  (show-page context "net.nightweb.MainPage" params))
                :negative-name (get-string :cancel)
                :negative-func
                (fn [context dialog-view button-view]
                  (.finish context))}))

(defn show-pending-user-dialog
  [context]
  (show-dialog context nil (get-string :pending_user)))

(defn show-lost-post-dialog
  [context]
  (show-dialog context
               (get-string :lost_post)
               nil
               {:positive-name (get-string :ok)
                :positive-func
                (fn [context dialog-view button-view]
                  (.finish context))}))

(defn show-welcome-dialog
  [context]
  (show-dialog context
               (get-string :welcome_title)
               (get-string :welcome_message)))

(defn show-spinner
  [context message func]
  (on-ui
    (let [spinner (android.app.ProgressDialog/show context nil message true)]
      (future
        (func)
        (on-ui
          (.dismiss spinner)
          (.recreate context))))))

(defn show-categories
  [context content]
  (show-page context "net.nightweb.CategoryPage" content))

(defn show-gallery
  [context content]
  (show-page context "net.nightweb.GalleryPage" content))

(defn show-basic
  [context content]
  (show-page context "net.nightweb.BasicPage" content))

(defn do-send-new-post
  [context dialog-view button-view]
  (let [text-view (.findViewWithTag dialog-view "post-body")
        text (.toString (.getText text-view))
        attachments (get-state context :attachments)]
    (show-spinner context
                  (get-string :sending)
                  #(let [create-time (.getTime (java.util.Date.))
                         pic-hashes (for [path attachments]
                                      (-> (if (.startsWith path "content://")
                                            (uri-to-bitmap context path)
                                            (byte-array-to-bitmap
                                              (read-file path)))
                                          (bitmap-to-byte-array)
                                          (write-pic-file)))
                         post (post-encode create-time text pic-hashes)]
                     (insert-post my-hash-bytes
                                  create-time
                                  (b-decode-map (b-decode post)))
                     (future
                       (if (is-connecting?)
                         (create-meta-torrent))
                       (write-post-file create-time post)
                       (create-meta-torrent)))))
  true)

(defn do-attach-to-new-post
  [context dialog-view button-view]
  (request-files context
                 "image/*"
                 (fn [uri]
                   (let [total-count (count (receive-attachments context uri))
                         text (str (get-string :attach_pics)
                                   " (" total-count ")")]
                     (on-ui (.setText button-view text)))))
  false)

(defn do-save-profile
  [context dialog-view button-view]
  (let [name-field (.findViewWithTag dialog-view "profile-title")
        body-field (.findViewWithTag dialog-view "profile-body")
        image-view (.findViewWithTag dialog-view "profile-image")
        name-text (.toString (.getText name-field))
        body-text (.toString (.getText body-field))
        image-bitmap (if-let [drawable (.getDrawable image-view)]
                       (.getBitmap drawable))
        image-barray (bitmap-to-byte-array image-bitmap)]
    (show-spinner context
                  (get-string :saving)
                  #(let [img-hash (write-pic-file image-barray)
                         profile (profile-encode name-text body-text img-hash)]
                     (insert-profile my-hash-bytes
                                     (b-decode-map (b-decode profile)))
                     (future
                       (if (is-connecting?)
                         (create-meta-torrent))
                       (write-profile-file profile)
                       (create-meta-torrent)))))
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
                  :fav show-categories
                  :search show-categories
                  :pic show-gallery
                  :custom-func (get item :func)
                  show-basic)]
    (func context item)))
