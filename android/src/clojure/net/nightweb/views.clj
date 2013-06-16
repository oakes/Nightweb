(ns net.nightweb.views
  (:use [neko.ui :only [make-ui]]
        [neko.threading :only [on-ui]]
        [neko.resource :only [get-string get-resource]]
        [neko.notify :only [toast]]
        [net.nightweb.utils :only [full-size
                                   thumb-size
                                   path-to-bitmap
                                   get-pic-path
                                   make-dip
                                   create-tile-image
                                   default-text-size
                                   set-text-size
                                   set-text-max-length
                                   set-text-content
                                   get-drawable-at-runtime
                                   get-string-at-runtime]]
        [net.nightweb.actions :only [show-spinner
                                     clear-attachments
                                     send-post
                                     tile-action
                                     save-profile
                                     cancel
                                     toggle-fav]]
        [net.nightweb.dialogs :only [show-delete-post-dialog
                                     show-edit-post-dialog
                                     show-profile-dialog]]
        [nightweb.db :only [get-pic-data
                            get-single-post-data
                            get-single-user-data
                            get-single-tag-data]]
        [nightweb.db_tiles :only [get-post-tiles
                                  get-user-tiles
                                  get-category-tiles]]
        [nightweb.formats :only [remove-dupes-and-nils
                                 base32-encode
                                 tags-encode]]
        [nightweb.constants :only [is-me?]]))

(def ^:const default-tile-width 160)

(defn create-grid-view-tile
  [context item]
  (let [pad (make-dip context 3)
        radius (make-dip context 10)
        dip-text-size (make-dip context default-text-size)
        black android.graphics.Color/BLACK
        white android.graphics.Color/WHITE
        end android.text.TextUtils$TruncateAt/END
        tile-view (make-ui context
                           [:frame-layout {}
                            [:image-view {}]
                            [:linear-layout {:orientation 1}
                             [:text-view {:text-color white
                                          :layout-width :fill
                                          :layout-weight 1}]
                             [:linear-layout {:orientation 0}
                              [:text-view {:text-color white
                                           :single-line true
                                           :layout-weight 1
                                           :ellipsize end}]
                              [:text-view {:text-color white}]]]])
        image (.getChildAt tile-view 0)
        linear-layout (.getChildAt tile-view 1)
        text-top (.getChildAt linear-layout 0)
        bottom-layout (.getChildAt linear-layout 1)
        text-bottom (.getChildAt bottom-layout 0)
        text-count (.getChildAt bottom-layout 1)]
    (.setScaleType image android.widget.ImageView$ScaleType/CENTER_CROP)
    (.setTypeface text-bottom android.graphics.Typeface/DEFAULT_BOLD)
    (doseq [text-view [text-top text-bottom text-count]]
      (set-text-size text-view default-text-size)
      (.setPadding text-view pad pad pad pad)
      (.setShadowLayer text-view radius 0 0 black))
    (if (:add-emphasis? item)
      (do
        (.setTypeface text-top android.graphics.Typeface/DEFAULT_BOLD)
        (.setGravity text-top android.view.Gravity/CENTER_HORIZONTAL))
      (do
        (.setTypeface text-top android.graphics.Typeface/DEFAULT)
        (.setGravity text-top android.view.Gravity/LEFT)))
    (when-let [bg (:background item)]
      (.setBackgroundResource image (get-drawable-at-runtime context bg)))
    (when-let [title (or (:title item) (:body item) (:tag item))]
      (if (= title :page)
        (.setText text-top (str (get-string-at-runtime context title)
                                " "
                                (:page item)))
        (.setText text-top (get-string-at-runtime context title))))
    (.setText text-bottom (:subtitle item))
    (if-let [item-count (:count item)]
      (.setText text-count (if (> item-count 0) (str item-count) nil)))
    (if (and (= (.length (.getText text-bottom)) 0)
             (= (.length (.getText text-count)) 0))
      (.setVisibility bottom-layout android.view.View/GONE))
    (.setImageDrawable image
                       (if (nil? (:tag item))
                         (create-tile-image context
                                            (:userhash item)
                                            (:pichash item))
                         (let [tag (get-single-tag-data item)]
                           (create-tile-image context
                                              (:userhash tag)
                                              (:pichash tag)))))
    (.setOnClickListener
      tile-view
      (proxy [android.view.View$OnClickListener] []
        (onClick [view]
          (tile-action context item))))
    tile-view))

(defn add-grid-view-tiles
  [context content view]
  (future
    (let [num-columns (.getColumnCount view)
          width (.getWidth view)
          tile-view-width (if (and (> width 0) (> num-columns 0))
                            (int (/ width num-columns))
                            (make-dip context default-tile-width))]
      (doseq [position (range (count content))]
        (let [tile (create-grid-view-tile context (get content position))]
          (on-ui (.addView view tile tile-view-width tile-view-width)))))))

(defn get-grid-view
  [context]
  (let [tile-view-min (make-dip context default-tile-width)
        view (proxy [android.widget.GridLayout] [context]
               (onMeasure [width-spec height-spec]
                 (let [w (android.view.View$MeasureSpec/getSize width-spec)
                       num-columns (int (/ w tile-view-min))]
                   (.setColumnCount this num-columns))
                 (proxy-super onMeasure width-spec height-spec)))]
    view))

(defn get-post-view
  [context params]
  (let [view (make-ui context [:scroll-view {}
                               [:linear-layout {:orientation 1}
                                [:text-view {:layout-width :fill
                                             :text-is-selectable true}]
                                [:text-view {:layout-width :fill}]]])
        linear-layout (.getChildAt view 0)
        text-view (.getChildAt linear-layout 0)
        date-view (.getChildAt linear-layout 1)
        grid-view (get-grid-view context)
        pad (make-dip context 10)]
    (.setPadding text-view pad pad pad pad)
    (.setPadding date-view pad pad pad pad)
    (set-text-size text-view default-text-size)
    (set-text-size date-view default-text-size)
    (.addView linear-layout grid-view)
    (show-spinner
      context
      (get-string :loading)
      (fn []
        (let [post (get-single-post-data params)
              tiles (get-post-tiles post show-edit-post-dialog)]
          (if (nil? (:body post))
            (on-ui (toast (get-string :lost_post)))
            (on-ui 
              (set-text-content context
                                text-view
                                (tags-encode :post (:body post)))
              (let [date-format (java.text.DateFormat/getDateTimeInstance
                                  java.text.DateFormat/MEDIUM
                                  java.text.DateFormat/SHORT)]
                (.setText date-view (->> (:time post)
                                         (java.util.Date.)
                                         (.format date-format))))
              (add-grid-view-tiles context tiles grid-view))))
        false))
    view))

(defn get-gallery-view
  [context params]
  (let [view (make-ui context [:view-pager {}])]
    (show-spinner
      context
      (get-string :loading)
      (fn []
        (let [pics (get-pic-data params (:ptrtime params) false)]
          (on-ui
            (.setAdapter
              view
              (proxy [android.support.v4.view.PagerAdapter] []
                (destroyItem [container position object]
                  (.removeView container object))
                (getCount [] (count pics))
                (instantiateItem [container pos]
                  (let [image-view (android.widget.ImageView. context)
                        bitmap (-> (get-pic-path (get-in pics [pos :userhash])
                                                 (get-in pics [pos :pichash]))
                                   (path-to-bitmap full-size))]
                    (.setImageBitmap image-view bitmap)
                    (.addView container image-view)
                    image-view))
                (isViewFromObject [view object] (= view object))
                (setPrimaryItem [container position object])))
            (.setCurrentItem view
                             (->> pics
                                  (filter (fn [pic]
                                            (java.util.Arrays/equals
                                              (:pichash pic)
                                              (:pichash params))))
                                  (first)
                                  (.indexOf pics)))))
        false))
    view))

(defn get-user-view
  [context params]
  (let [view (make-ui context [:scroll-view {}])
        grid-view (get-grid-view context)]
    (.addView view grid-view)
    (show-spinner
      context
      (get-string :loading)
      (fn []
        (let [user (get-single-user-data params)
              tiles (get-user-tiles params user show-profile-dialog)]
          (add-grid-view-tiles context tiles grid-view))
        false))
    view))

(defn get-category-view
  [context params]
  (let [view (make-ui context [:scroll-view {}])
        grid-view (get-grid-view context)]
    (.addView view grid-view)
    (show-spinner
      context
      (get-string :loading)
      (fn []
        (let [tiles (get-category-tiles params)]
          (add-grid-view-tiles context tiles grid-view))
        false))
    view))

(defn create-tab
  [action-bar title create-view]
  (try
    (let [tab (.newTab action-bar)
          fragment (proxy [android.app.Fragment] []
                     (onCreateView [layout-inflater viewgroup bundle]
                       (create-view)))
          listener (proxy [android.app.ActionBar$TabListener] []
                     (onTabSelected [tab ft]
                       (.add ft (get-resource :id :android/content) fragment))
                     (onTabUnselected [tab ft]
                       (.remove ft fragment))
                     (onTabReselected [tab ft]
                       (.detach ft fragment)
                       (.attach ft fragment)))]
      (.setText tab title)
      (.setTabListener tab listener)
      (.addTab action-bar tab))
    (catch Exception e nil)))
