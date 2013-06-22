(ns net.nightweb.views
  (:require [neko.notify :as notify]
            [neko.resource :as r]
            [neko.threading :as thread]
            [neko.ui :as ui]
            [net.nightweb.actions :as actions]
            [net.nightweb.dialogs :as dialogs]
            [net.nightweb.utils :as utils]
            [nightweb.db :as db]
            [nightweb.db_tiles :as tiles]
            [nightweb.formats :as f])
  (:import [android.app ActionBar Fragment FragmentTransaction]
           [android.graphics Typeface Color]
           [android.view View ViewGroup Gravity]
           [android.widget FrameLayout LinearLayout TextView ImageView
                           GridLayout ScrollView]))

(def ^:const default-tile-width 160)

(defn do-action
  "Provides a central place to associate types with the appropriate actions."
  [context item]
  (when-let [func (case (:type item)
                    :fav actions/show-categories
                    :toggle-fav actions/toggle-fav
                    :search actions/show-categories
                    :pic actions/show-gallery
                    :edit-post dialogs/show-edit-post-dialog
                    :profile dialogs/show-profile-dialog
                    actions/show-basic)]
    (if (:confirm item)
      (dialogs/show-confirm-dialog context item func)
      (func context item))))

(defn create-grid-view-tile
  "Creates a tile based on the given item map."
  [context item]
  (let [pad (utils/make-dip context 3)
        radius (utils/make-dip context 10)
        dip-text-size (utils/make-dip context utils/default-text-size)
        end android.text.TextUtils$TruncateAt/END
        ^FrameLayout tile-view
        (ui/make-ui context
                    [:frame-layout {}
                     [:image-view {}]
                     [:linear-layout {:orientation 1}
                      [:text-view {:text-color Color/WHITE
                                   :layout-width :fill
                                   :layout-weight 1}]
                      [:linear-layout {:orientation 0}
                       [:text-view {:text-color Color/WHITE
                                    :single-line true
                                    :layout-weight 1
                                    :ellipsize end}]
                       [:text-view {:text-color Color/WHITE}]]]])
        ^ImageView image (.getChildAt tile-view 0)
        ^LinearLayout linear-layout (.getChildAt tile-view 1)
        ^TextView text-top (.getChildAt linear-layout 0)
        ^LinearLayout bottom-layout (.getChildAt linear-layout 1)
        ^TextView text-bottom (.getChildAt bottom-layout 0)
        ^TextView text-count (.getChildAt bottom-layout 1)]
    (.setScaleType image android.widget.ImageView$ScaleType/CENTER_CROP)
    (.setTypeface text-bottom Typeface/DEFAULT_BOLD)
    (doseq [^TextView text-view [text-top text-bottom text-count]]
      (utils/set-text-size text-view utils/default-text-size)
      (.setPadding text-view pad pad pad pad)
      (.setShadowLayer text-view radius 0 0 Color/BLACK))
    (.setTypeface text-top (if (:add-emphasis? item)
                             Typeface/DEFAULT_BOLD
                             Typeface/DEFAULT))
    (.setGravity text-top (if (:add-emphasis? item)
                            Gravity/CENTER_HORIZONTAL
                            Gravity/LEFT))
    (when-let [bg (:background item)]
      (.setBackgroundResource image (utils/get-drawable-at-runtime context bg)))
    (when-let [title (or (:title item) (:body item) (:tag item))]
      (let [^String title-str (utils/get-string-at-runtime context title)]
        (.setText text-top (if (= title :page)
                             (str title-str " " (:page item))
                             title-str))))
    (.setText text-bottom ^String (:subtitle item))
    (when-let [item-count (:count item)]
      (.setText text-count (if (> item-count 0) (str item-count) nil)))
    (when (and (= (.length (.getText text-bottom)) 0)
               (= (.length (.getText text-count)) 0))
      (.setVisibility bottom-layout View/GONE))
    (.setImageDrawable
      image
      (if (nil? (:tag item))
        (utils/create-tile-image context (:userhash item) (:pichash item))
        (let [tag (db/get-single-tag-data item)]
          (utils/create-tile-image context (:userhash tag) (:pichash tag)))))
    (.setOnClickListener
      tile-view
      (proxy [android.view.View$OnClickListener] []
        (onClick [view]
          (do-action context item))))
    tile-view))

(defn add-grid-view-tiles
  "Adds vector of tiles to the given view."
  [context content ^GridLayout view]
  (future
    (let [num-columns (.getColumnCount view)
          width (.getWidth view)
          tile-view-width (if (and (> width 0) (> num-columns 0))
                            (int (/ width num-columns))
                            (utils/make-dip context default-tile-width))]
      (doseq [position (range (count content))]
        (let [^View tile (create-grid-view-tile context (get content position))]
          (thread/on-ui
            (.addView view tile ^int tile-view-width ^int tile-view-width)))))))

(defn get-grid-view
  "Creates a GridLayout whose tile size is based on the screen size."
  [context]
  (let [tile-view-min (utils/make-dip context default-tile-width)]
    (proxy [GridLayout] [context]
      (onMeasure [width-spec height-spec]
        (let [w (android.view.View$MeasureSpec/getSize width-spec)
              num-columns (int (/ w tile-view-min))]
          (.setColumnCount ^GridLayout this num-columns))
        (proxy-super onMeasure width-spec height-spec)))))

(defn get-post-view
  "Creates a view with a post at top and various tiles below it."
  [context params]
  (let [^ScrollView view (ui/make-ui context
                                     [:scroll-view {}
                                      [:linear-layout {:orientation 1}
                                       [:text-view {:layout-width :fill
                                                    :text-is-selectable true}]
                                       [:text-view {:layout-width :fill}]]])
        ^LinearLayout linear-layout (.getChildAt view 0)
        ^TextView text-view (.getChildAt linear-layout 0)
        ^TextView date-view (.getChildAt linear-layout 1)
        ^GridLayout grid-view (get-grid-view context)
        pad (utils/make-dip context 10)]
    (.setPadding text-view pad pad pad pad)
    (.setPadding date-view pad pad pad pad)
    (utils/set-text-size text-view utils/default-text-size)
    (utils/set-text-size date-view utils/default-text-size)
    (.addView linear-layout grid-view)
    (actions/show-spinner
      context
      (r/get-string :loading)
      (fn []
        (let [post (db/get-single-post-data params)
              tiles (tiles/get-post-tiles post)]
          (if (nil? (:body post))
            (thread/on-ui (notify/toast (r/get-string :lost_post)))
            (thread/on-ui 
              (->> (:body post)
                (f/tags-encode :post)
                (utils/set-text-content context text-view actions/show-basic))
              (let [date-format (java.text.DateFormat/getDateTimeInstance
                                  java.text.DateFormat/MEDIUM
                                  java.text.DateFormat/SHORT)]
                (.setText date-view (->> (:time post)
                                         java.util.Date.
                                         (.format date-format))))
              (add-grid-view-tiles context tiles grid-view))))
        false))
    view))

(defn get-gallery-view
  "Creates a view of photos that can be swiped left and right."
  [context params]
  (let [view (ui/make-ui context [:view-pager {}])]
    (actions/show-spinner
      context
      (r/get-string :loading)
      (fn []
        (let [pics (db/get-pic-data params (:ptrtime params) false)]
          (thread/on-ui
            (.setAdapter
              view
              (proxy [android.support.v4.view.PagerAdapter] []
                (destroyItem [^ViewGroup container position object]
                  (.removeView container object))
                (getCount [] (count pics))
                (instantiateItem [^ViewGroup container pos]
                  (let [^ImageView image-view (ImageView. context)
                        bitmap (-> (utils/get-pic-path
                                     (get-in pics [pos :userhash])
                                     (get-in pics [pos :pichash]))
                                   (utils/path-to-bitmap utils/full-size))]
                    (.setImageBitmap image-view bitmap)
                    (.addView container image-view)
                    image-view))
                (isViewFromObject [view object] (= view object))
                (setPrimaryItem [^ViewGroup container position object])))
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
  "Creates a special grid view for user pages."
  [context params]
  (let [view (ui/make-ui context [:scroll-view {}])
        grid-view (get-grid-view context)]
    (.addView view grid-view)
    (actions/show-spinner
      context
      (r/get-string :loading)
      (fn []
        (let [user (db/get-single-user-data params)
              tiles (tiles/get-user-tiles params user)]
          (add-grid-view-tiles context tiles grid-view))
        false))
    view))

(defn get-category-view
  "Creates a special grid view for either users or posts."
  [context params]
  (let [view (ui/make-ui context [:scroll-view {}])
        grid-view (get-grid-view context)]
    (.addView view grid-view)
    (actions/show-spinner
      context
      (r/get-string :loading)
      (fn []
        (let [tiles (tiles/get-category-tiles params)]
          (add-grid-view-tiles context tiles grid-view))
        false))
    view))

(defn create-tab
  "Creates and adds a tab to the given action bar."
  [^ActionBar action-bar ^String title create-view]
  (try
    (let [^android.app.ActionBar$Tab tab (.newTab action-bar)
          fragment (proxy [Fragment] []
                     (onCreateView [layout-inflater viewgroup bundle]
                       (create-view)))
          listener (proxy [android.app.ActionBar$TabListener] []
                     (onTabSelected [tab ^FragmentTransaction ft]
                       (.add ft (r/get-resource :id :android/content) fragment))
                     (onTabUnselected [tab ^FragmentTransaction ft]
                       (.remove ft fragment))
                     (onTabReselected [tab ^FragmentTransaction ft]
                       (.detach ft fragment)
                       (.attach ft fragment)))]
      (.setText tab title)
      (.setTabListener tab listener)
      (.addTab action-bar tab))
    (catch Exception e nil)))
