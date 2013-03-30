(ns net.nightweb.dialogs
  (:use [neko.resource :only [get-string get-resource]]
        [neko.ui :only [make-ui]]
        [neko.ui.mapping :only [set-classname!]]
        [net.nightweb.actions :only [clear-attachments
                                     send-post
                                     toggle-fav
                                     attach-to-post
                                     cancel
                                     zip-and-send
                                     unzip-and-save
                                     save-profile]]
        [net.nightweb.utils :only [make-dip
                                   default-text-size
                                   large-text-size
                                   set-text-size
                                   set-text-max-length]]
        [nightweb.db :only [max-length-large]]
        [nightweb.constants :only [is-me?]]))

(set-classname! :scroll-view android.widget.ScrollView)
(set-classname! :frame-layout android.widget.FrameLayout)
(set-classname! :relative-layout android.widget.RelativeLayout)
(set-classname! :image-view android.widget.ImageView)
(set-classname! :view-pager android.support.v4.view.ViewPager)

(defn create-dialog
  [context message view buttons]
  (let [builder (android.app.AlertDialog$Builder. context)]
    (when-let [positive-name (get buttons :positive-name)]
      (.setPositiveButton builder positive-name nil))
    (when-let [neutral-name (get buttons :neutral-name)]
      (.setNeutralButton builder neutral-name nil))
    (when-let [negative-name (get buttons :negative-name)]
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
                             (.dismiss dialog)))))]
      (.setOnShowListener
        dialog
        (proxy [android.content.DialogInterface$OnShowListener] []
          (onShow [d]
            (when-let [positive-btn (.getButton d positive-type)]
              (.setOnClickListener
                positive-btn (btn-action d
                                         positive-btn
                                         (get buttons :positive-func))))
            (when-let [neutral-btn (.getButton d neutral-type)]
              (.setOnClickListener
                neutral-btn (btn-action d
                                        neutral-btn
                                        (get buttons :neutral-func))))
            (when-let [negative-btn (.getButton d negative-type)]
              (.setOnClickListener
                negative-btn (btn-action d
                                         negative-btn
                                         (get buttons :negative-func)))))))
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
       (.show dialog))))
  ([context message view buttons]
   (let [dialog-fragment (proxy [android.app.DialogFragment] []
                           (onCreate [bundle]
                             (proxy-super onCreate bundle)
                             (.setRetainInstance this true))
                           (onDetach []
                             (proxy-super onDetach)
                             (.removeView (.getParent view) view))
                           (onDestroyView []
                             (when (and (.getDialog this)
                                        (.getRetainInstance this))
                               (.setDismissMessage (.getDialog this) nil))
                             (proxy-super onDestroyView))
                           (onCreateDialog [bundle]
                             (proxy-super onCreateDialog bundle)
                             (create-dialog context message view buttons)))]
     (.show dialog-fragment (.getFragmentManager context) "dialog"))))

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
                                      (get-resource :drawable :profile)
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
                :positive-func (fn [c d b] true)}))

(defn show-new-user-dialog
  [context content]
  (show-dialog context
               (get-string :new_user)
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
                 (get-string :confirm_import)
                 view
                 {:positive-name (get-string :import_user)
                  :positive-func
                  (fn [c d b]
                    (unzip-and-save context (.toString (.getText view)) uri-str)
                    true)
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
  (let [view (make-ui context [:linear-layout {:orientation 1}
                               [:edit-text {:min-lines 10
                                            :tag "post-body"}]])
        text-view (.getChildAt view 0)]
    (set-text-size text-view default-text-size)
    (set-text-max-length text-view max-length-large)
    (.setText text-view (get content :body))
    (clear-attachments context)
    view))

(defn show-new-post-dialog
  [context content]
  (show-dialog context
               nil
               (get-new-post-view context content)
               {:positive-name (get-string :send)
                :positive-func send-post
                :neutral-name (get-string :attach_pics)
                :neutral-func attach-to-post
                :negative-name (get-string :cancel)
                :negative-func cancel}))

(defn show-edit-post-dialog
  [context content pics]
  (show-dialog context
               nil
               (get-new-post-view context content)
               {:positive-name (get-string :send)
                :positive-func
                (fn [context dialog-view button-view]
                  (send-post context
                             dialog-view
                             button-view
                             (get content :time)
                             (for [pic pics]
                               (get pic :pichash))
                             1))
                :neutral-name (get-string :delete)
                :neutral-func
                (fn [context dialog-view button-view]
                  (show-delete-post-dialog
                                  context
                                  dialog-view
                                  button-view
                                  (get content :time)))
                :negative-name (get-string :cancel)
                :negative-func cancel}))

(defn show-profile-dialog
  [context content view]
  (show-dialog context
               nil
               view
               (if (is-me? (get content :userhash))
                 {:positive-name (get-string :save)
                  :positive-func save-profile
                  :neutral-name (get-string :export)
                  :neutral-func show-export-dialog
                  :negative-name (get-string :cancel)
                  :negative-func cancel}
                 {:positive-name (get-string :ok)
                  :positive-func cancel})))
