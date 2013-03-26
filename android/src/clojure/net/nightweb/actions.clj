(ns net.nightweb.actions
  (:use [neko.resource :only [get-resource get-string]]
        [neko.threading :only [on-ui]]
        [neko.find-view :only [find-view]]
        [neko.notify :only [toast]]
        [net.clandroid.activity :only [set-state get-state]]
        [net.clandroid.service :only [send-broadcast]]
        [net.nightweb.utils :only [full-size
                                   thumb-size
                                   get-resample-ratio
                                   uri-to-bitmap
                                   path-to-bitmap
                                   bitmap-to-byte-array]]
        [nightweb.router :only [create-meta-torrent]]
        [nightweb.io :only [read-file
                            get-files-in-uri
                            write-pic-file
                            write-post-file
                            write-profile-file
                            write-fav-file
                            delete-orphaned-pics]]
        [nightweb.db :only [insert-post
                            insert-profile
                            insert-fav]]
        [nightweb.formats :only [b-decode
                                 b-decode-map
                                 base32-encode
                                 url-encode
                                 post-encode
                                 profile-encode
                                 fav-encode
                                 remove-dupes-and-nils]]
        [nightweb.torrents-dht :only [add-user-hash]]
        [nightweb.zip :only [zip-dir unzip-dir]]
        [nightweb.constants :only [slash
                                   my-hash-bytes
                                   my-hash-str
                                   get-user-dir
                                   user-zip-file]]))

(defn share-url
  [context]
  (let [intent (android.content.Intent. android.content.Intent/ACTION_SEND)
        url (url-encode (get-state context :share))]
    (.setType intent "text/plain")
    (.putExtra intent android.content.Intent/EXTRA_TEXT url)
    (.startActivity context intent)))

(defn send-file
  [context file-type path]
  (let [intent (android.content.Intent. android.content.Intent/ACTION_SEND)
        uri (android.net.Uri/fromFile (java.io.File. path))]
    (.putExtra intent android.content.Intent/EXTRA_STREAM uri)
    (.setType intent file-type)
    (->> (android.content.Intent/createChooser intent (get-string :save))
         (.startActivity context))))

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
  (case request-code
    1 (let [callback (get-state context :file-request)
            data-result (when intent (.getData intent))]
        (when (and callback data-result)
          (callback data-result)))
    nil))

(defn receive-attachments
  [context uri]
  (let [uri-str (.toString uri)
        attachments (get-state context :attachments)
        new-attachments (if (.startsWith uri-str "file://")
                          (for [path (get-files-in-uri uri-str)]
                            (when (path-to-bitmap path thumb-size) path))
                          (when (.startsWith uri-str "content://")
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

(defn show-spinner
  [context message func]
  (on-ui
    (let [spinner (android.app.ProgressDialog/show context nil message true)]
      (future
        (let [should-refresh? (func)]
          (on-ui
            (.dismiss spinner)
            (when should-refresh? (.recreate context))))))))

(defn show-categories
  [context content]
  (show-page context "net.nightweb.CategoryPage" content))

(defn show-gallery
  [context content]
  (show-page context "net.nightweb.GalleryPage" content))

(defn show-basic
  [context content]
  (show-page context "net.nightweb.BasicPage" content))

(defn show-home
  [context content]
  (show-page context "net.nightweb.MainPage" content))

(defn send-post
  ([context dialog-view button-view]
   (send-post context dialog-view button-view nil nil 1))
  ([context dialog-view button-view create-time pic-hashes status]
   (let [text-view (.findViewWithTag dialog-view "post-body")
         text (.toString (.getText text-view))
         attachments (get-state context :attachments)]
     (show-spinner context
                   (get-string :sending)
                   #(let [is-new? (nil? create-time)
                          create-time (or create-time
                                          (.getTime (java.util.Date.)))
                          pic-hashes
                          (or pic-hashes
                              (for [path attachments]
                                (-> (if (.startsWith path "content://")
                                      (uri-to-bitmap context path)
                                      (path-to-bitmap path full-size))
                                    (bitmap-to-byte-array)
                                    (write-pic-file))))
                          post (post-encode create-time text pic-hashes status)]
                      (insert-post my-hash-bytes
                                   create-time
                                   (b-decode-map (b-decode post)))
                      (delete-orphaned-pics my-hash-bytes)
                      (write-post-file create-time post)
                      (future (create-meta-torrent))
                      (if-not is-new?
                        (show-home context {})
                        true))))
   true))

(defn attach-to-post
  [context dialog-view button-view]
  (request-files context
                 "image/*"
                 (fn [uri]
                   (let [total-count (count (receive-attachments context uri))
                         text (str (get-string :attach_pics)
                                   " (" total-count ")")]
                     (on-ui (.setText button-view text)))))
  false)

(defn cancel
  [context dialog-view button-view]
  true)

(defn save-profile
  [context dialog-view button-view]
  (let [name-field (.findViewWithTag dialog-view "profile-title")
        body-field (.findViewWithTag dialog-view "profile-body")
        image-view (.findViewWithTag dialog-view "profile-image")
        name-text (.toString (.getText name-field))
        body-text (.toString (.getText body-field))
        image-bitmap (when-let [drawable (.getDrawable image-view)]
                       (.getBitmap drawable))]
    (show-spinner context
                  (get-string :saving)
                  #(let [image-barray (bitmap-to-byte-array image-bitmap)
                         img-hash (write-pic-file image-barray)
                         profile (profile-encode name-text body-text img-hash)]
                     (insert-profile my-hash-bytes
                                     (b-decode-map (b-decode profile)))
                     (delete-orphaned-pics my-hash-bytes)
                     (write-profile-file profile)
                     (future (create-meta-torrent))
                     true)))
  true)

(defn zip-and-send
  [context password]
  (let [path (get-user-dir my-hash-str)
        ext-dir (android.os.Environment/getExternalStorageDirectory)
        dest-path (str (.getAbsolutePath ext-dir) slash user-zip-file)]
    (show-spinner context
                  (get-string :saving)
                  #(if (zip-dir path dest-path password)
                     (send-file context "application/zip" dest-path)
                     (toast (get-string :zip_error))))))

(defn menu-action
  [context item]
  (when (= (.getItemId item) (get-resource :id :android/home))
    (show-home context {})))

(defn toggle-fav
  ([context content] (toggle-fav context content false))
  ([context content go-home?]
   (show-spinner context
                 (if (= 1 (get content :status))
                   (get-string :removing)
                   (get-string :adding))
                 #(let [fav-time (or (get content :time)
                                     (.getTime (java.util.Date.)))
                        ptr-hash (get content :userhash)
                        ptr-time (get content :ptrtime)
                        new-status (if (= 1 (get content :status)) 0 1)
                        fav (fav-encode ptr-hash ptr-time new-status)]
                    (insert-fav my-hash-bytes
                                fav-time
                                (b-decode-map (b-decode fav)))
                    (write-fav-file fav-time fav)
                    (add-user-hash ptr-hash)
                    (future (create-meta-torrent))
                    (if go-home?
                      (show-home context {})
                      true)))))

(defn tile-action
  [context item]
  (when-let [func (case (get item :type)
                    :fav show-categories
                    :toggle-fav toggle-fav
                    :search show-categories
                    :pic show-gallery
                    :custom-func (get item :func)
                    show-basic)]
    (func context item)))
