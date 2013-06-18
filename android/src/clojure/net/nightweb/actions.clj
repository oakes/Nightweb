(ns net.nightweb.actions
  (:require [clojure.java.io :as java.io]
            [neko.resource :as r]
            [neko.threading :as thread]
            [neko.notify :as notify]
            [net.clandroid.activity :as activity]
            [net.nightweb.utils :as utils]
            [nightweb.actions :as a]
            [nightweb.constants :as c]
            [nightweb.io :as io]
            [nightweb.formats :as f]))

(defn share-url
  "Displays an app chooser to share a link to the displayed content."
  [context]
  (let [intent (android.content.Intent. android.content.Intent/ACTION_SEND)
        url (f/url-encode (activity/get-state context :share))]
    (.setType intent "text/plain")
    (.putExtra intent android.content.Intent/EXTRA_TEXT url)
    (.startActivity context intent)))

(defn send-file
  "Displays an app chooser to send a file."
  [context file-type path]
  (let [intent (android.content.Intent. android.content.Intent/ACTION_SEND)
        uri (android.net.Uri/fromFile (java.io/file path))]
    (.putExtra intent android.content.Intent/EXTRA_STREAM uri)
    (.setType intent file-type)
    (->> (android.content.Intent/createChooser intent (r/get-string :save))
         (.startActivity context))))

(defn request-files
  "Displays an app chooser to select files of the specified file type."
  [context file-type callback]
  (activity/set-state context :file-request callback)
  (let [intent (android.content.Intent.
                 android.content.Intent/ACTION_GET_CONTENT)]
    (.setType intent file-type)
    (.addCategory intent android.content.Intent/CATEGORY_OPENABLE)
    (.startActivityForResult context intent 1)))

(defn receive-result
  "Runs after a request returns with a result."
  [context request-code result-code intent]
  (case request-code
    1 (let [callback (activity/get-state context :file-request)
            data-result (when intent (.getData intent))]
        (when (and callback data-result)
          (callback data-result)))
    nil))

(defn receive-attachments
  "Stores a list of selected attachments."
  [context uri]
  (let [uri-str (.toString uri)
        attachments (activity/get-state context :attachments)
        new-attachments (if (.startsWith uri-str "file://")
                          (for [path (io/get-files-in-uri uri-str)]
                            (when (utils/path-to-bitmap path utils/thumb-size)
                              path))
                          (when (.startsWith uri-str "content://")
                            [uri-str]))
        total-attachments (f/remove-dupes-and-nils
                            (concat attachments new-attachments))]
    (activity/set-state context :attachments total-attachments)
    total-attachments))

(defn clear-attachments
  "Clears the list of selected attachments."
  [context]
  (activity/set-state context :attachments nil))

(defn show-spinner
  "Displays a spinner while the specified function runs in a thread."
  [context message func]
  (thread/on-ui
    (let [spinner (android.app.ProgressDialog/show context nil message true)]
      (future
        (let [should-refresh? (func)]
          (thread/on-ui
            (try
              (.dismiss spinner)
              (when should-refresh? (.recreate context))
              (catch Exception e nil))))))))

(defn new-post
  "Saves a post to the disk and creates a new meta torrent to share it."
  ([context dialog-view button-view]
   (new-post context dialog-view button-view nil nil 1))
  ([context dialog-view button-view create-time pic-hashes status]
   (let [text-view (.findViewWithTag dialog-view "post-body")
         text (.toString (.getText text-view))
         attachments (activity/get-state context :attachments)
         pointers (.getTag dialog-view)]
     (show-spinner context
                   (r/get-string :sending)
                   #(do
                      (a/new-post
                        {:pic-hashes
                         (or pic-hashes
                             (for [path attachments]
                               (-> (if (.startsWith path "content://")
                                     (utils/uri-to-bitmap context path)
                                     (utils/path-to-bitmap
                                       path utils/full-size))
                                   utils/bitmap-to-byte-array
                                   io/write-pic-file)))
                         :status status
                         :body-str text
                         :ptr-hash (:ptrhash pointers)
                         :ptr-time (:ptrtime pointers)
                         :create-time create-time})
                      (if-not (nil? create-time)
                        (utils/show-home context {})
                        true))))
   true))

(defn attach-to-post
  "Initiates the action to select images to attach to a post."
  [context dialog-view button-view]
  (request-files context
                 "image/*"
                 (fn [uri]
                   (let [total-count (count (receive-attachments context uri))
                         text (str (r/get-string :attach_pics)
                                   " (" total-count ")")]
                     (thread/on-ui (.setText button-view text)))))
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
                  (r/get-string :saving)
                  #(do
                     (a/save-profile
                       {:pic-hash (-> image-bitmap
                                      utils/bitmap-to-byte-array
                                      io/write-pic-file)
                        :name-str name-text
                        :body-str body-text})
                     true)))
  true)

(defn zip-and-send
  "Creates an encrypted zip file with our content and sends it somewhere."
  [context password]
  (show-spinner context
                (r/get-string :zipping)
                #(let [path (c/get-user-dir @c/my-hash-str)
                       dir (android.os.Environment/getExternalStorageDirectory)
                       dest-path (-> (.getAbsolutePath dir)
                                     (str c/slash c/user-zip-file))]
                   (if (a/export-user
                         {:dest-str dest-path
                          :pass-str password})
                     (send-file context "application/zip" dest-path)
                     (thread/on-ui
                       (notify/toast (r/get-string :zip_error)))))))

(defn unzip-and-save
  "Unzips an encrypted zip file and replaces the current user with it."
  [context password uri-str]
  (show-spinner context
                (r/get-string :unzipping)
                #(let [dir (android.os.Environment/getExternalStorageDirectory)
                       temp-path (-> (.getAbsolutePath dir)
                                     (str c/slash c/user-zip-file))
                       ; if it's a content URI, copy to root of SD card
                       path (if (.startsWith uri-str "content://")
                              (do (utils/copy-uri-to-path context
                                                          uri-str
                                                          temp-path)
                                  temp-path)
                              (.getRawPath (java.net.URI. uri-str)))]
                   ; if unzip succeeds, import user, otherwise show error
                   (if-let [error (a/import-user {:source-str path
                                                  :pass-str password})]
                     (thread/on-ui
                       (notify/toast
                         (utils/get-string-at-runtime context error)))
                     true))))

(defn menu-action
  "Provides an action for menu items in the action bar."
  [context item]
  (when (= (.getItemId item) (r/get-resource :id :android/home))
    (utils/show-home context {})))

(defn toggle-fav
  "Toggles our favorite status for the specified content."
  ([context content] (toggle-fav context content false))
  ([context content go-home?]
   (show-spinner context
                 (if (= 1 (:status content))
                   (r/get-string :removing)
                   (r/get-string :adding))
                 #(do
                    (a/toggle-fav {:ptr-hash (:userhash content)
                                   :ptr-time (:time content)})
                    (if go-home?
                      (utils/show-home context {})
                      true)))))

(defn do-action
  "Provides a central place to associate types with the appropriate actions."
  [context item]
  (when-let [func (case (:type item)
                    :fav utils/show-categories
                    :toggle-fav toggle-fav
                    :search utils/show-categories
                    :pic utils/show-gallery
                    :custom-func (:func item)
                    utils/show-basic)]
    (func context item)))
