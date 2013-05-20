(ns net.nightweb.dialogs
  (:use [neko.resource :only [get-string get-resource]]
        [neko.threading :only [on-ui]]
        [neko.ui :only [make-ui]]
        [neko.ui.mapping :only [set-classname!]]
        [net.clandroid.activity :only [get-state]]
        [net.nightweb.actions :only [clear-attachments
                                     send-post
                                     request-files
                                     toggle-fav
                                     attach-to-post
                                     cancel
                                     zip-and-send
                                     unzip-and-save
                                     save-profile]]
        [net.nightweb.utils :only [full-size
                                   thumb-size
                                   uri-to-bitmap
                                   path-to-bitmap
                                   get-pic-path
                                   make-dip
                                   default-text-size
                                   large-text-size
                                   set-text-size
                                   set-text-max-length
                                   set-text-content]]
        [nightweb.io :only [read-user-list-file]]
        [nightweb.formats :only [tags-encode]]
        [nightweb.db :only [max-length-small
                            max-length-large
                            get-single-user-data]]
        [nightweb.constants :only [is-me?]]))

(set-classname! :scroll-view android.widget.ScrollView)
(set-classname! :frame-layout android.widget.FrameLayout)
(set-classname! :relative-layout android.widget.RelativeLayout)
(set-classname! :image-view android.widget.ImageView)
(set-classname! :radio-button android.widget.RadioButton)
(set-classname! :radio-group android.widget.RadioGroup)
(set-classname! :view-pager android.support.v4.view.ViewPager)

(defn create-dialog
  [context message view buttons]
  (let [builder (android.app.AlertDialog$Builder. context)]
    (when-let [positive-name (:positive-name buttons)]
      (.setPositiveButton builder positive-name nil))
    (when-let [neutral-name (:neutral-name buttons)]
      (.setNeutralButton builder neutral-name nil))
    (when-let [negative-name (:negative-name buttons)]
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
                           (when (func context view button)
                             (try
                               (.dismiss dialog)
                               (catch java.lang.Exception e nil))))))]
      (.setOnShowListener
        dialog
        (proxy [android.content.DialogInterface$OnShowListener] []
          (onShow [d]
            (when-let [positive-btn (.getButton d positive-type)]
              (.setOnClickListener
                positive-btn (btn-action d
                                         positive-btn
                                         (:positive-func buttons))))
            (when-let [neutral-btn (.getButton d neutral-type)]
              (.setOnClickListener
                neutral-btn (btn-action d
                                        neutral-btn
                                        (:neutral-func buttons))))
            (when-let [negative-btn (.getButton d negative-type)]
              (.setOnClickListener
                negative-btn (btn-action d
                                         negative-btn
                                         (:negative-func buttons)))))))
      (.setCanceledOnTouchOutside dialog false)
      dialog)))

(defn show-dialog
  ([context title message]
   (let [builder (android.app.AlertDialog$Builder. context)]
     (.setPositiveButton builder (get-string :ok) nil)
     (let [dialog (.create builder)]
       (.setTitle dialog title)
       (.setMessage dialog message)
       (.setCanceledOnTouchOutside dialog false)
       (try
         (.show dialog)
         (catch java.lang.Exception e nil)))))
  ([context message view buttons]
   (let [dialog-fragment (proxy [android.app.DialogFragment] []
                           (onCreate [bundle]
                             (proxy-super onCreate bundle)
                             (.setRetainInstance this true))
                           (onDetach []
                             (proxy-super onDetach)
                             (when view
                               (.removeView (.getParent view) view)))
                           (onDestroyView []
                             (when (and (.getDialog this)
                                        (.getRetainInstance this))
                               (.setDismissMessage (.getDialog this) nil))
                             (proxy-super onDestroyView))
                           (onCreateDialog [bundle]
                             (proxy-super onCreateDialog bundle)
                             (create-dialog context message view buttons)))]
     (try
       (.show dialog-fragment (.getFragmentManager context) "dialog")
       (catch java.lang.Exception e nil)))))

(defn show-pending-user-dialog
  [context]
  (show-dialog context nil (get-string :pending_user)))

(defn get-welcome-view
  [context]
  (let [view (make-ui context
                      [:linear-layout {:orientation 1}
                       [:text-view {:text (get-string :welcome_title)
                                    :tag "title-text"
                                    :layout-width :fill}]
                       [:text-view {:text (get-string :welcome_subtitle)
                                    :tag "subtitle-text"}]
                       [:linear-layout {:orientation 0}
                        [:image-view {:image-resource
                                      (get-resource :drawable :profile_small)
                                      :tag "profile-image"}]
                        [:text-view {:text (get-string :welcome_profile)
                                     :tag "profile-text"}]]
                       [:linear-layout {:orientation 0}
                        [:image-view {:image-resource
                                      (get-resource :drawable
                                                    :android/ic_menu_add)
                                      :tag "post-image"}]
                        [:text-view {:text (get-string :welcome_post)
                                     :tag "post-text"}]]
                       [:linear-layout {:orientation 0}
                        [:image-view {:image-resource
                                      (get-resource :drawable
                                                    :android/ic_menu_share)
                                      :tag "share-image"}]
                        [:text-view {:text (get-string :welcome_share)
                                     :tag "share-text"}]]])
        title-text (.findViewWithTag view "title-text")
        subtitle-text (.findViewWithTag view "subtitle-text")
        profile-text (.findViewWithTag view "profile-text")
        post-text (.findViewWithTag view "post-text")
        share-text (.findViewWithTag view "share-text")
        profile-image (.findViewWithTag view "profile-image")
        post-image (.findViewWithTag view "post-image")
        share-image (.findViewWithTag view "share-image")
        pad (make-dip context 10)
        s 80]
    (set-text-size title-text large-text-size)
    (set-text-size subtitle-text default-text-size)
    (.setGravity title-text android.view.Gravity/CENTER_HORIZONTAL)
    (doseq [txt [profile-text post-text share-text]]
      (set-text-size txt default-text-size)
      (.setMinHeight txt s)
      (.setGravity txt android.view.Gravity/CENTER_VERTICAL))
    (doseq [img [profile-image post-image share-image]]
      (.setLayoutParams img (android.widget.LinearLayout$LayoutParams. s s)))
    (.setPadding view pad pad pad pad)
    view))

(defn show-welcome-dialog
  [context]
  (show-dialog context
               nil
               (get-welcome-view context)
               {:positive-name (get-string :ok)
                :positive-func cancel}))

(defn show-new-user-dialog
  [context content]
  (show-dialog context
               (get-string :found_user)
               nil
               {:positive-name (get-string :download_user)
                :positive-func
                (fn [context dialog-view button-view]
                  (toggle-fav context content true))
                :negative-name (get-string :cancel)
                :negative-func
                (fn [context dialog-view button-view]
                  (.finish context))}))

(defn show-delete-post-dialog
  [context dialog-view button-view create-time]
    (show-dialog context
                 (get-string :confirm_delete)
                 nil
                 {:positive-name (get-string :delete)
                  :positive-func
                  (fn [c d b]
                    (let [text-view (.findViewWithTag dialog-view "post-body")]
                      (.setText text-view "")
                      (send-post context
                                    dialog-view
                                    button-view
                                    create-time
                                    nil
                                    0)))
                  :negative-name (get-string :cancel)
                  :negative-func cancel})
  false)

(defn show-export-dialog
  [context dialog-view button-view]
  (let [view (make-ui context [:edit-text {:single-line true
                                           :layout-width :fill
                                           :hint (get-string :password)}])
        input-type (bit-or android.text.InputType/TYPE_CLASS_TEXT
                           android.text.InputType/TYPE_TEXT_VARIATION_PASSWORD)]
    (.setInputType view input-type)
    (show-dialog context
                 (get-string :export_desc)
                 view
                 {:positive-name (get-string :save)
                  :positive-func
                  (fn [c d b]
                    (zip-and-send context (.toString (.getText view)))
                    true)
                  :negative-name (get-string :cancel)
                  :negative-func cancel}))
  true)

(defn show-import-dialog
  [context uri-str]
  (let [view (make-ui context [:edit-text {:single-line true
                                           :layout-width :fill
                                           :hint (get-string :password)}])
        input-type (bit-or android.text.InputType/TYPE_CLASS_TEXT
                           android.text.InputType/TYPE_TEXT_VARIATION_PASSWORD)]
    (.setInputType view input-type)
    (show-dialog context
                 (get-string :import_desc)
                 view
                 {:positive-name (get-string :import_user)
                  :positive-func
                  (fn [c d b]
                    (unzip-and-save context (.toString (.getText view)) uri-str)
                    true)
                  :negative-name (get-string :cancel)
                  :negative-func cancel})))

(defn show-switch-user-dialog
  [context content]
  (let [view (make-ui context [:scroll-view {}
                               [:radio-group {:orientation 1}]])
        radio-group (.getChildAt view 0)
        items (conj (vec (for [user-hash (read-user-list-file)]
                           (get-single-user-data {:userhash user-hash})))
                    {:title (get-string :create_user)})]
    ; add each user to the list
    (future
      (doseq [i (range (count items))]
        (let [item (get items i)
              title (if (= 0 (count (:title item)))
                      (get-string :no_name)
                      (:title item))
              button (make-ui context [:radio-button {:text title
                                                      :single-line true
                                                      :id i}])
              pad (make-dip context 10)]
          (.setChecked button (is-me? (:userhash item)))
          (on-ui (set-text-size button default-text-size)
                 (.setPadding button pad pad pad pad)
                 (when (nil? (:userhash item))
                   (.setTypeface button (cast String nil) 2))
                 (.setOnClickListener
                   button
                   (proxy [android.view.View$OnClickListener] []
                     (onClick [v])))
                 (.addView radio-group button)))))
    ; display a dialog with the list
    (show-dialog context
                 nil
                 view
                 {:positive-name (get-string :select)
                  :positive-func (fn [c d b])
                  :neutral-name (get-string :delete)
                  :neutral-func (fn [c d b])
                  :negative-name (get-string :cancel)
                  :negative-func cancel})))

(defn show-remove-user-dialog
  [context content]
  (show-dialog context
               (get-string :confirm_unfav)
               nil
               {:positive-name (get-string :unfav_user)
                :positive-func
                (fn [context dialog-view button-view]
                  (toggle-fav context content true))
                :negative-name (get-string :cancel)
                :negative-func cancel}))

(defn get-new-post-view
  [context content]
  (let [page-content (get-state context :share)
        pointers (if (empty? content)
                   {:ptrhash (when (and (:userhash page-content)
                                        (or (= :post (:type page-content))
                                            (-> (:userhash page-content)
                                                (is-me?)
                                                (not))))
                               (:userhash page-content))
                    :ptrtime (when (= :post (:type page-content))
                               (:time page-content))}
                   {:ptrhash (:ptrhash content)
                    :ptrtime (:ptrtime content)})
        view (make-ui context [:linear-layout {:orientation 1}
                               [:linear-layout {:orientation 0
                                                :tag "user-info"}
                                [:image-view {:background-resource
                                              (get-resource :drawable :profile)
                                              :tag "user-img"}]
                                [:text-view {:single-line true
                                             :tag "user-name"}]]
                               [:edit-text {:min-lines 10
                                            :tag "post-body"}]])
        user-info (.findViewWithTag view "user-info")
        user-img (.findViewWithTag view "user-img")
        user-name (.findViewWithTag view "user-name")
        post-body (.findViewWithTag view "post-body")
        pad (make-dip context 10)
        s 80]
    (.setTag view pointers)
    (set-text-size user-name default-text-size)
    (.setMinHeight user-name s)
    (.setGravity user-name android.view.Gravity/CENTER_VERTICAL)
    (.setPadding user-name pad 0 0 0)
    (.setLayoutParams user-img (android.widget.LinearLayout$LayoutParams. s s))
    (.setScaleType user-img android.widget.ImageView$ScaleType/CENTER_CROP)
    (if-let [ptr-hash (:ptrhash pointers)]
      (future (let [user (get-single-user-data {:userhash ptr-hash})
                    path (get-pic-path (:userhash user) (:pichash user))
                    bitmap (path-to-bitmap path thumb-size)]
                (on-ui (.setText user-name (:title user))
                       (.setImageBitmap user-img bitmap))))
      (.setVisibility user-info android.view.View/GONE))
    (set-text-size post-body default-text-size)
    (set-text-max-length post-body max-length-large)
    (.setText post-body (:body content))
    (clear-attachments context)
    view))

(defn show-new-post-dialog
  [context content]
  (let [view (get-new-post-view context content)]
    (show-dialog context
                 nil
                 view
                 {:positive-name (if (:ptrtime (.getTag view))
                                   (get-string :send_reply)
                                   (get-string :send))
                  :positive-func send-post
                  :neutral-name (get-string :attach_pics)
                  :neutral-func attach-to-post
                  :negative-name (get-string :cancel)
                  :negative-func cancel})))

(defn show-edit-post-dialog
  [context content pics]
  (show-dialog context
               nil
               (get-new-post-view context content)
               {:positive-name (get-string :save)
                :positive-func
                (fn [context dialog-view button-view]
                  (send-post context
                             dialog-view
                             button-view
                             (:time content)
                             (for [pic pics]
                               (:pichash pic))
                             1))
                :neutral-name (get-string :delete)
                :neutral-func
                (fn [context dialog-view button-view]
                  (show-delete-post-dialog
                                  context
                                  dialog-view
                                  button-view
                                  (:time content)))
                :negative-name (get-string :cancel)
                :negative-func cancel}))

(defn get-profile-view
  [context content]
  (let [bold android.graphics.Typeface/DEFAULT_BOLD
        view (if (is-me? (:userhash content))
               (make-ui context [:scroll-view {}
                                 [:linear-layout {:orientation 1}
                                  [:edit-text {:single-line true
                                               :layout-width :fill
                                               :tag "profile-title"}]
                                  [:edit-text {:layout-width :fill
                                               :tag "profile-body"}]
                                  [:relative-layout {}]]])
               (make-ui context [:scroll-view {}
                                 [:linear-layout {:orientation 1}
                                  [:text-view {:single-line true
                                               :layout-width :fill
                                               :text-is-selectable true
                                               :typeface bold}]
                                  [:text-view {:layout-width :fill
                                               :text-is-selectable true}]
                                  [:relative-layout {}]]]))
        linear-layout (.getChildAt view 0)
        text-name (.getChildAt linear-layout 0)
        text-body (.getChildAt linear-layout 1)
        relative-layout (.getChildAt linear-layout 2)
        image-view (proxy [android.widget.ImageButton] [context]
                     (onMeasure [width height]
                       (proxy-super onMeasure width width)))
        clear-btn (android.widget.Button. context)
        pad (make-dip context 10)]
    ; set padding and text size
    (.setPadding linear-layout pad pad pad pad)
    (set-text-size text-name default-text-size)
    (set-text-size text-body default-text-size)
    (set-text-max-length text-name max-length-small)
    (set-text-max-length text-body max-length-large)
    ; set text content
    (when (is-me? (:userhash content))
      (.setHint text-name (get-string :name))
      (.setHint text-body (get-string :about_me)))
    (.setText text-name (:title content))
    (if (is-me? (:userhash content))
      (.setText text-body (:body content))
      (->> (:body content)
           (tags-encode :user)
           (set-text-content context text-body)))
    (.setText clear-btn (get-string :clear))
    ; set layout params for image view and clear button
    (let [fill android.widget.RelativeLayout$LayoutParams/FILL_PARENT
          params (android.widget.RelativeLayout$LayoutParams. fill 0)]
      (.setLayoutParams image-view params))
    (let [wrap android.widget.RelativeLayout$LayoutParams/WRAP_CONTENT
          params (android.widget.RelativeLayout$LayoutParams. wrap wrap)]
      (.addRule params android.widget.RelativeLayout/ALIGN_PARENT_TOP)
      (.addRule params android.widget.RelativeLayout/ALIGN_PARENT_RIGHT)
      (.setLayoutParams clear-btn params))
    ; set image view and clear button parameters
    (.setTag image-view "profile-image")
    (.setScaleType image-view android.widget.ImageView$ScaleType/CENTER_CROP)
    (.setBackgroundResource image-view (get-resource :drawable :profile))
    (future
      (let [bitmap (-> (get-pic-path (:userhash content)
                                     (:pichash content))
                       (path-to-bitmap full-size))]
        (on-ui (.setImageBitmap image-view bitmap))))
    (.addView relative-layout image-view)
    (when (is-me? (:userhash content))
      (.setOnClickListener
        image-view
        (proxy [android.view.View$OnClickListener] []
          (onClick [v]
            (request-files context
                           "image/*"
                           (fn [uri]
                             (let [pic (uri-to-bitmap context (.toString uri))]
                               (.setImageBitmap image-view pic)))))))
      (.setOnClickListener clear-btn
                           (proxy [android.view.View$OnClickListener] []
                             (onClick [v]
                               (.setImageBitmap image-view nil))))
      (.addView relative-layout clear-btn))
    ; return the parent view
    view))

(defn show-profile-dialog
  [context content]
  (show-dialog context
               nil
               (get-profile-view context content)
               (if (is-me? (:userhash content))
                 {:positive-name (get-string :save)
                  :positive-func save-profile
                  :neutral-name (get-string :export_start)
                  :neutral-func show-export-dialog
                  :negative-name (get-string :cancel)
                  :negative-func cancel}
                 {:positive-name (get-string :ok)
                  :positive-func cancel})))
