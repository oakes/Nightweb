(ns net.nightweb.actions
  (:use [clojure.java.io :only [file]]
        [neko.resource :only [get-resource get-string]]
        [neko.threading :only [on-ui]]
        [neko.find-view :only [find-view]]
        [neko.notify :only [toast]]
        [net.clandroid.activity :only [set-state get-state]]
        [net.nightweb.utils :only [full-size
                                   thumb-size
                                   get-resample-ratio
                                   uri-to-bitmap
                                   path-to-bitmap
                                   bitmap-to-byte-array
                                   copy-uri-to-path
                                   show-categories
                                   show-gallery
                                   show-basic
                                   show-home]]
        [nightweb.router :only [create-meta-torrent
                                create-imported-user]]
        [nightweb.io :only [read-file
                            delete-file
                            list-dir
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
        [nightweb.zip :only [zip-dir unzip-dir get-zip-headers]]
        [nightweb.constants :only [slash
                                   my-hash-bytes
                                   my-hash-str
                                   get-user-dir
                                   user-zip-file]]))

(defn share-url
  "Displays an app chooser to share a link to the displayed content."
  [context]
  (let [intent (android.content.Intent. android.content.Intent/ACTION_SEND)
        url (url-encode (get-state context :share))]
    (.setType intent "text/plain")
    (.putExtra intent android.content.Intent/EXTRA_TEXT url)
    (.startActivity context intent)))

(defn send-file
  "Displays an app chooser to send a file."
  [context file-type path]
  (let [intent (android.content.Intent. android.content.Intent/ACTION_SEND)
        uri (android.net.Uri/fromFile (file path))]
    (.putExtra intent android.content.Intent/EXTRA_STREAM uri)
    (.setType intent file-type)
    (->> (android.content.Intent/createChooser intent (get-string :save))
         (.startActivity context))))

(defn request-files
  "Displays an app chooser to select files of the specified file type."
  [context file-type callback]
  (set-state context :file-request callback)
  (let [intent (android.content.Intent.
                 android.content.Intent/ACTION_GET_CONTENT)]
    (.setType intent file-type)
    (.addCategory intent android.content.Intent/CATEGORY_OPENABLE)
    (.startActivityForResult context intent 1)))

(defn receive-result
  "Runs after a request returns with a result."
  [context request-code result-code intent]
  (case request-code
    1 (let [callback (get-state context :file-request)
            data-result (when intent (.getData intent))]
        (when (and callback data-result)
          (callback data-result)))
    nil))

(defn receive-attachments
  "Stores a list of selected attachments."
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
  "Clears the list of selected attachments."
  [context]
  (set-state context :attachments nil))

(defn show-spinner
  "Displays a spinner while the specified function runs in a thread."
  [context message func]
  (on-ui
    (let [spinner (android.app.ProgressDialog/show context nil message true)]
      (future
        (let [should-refresh? (func)]
          (on-ui
            (try
              (.dismiss spinner)
              (when should-refresh? (.recreate context))
              (catch Exception e nil))))))))

(defn send-post
  "Saves a post to the disk and creates a new meta torrent to share it."
  ([context dialog-view button-view]
   (send-post context dialog-view button-view nil nil 1))
  ([context dialog-view button-view create-time pic-hashes status]
   (let [text-view (.findViewWithTag dialog-view "post-body")
         text (.toString (.getText text-view))
         attachments (get-state context :attachments)
         is-new? (nil? create-time)
         create-time (or create-time (.getTime (java.util.Date.)))
         pointers (.getTag dialog-view)]
     (show-spinner context
                   (get-string :sending)
                   #(let [pic-hashes
                          (or pic-hashes
                              (for [path attachments]
                                (-> (if (.startsWith path "content://")
                                      (uri-to-bitmap context path)
                                      (path-to-bitmap path full-size))
                                    (bitmap-to-byte-array)
                                    (write-pic-file))))
                          post (post-encode :text text
                                            :pic-hashes pic-hashes
                                            :status status
                                            :ptrhash (:ptrhash pointers)
                                            :ptrtime (:ptrtime pointers))]
                      (insert-post @my-hash-bytes
                                   create-time
                                   (b-decode-map (b-decode post)))
                      (delete-orphaned-pics @my-hash-bytes)
                      (write-post-file create-time post)
                      (future (create-meta-torrent))
                      (if-not is-new?
                        (show-home context {})
                        true))))
   true))

(defn attach-to-post
  "Initiates the action to select images to attach to a post."
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
  "Used by dialogs to perform no action other than closing themselves."
  [context dialog-view button-view]
  true)

(defn save-profile
  "Saves the profile to the disk and creates a new meta torrent to share it."
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
                     (insert-profile @my-hash-bytes
                                     (b-decode-map (b-decode profile)))
                     (delete-orphaned-pics @my-hash-bytes)
                     (write-profile-file profile)
                     (future (create-meta-torrent))
                     true)))
  true)

(defn select-user
  "Switches to a different user."
  [user]
  (let []))

(defn delete-user
  "Deletes a user."
  [user]
  (let []))

(defn create-user
  "Creates a user."
  [context dialog-view button-view]
  (let []))

(defn zip-and-send
  "Creates an encrypted zip file with our content and sends it somewhere."
  [context password]
  (show-spinner context
                (get-string :zipping)
                #(let [path (get-user-dir @my-hash-str)
                       dir (android.os.Environment/getExternalStorageDirectory)
                       dest-path (-> (.getAbsolutePath dir)
                                     (str slash user-zip-file))]
                   (delete-file dest-path)
                   ; if zip succeeds, show app chooser, otherwise show error
                   (if (zip-dir path dest-path password)
                     (send-file context "application/zip" dest-path)
                     (on-ui (toast (get-string :zip_error)))))))

(defn unzip-and-save
  "Unzips an encrypted zip file and replaces the current user with it."
  [context password uri-str]
  (show-spinner context
                (get-string :unzipping)
                #(let [dir (android.os.Environment/getExternalStorageDirectory)
                       temp-path (-> (.getAbsolutePath dir)
                                     (str slash user-zip-file))
                       ; if it's a content URI, copy to root of SD card
                       path (if (.startsWith uri-str "content://")
                              (do (copy-uri-to-path context uri-str temp-path)
                                  temp-path)
                              (.getRawPath (java.net.URI. uri-str)))
                       dest-path (get-user-dir)]
                   ; if unzip succeeds, import user, otherwise show error
                   (if (unzip-dir path dest-path password)
                     (let [paths (set (get-zip-headers path))
                           new-dirs (-> (fn [d] (contains? paths (str d slash)))
                                        (filter (list-dir dest-path)))]
                       ; if import succeeds, close dialog, otherwise show error
                       (if (create-imported-user new-dirs)
                         true
                         (on-ui (toast (get-string :import_error)))))
                     (on-ui (toast (get-string :unzip_error)))))))

(defn menu-action
  "Provides an action for menu items in the action bar."
  [context item]
  (when (= (.getItemId item) (get-resource :id :android/home))
    (show-home context {})))

(defn toggle-fav
  "Toggles our favorite status for the specified content."
  ([context content] (toggle-fav context content false))
  ([context content go-home?]
   (show-spinner context
                 (if (= 1 (:status content))
                   (get-string :removing)
                   (get-string :adding))
                 #(let [fav-time (or (:time content)
                                     (.getTime (java.util.Date.)))
                        ptr-hash (:userhash content)
                        ptr-time (:ptrtime content)
                        new-status (if (= 1 (:status content)) 0 1)
                        fav (fav-encode ptr-hash ptr-time new-status)]
                    (insert-fav @my-hash-bytes
                                fav-time
                                (b-decode-map (b-decode fav)))
                    (write-fav-file fav-time fav)
                    (add-user-hash ptr-hash)
                    (future (create-meta-torrent))
                    (if go-home?
                      (show-home context {})
                      true)))))

(defn tile-action
  "Provides a central place to associate types with the appropriate actions."
  [context item]
  (when-let [func (case (:type item)
                    :fav show-categories
                    :toggle-fav toggle-fav
                    :search show-categories
                    :pic show-gallery
                    :custom-func (:func item)
                    show-basic)]
    (func context item)))
