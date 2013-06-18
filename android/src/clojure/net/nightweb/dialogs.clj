(ns net.nightweb.dialogs
  (:require [neko.resource :as r]
            [neko.threading :as thread]
            [neko.ui :as ui]
            [neko.ui.mapping :as mapping]
            [net.clandroid.activity :as activity]
            [net.nightweb.actions :as actions]
            [net.nightweb.utils :as utils]
            [nightweb.actions :as a]
            [nightweb.constants :as c]
            [nightweb.db :as db]
            [nightweb.formats :as f]
            [nightweb.io :as io]
            [nightweb.users :as users]))

(mapping/defelement :scroll-view :classname android.widget.ScrollView)
(mapping/defelement :frame-layout :classname android.widget.FrameLayout)
(mapping/defelement :relative-layout :classname android.widget.RelativeLayout)
(mapping/defelement :image-view :classname android.widget.ImageView)
(mapping/defelement :view-pager :classname android.support.v4.view.ViewPager)

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
                               (catch Exception e nil))))))]
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
     (.setPositiveButton builder (r/get-string :ok) nil)
     (let [dialog (.create builder)]
       (.setTitle dialog title)
       (.setMessage dialog message)
       (.setCanceledOnTouchOutside dialog false)
       (try
         (.show dialog)
         (catch Exception e nil)))))
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
       (catch Exception e nil)))))

(defn show-pending-user-dialog
  [context]
  (show-dialog context nil (r/get-string :pending_user)))

(defn get-welcome-view
  [context]
  (let [view (ui/make-ui
               context
               [:linear-layout {:orientation 1}
                [:text-view {:text (r/get-string :welcome_title)
                             :tag "title-text"
                             :layout-width :fill}]
                [:text-view {:text (r/get-string :welcome_subtitle)
                             :tag "subtitle-text"}]
                [:linear-layout {:orientation 0}
                 [:image-view {:image-resource
                               (r/get-resource :drawable :profile_small)
                               :tag "profile-image"}]
                 [:text-view {:text (r/get-string :welcome_profile)
                              :tag "profile-text"}]]
                [:linear-layout {:orientation 0}
                 [:image-view {:image-resource
                               (r/get-resource :drawable
                                             :android/ic_menu_add)
                               :tag "post-image"}]
                 [:text-view {:text (r/get-string :welcome_post)
                              :tag "post-text"}]]
                [:linear-layout {:orientation 0}
                 [:image-view {:image-resource
                               (r/get-resource :drawable
                                             :android/ic_menu_share)
                               :tag "share-image"}]
                 [:text-view {:text (r/get-string :welcome_share)
                              :tag "share-text"}]]])
        title-text (.findViewWithTag view "title-text")
        subtitle-text (.findViewWithTag view "subtitle-text")
        profile-text (.findViewWithTag view "profile-text")
        post-text (.findViewWithTag view "post-text")
        share-text (.findViewWithTag view "share-text")
        profile-image (.findViewWithTag view "profile-image")
        post-image (.findViewWithTag view "post-image")
        share-image (.findViewWithTag view "share-image")
        pad (utils/make-dip context 10)
        s 80]
    (utils/set-text-size title-text utils/large-text-size)
    (utils/set-text-size subtitle-text utils/default-text-size)
    (.setGravity title-text android.view.Gravity/CENTER_HORIZONTAL)
    (doseq [txt [profile-text post-text share-text]]
      (utils/set-text-size txt utils/default-text-size)
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
               {:positive-name (r/get-string :ok)
                :positive-func actions/cancel}))

(defn show-new-user-dialog
  [context content]
  (show-dialog context
               (r/get-string :found_user)
               nil
               {:positive-name (r/get-string :download_user)
                :positive-func
                (fn [context dialog-view button-view]
                  (actions/toggle-fav context content true))
                :negative-name (r/get-string :cancel)
                :negative-func
                (fn [context dialog-view button-view]
                  (.finish context))}))

(defn show-delete-post-dialog
  [context dialog-view button-view create-time]
    (show-dialog context
                 (r/get-string :confirm_delete)
                 nil
                 {:positive-name (r/get-string :delete)
                  :positive-func
                  (fn [c d b]
                    (let [text-view (.findViewWithTag dialog-view "post-body")]
                      (.setText text-view "")
                      (actions/new-post context
                                        dialog-view
                                        button-view
                                        create-time
                                        nil
                                        0)))
                  :negative-name (r/get-string :cancel)
                  :negative-func actions/cancel})
  false)

(defn show-delete-user-dialog
  [context user-hash]
  (show-dialog context
               (r/get-string :confirm_delete)
               nil
               {:positive-name (r/get-string :delete)
                :positive-func (fn [c d b]
                                 (users/delete-user user-hash)
                                 (.finish context)
                                 (utils/show-home context {}))
                :negative-name (r/get-string :cancel)
                :negative-func actions/cancel}))

(defn show-export-dialog
  [context dialog-view button-view]
  (let [view (ui/make-ui context [:edit-text {:single-line true
                                              :layout-width :fill
                                              :hint (r/get-string :password)}])
        input-type (bit-or android.text.InputType/TYPE_CLASS_TEXT
                           android.text.InputType/TYPE_TEXT_VARIATION_PASSWORD)]
    (.setInputType view input-type)
    (show-dialog context
                 (r/get-string :export_desc)
                 view
                 {:positive-name (r/get-string :save)
                  :positive-func
                  (fn [c d b]
                    (actions/zip-and-send context (.toString (.getText view)))
                    true)
                  :negative-name (r/get-string :cancel)
                  :negative-func actions/cancel}))
  true)

(defn show-import-dialog
  [context uri-str]
  (let [view (ui/make-ui context [:edit-text {:single-line true
                                              :layout-width :fill
                                              :hint (r/get-string :password)}])
        input-type (bit-or android.text.InputType/TYPE_CLASS_TEXT
                           android.text.InputType/TYPE_TEXT_VARIATION_PASSWORD)]
    (.setInputType view input-type)
    (show-dialog context
                 (r/get-string :import_desc)
                 view
                 {:positive-name (r/get-string :import_user)
                  :positive-func
                  (fn [c d b]
                    (actions/unzip-and-save
                      context (.toString (.getText view)) uri-str)
                    true)
                  :negative-name (r/get-string :cancel)
                  :negative-func actions/cancel})))

(defn show-switch-user-dialog
  [context content]
  (let [view (ui/make-ui context [:scroll-view {}
                                  [:linear-layout {:orientation 1}]])]
    ; add each user to the list
    (future
      (let [linear-layout (.getChildAt view 0)
            items (for [user-hash (io/read-user-list-file)]
                    (db/get-single-user-data {:userhash user-hash}))]
        (doseq [item items]
          (let [title (if (= 0 (count (:title item)))
                        (r/get-string :no_name)
                        (:title item))
                list-item (ui/make-ui context
                                      [:linear-layout {:orientation 0}
                                       [:button {:text title
                                                 :layout-weight 3}]
                                       [:button {:text (r/get-string :delete)
                                                 :layout-weight 1}]])
                select-button (.getChildAt list-item 0)
                delete-button (.getChildAt list-item 1)]
            (thread/on-ui
              (.setEnabled select-button (not (c/is-me? (:userhash item))))
              (.setOnClickListener
                select-button
                (proxy [android.view.View$OnClickListener] []
                  (onClick [v]
                    (users/load-user (:userhash item))
                    (.finish context)
                    (utils/show-home context {}))))
              (.setOnClickListener
                delete-button
                (proxy [android.view.View$OnClickListener] []
                  (onClick [v]
                    (show-delete-user-dialog context (:userhash item)))))
              (.addView linear-layout list-item))))))
    ; display a dialog with the list
    (show-dialog context
                 nil
                 view
                 {:positive-name (r/get-string :create_user)
                  :positive-func (fn [context dialog-view button-view]
                                   (users/load-user (users/create-user))
                                   (a/fav-default-user)
                                   (.finish context)
                                   (utils/show-home context {}))
                  :negative-name (r/get-string :cancel)
                  :negative-func actions/cancel})))

(defn show-remove-user-dialog
  [context content]
  (show-dialog context
               (r/get-string :confirm_unfav)
               nil
               {:positive-name (r/get-string :unfav_user)
                :positive-func
                (fn [context dialog-view button-view]
                  (actions/toggle-fav context content true))
                :negative-name (r/get-string :cancel)
                :negative-func actions/cancel}))

(defn get-new-post-view
  [context content]
  (let [page-content (activity/get-state context :share)
        pointers (if (empty? content)
                   {:ptrhash (when (and (:userhash page-content)
                                        (or (= :post (:type page-content))
                                            (-> (:userhash page-content)
                                                c/is-me?
                                                not)))
                               (:userhash page-content))
                    :ptrtime (when (= :post (:type page-content))
                               (:time page-content))}
                   {:ptrhash (:ptrhash content)
                    :ptrtime (:ptrtime content)})
        view (ui/make-ui
               context
               [:linear-layout {:orientation 1}
                [:linear-layout {:orientation 0
                                 :tag "user-info"}
                 [:image-view {:background-resource
                               (r/get-resource :drawable :profile)
                               :tag "user-img"}]
                 [:text-view {:single-line true
                              :tag "user-name"}]]
                [:edit-text {:min-lines 10
                             :tag "post-body"}]])
        user-info (.findViewWithTag view "user-info")
        user-img (.findViewWithTag view "user-img")
        user-name (.findViewWithTag view "user-name")
        post-body (.findViewWithTag view "post-body")
        pad (utils/make-dip context 10)
        s 80]
    (.setTag view pointers)
    (utils/set-text-size user-name utils/default-text-size)
    (.setMinHeight user-name s)
    (.setGravity user-name android.view.Gravity/CENTER_VERTICAL)
    (.setPadding user-name pad 0 0 0)
    (.setLayoutParams user-img (android.widget.LinearLayout$LayoutParams. s s))
    (.setScaleType user-img android.widget.ImageView$ScaleType/CENTER_CROP)
    (if-let [ptr-hash (:ptrhash pointers)]
      (future (let [user (db/get-single-user-data {:userhash ptr-hash})
                    path (utils/get-pic-path (:userhash user) (:pichash user))
                    bitmap (utils/path-to-bitmap path utils/thumb-size)]
                (thread/on-ui (.setText user-name (:title user))
                              (.setImageBitmap user-img bitmap))))
      (.setVisibility user-info android.view.View/GONE))
    (utils/set-text-size post-body utils/default-text-size)
    (utils/set-text-max-length post-body db/max-length-large)
    (.setText post-body (:body content))
    (actions/clear-attachments context)
    view))

(defn show-new-post-dialog
  [context content]
  (let [view (get-new-post-view context content)]
    (show-dialog context
                 nil
                 view
                 {:positive-name (if (:ptrtime (.getTag view))
                                   (r/get-string :send_reply)
                                   (r/get-string :send))
                  :positive-func actions/new-post
                  :neutral-name (r/get-string :attach_pics)
                  :neutral-func actions/attach-to-post
                  :negative-name (r/get-string :cancel)
                  :negative-func actions/cancel})))

(defn show-edit-post-dialog
  [context content pics]
  (show-dialog context
               nil
               (get-new-post-view context content)
               {:positive-name (r/get-string :save)
                :positive-func
                (fn [context dialog-view button-view]
                  (actions/new-post context
                                    dialog-view
                                    button-view
                                    (:time content)
                                    (for [pic pics] (:pichash pic))
                                    1))
                :neutral-name (r/get-string :delete)
                :neutral-func
                (fn [context dialog-view button-view]
                  (show-delete-post-dialog context
                                           dialog-view
                                           button-view
                                           (:time content)))
                :negative-name (r/get-string :cancel)
                :negative-func actions/cancel}))

(defn get-profile-view
  [context content]
  (let [bold android.graphics.Typeface/DEFAULT_BOLD
        view (if (c/is-me? (:userhash content))
               (ui/make-ui context
                           [:scroll-view {}
                            [:linear-layout {:orientation 1}
                             [:edit-text {:single-line true
                                          :layout-width :fill
                                          :tag "profile-title"}]
                             [:edit-text {:layout-width :fill
                                          :tag "profile-body"}]
                             [:relative-layout {}]]])
               (ui/make-ui context
                           [:scroll-view {}
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
        pad (utils/make-dip context 10)]
    ; set padding and text size
    (.setPadding linear-layout pad pad pad pad)
    (utils/set-text-size text-name utils/default-text-size)
    (utils/set-text-size text-body utils/default-text-size)
    (utils/set-text-max-length text-name db/max-length-small)
    (utils/set-text-max-length text-body db/max-length-large)
    ; set text content
    (when (c/is-me? (:userhash content))
      (.setHint text-name (r/get-string :name))
      (.setHint text-body (r/get-string :about_me)))
    (.setText text-name (:title content))
    (if (c/is-me? (:userhash content))
      (.setText text-body (:body content))
      (->> (:body content)
           (f/tags-encode :user)
           (utils/set-text-content context text-body)))
    (.setText clear-btn (r/get-string :clear))
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
    (.setBackgroundResource image-view (r/get-resource :drawable :profile))
    (future
      (let [bitmap (-> (utils/get-pic-path (:userhash content)
                                           (:pichash content))
                       (utils/path-to-bitmap utils/full-size))]
        (thread/on-ui (.setImageBitmap image-view bitmap))))
    (.addView relative-layout image-view)
    (when (c/is-me? (:userhash content))
      (.setOnClickListener
        image-view
        (proxy [android.view.View$OnClickListener] []
          (onClick [v]
            (actions/request-files
              context
              "image/*"
              (fn [uri]
                (let [pic (utils/uri-to-bitmap context (.toString uri))]
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
               (if (c/is-me? (:userhash content))
                 {:positive-name (r/get-string :save)
                  :positive-func actions/save-profile
                  :neutral-name (r/get-string :export_start)
                  :neutral-func show-export-dialog
                  :negative-name (r/get-string :cancel)
                  :negative-func actions/cancel}
                 {:positive-name (r/get-string :ok)
                  :positive-func actions/cancel})))
