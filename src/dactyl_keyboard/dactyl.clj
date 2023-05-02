(ns dactyl-keyboard.dactyl
  (:refer-clojure :exclude [use import])
  (:require [clojure.core.matrix :refer [array matrix mmul]]
            [scad-clj.scad :refer :all]
            [scad-clj.model :refer :all]
            [unicode-math.core :refer :all]))

(defn deg2rad [degrees]
  (* (/ degrees 180) pi))

;;;;;;;;;;;;;;;;;;;;;;
;; Shape parameters ;;
;;;;;;;;;;;;;;;;;;;;;;

(def nrows 4)
(def ncols 5)

(def α (/ π 12))                        ; curvature of the columns
(def β (/ π 36))                        ; curvature of the rows
(def centerrow (- nrows 3))             ; controls front-back tilt
(def centercol 4)                       ; controls left-right tilt / tenting (higher number is more tenting)
(def tenting-angle (/ π 12))            ; or, change this for more precise tenting control
(def column-style
  (if (> nrows 5) :orthographic :standard))  ; options include :standard, :orthographic, and :fixed
; (def column-style :fixed)

;(defn column-offset [column] (cond
;                               (= column 2) [0 2.82 -4.5]
;                               (>= column 4) [0 -12 5.64]            ; original [0 -5.8 5.64]
;                               :else [0 0 0]))
(defn column-offset [column] (cond
                               (= column 1) [0 0 -1]
                               (= column 2) [0 2.82 -4.5]
                               (= column 3) [2 -2 0]
                               (>= column 4) [4 -16 4.5]            ; original [0 -5.8 5.64]
                               :else [0 0 0]))

(def thumb-offsets [6 -3 7])

(def keyboard-z-offset 9)               ; controls overall height; original=9 with centercol=3; use 16 for centercol=2

(def extra-width 0.7)                   ; extra space between the base of keys; original= 2
(def extra-height 0.5)                  ; original= 0.5

(def wall-z-offset -15)                 ; length of the first downward-sloping part of the wall (negative)
(def wall-xy-offset 5)                  ; offset in the x and/or y direction for the first downward-sloping part of the wall (negative)
(def wall-thickness 2.5)                  ; wall thickness parameter; originally 5

;; Settings for column-style == :fixed 
;; The defaults roughly match Maltron settings
;;   http://patentimages.storage.googleapis.com/EP0219944A2/imgf0002.png
;; Fixed-z overrides the z portion of the column ofsets above.
;; NOTE: THIS DOESN'T WORK QUITE LIKE I'D HOPED.
(def fixed-angles [(deg2rad 10) (deg2rad 10) 0 0 0 (deg2rad -15) (deg2rad -15)])
(def fixed-x [-41.5 -22.5 0 20.3 41.4 65.5 89.6])  ; relative to the middle finger
(def fixed-z [12.1    8.3 0  5   10.7 14.5 17.5])
(def fixed-tenting (deg2rad 0))

;;;;;;;;;;;;;;;;;;;;;;;
;; General variables ;;
;;;;;;;;;;;;;;;;;;;;;;;

(def lastrow (dec nrows))
(def cornerrow (dec lastrow))
(def lastcol (dec ncols))

;(def keycapcolor [47/255 47/255 49/255 1])
;(def platecolor [25/255 25/255 25/255 1])
;(def casecolor
;  [100/255 255/255 14/255 1])

(def keycapcolor [10/255 188/255 198/255 1])
(def platecolor [25/255 25/255 25/255 1])
(def casecolor
  [47/255 47/255 49/255 1])

;;;;;;;;;;;;;;;;;
;; Switch Hole ;;
;;;;;;;;;;;;;;;;;

(def keyswitch-height 14.4) ;; Was 14.1, then 14.25
(def keyswitch-width 14.4)

(def sa-profile-key-height 12.7)

(def plate-thickness 4)
(def mount-width (+ keyswitch-width 3))
(def mount-height (+ keyswitch-height 3))

(def single-plate
  (let [top-wall (->> (cube (+ keyswitch-width 3) 1.5 plate-thickness)
                      (translate [0
                                  (+ (/ 1.5 2) (/ keyswitch-height 2))
                                  (/ plate-thickness 2)]))
        left-wall (->> (cube 1.5 (+ keyswitch-height 3) plate-thickness)
                       (translate [(+ (/ 1.5 2) (/ keyswitch-width 2))
                                   0
                                   (/ plate-thickness 2)]))
        side-nub (->> (binding [*fn* 30] (cylinder 1 2.75))
                      (rotate (/ π 2) [1 0 0])
                      (translate [(+ (/ keyswitch-width 2)) 0 1])
                      (hull (->> (cube 1.5 2.75 plate-thickness)
                                 (translate [(+ (/ 1.5 2) (/ keyswitch-width 2))
                                             0
                                             (/ plate-thickness 2)]))))
        plate-half (union top-wall left-wall (with-fn 100 side-nub))]
    (union plate-half
           (->> plate-half
                (mirror [1 0 0])
                (mirror [0 1 0])))))

;;;;;;;;;;;;;;;;
;; DSA Keycaps ;;
;;;;;;;;;;;;;;;;

(def dsa-cap {1 (let [bl2 (/ 18.5 2)
                      m (/ 12 2)
                      key-cap (hull (->> (polygon [[bl2 bl2] [bl2 (- bl2)] [(- bl2) (- bl2)] [(- bl2) bl2]])
                                         (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                         (translate [0 0 0.05]))
                                    (->> (polygon [[m m] [m (- m)] [(- m) (- m)] [(- m) m]])
                                         (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                         (translate [0 0 8])))]
                  (->> key-cap
                       (translate [0 0 (+ 1.5 plate-thickness)])
                       (color keycapcolor)))})

;;;;;;;;;;;;;;;;;;;;;;;;;
;; Placement Functions ;;
;;;;;;;;;;;;;;;;;;;;;;;;;

(def columns (range 0 ncols))
(def rows (range 0 nrows))

(def cap-top-height (+ plate-thickness sa-profile-key-height))
(def row-radius (+ (/ (/ (+ mount-height extra-height) 2)
                      (Math/sin (/ α 2)))
                   cap-top-height))
(def column-radius (+ (/ (/ (+ mount-width extra-width) 2)
                         (Math/sin (/ β 2)))
                      cap-top-height))
(def column-x-delta (+ -1 (- (* column-radius (Math/sin β)))))

(defn apply-key-geometry [translate-fn rotate-x-fn rotate-y-fn column row shape]
  (let [column-angle (* β (- centercol column))
        placed-shape (->> shape
                          (translate-fn [0 0 (- row-radius)])
                          (rotate-x-fn  (* α (- centerrow row)))
                          (translate-fn [0 0 row-radius])
                          (translate-fn [0 0 (- column-radius)])
                          (rotate-y-fn  column-angle)
                          (translate-fn [0 0 column-radius])
                          (translate-fn (column-offset column)))
        column-z-delta (* column-radius (- 1 (Math/cos column-angle)))
        placed-shape-ortho (->> shape
                                (translate-fn [0 0 (- row-radius)])
                                (rotate-x-fn  (* α (- centerrow row)))
                                (translate-fn [0 0 row-radius])
                                (rotate-y-fn  column-angle)
                                (translate-fn [(- (* (- column centercol) column-x-delta)) 0 column-z-delta])
                                (translate-fn (column-offset column)))
        placed-shape-fixed (->> shape
                                (rotate-y-fn  (nth fixed-angles column))
                                (translate-fn [(nth fixed-x column) 0 (nth fixed-z column)])
                                (translate-fn [0 0 (- (+ row-radius (nth fixed-z column)))])
                                (rotate-x-fn  (* α (- centerrow row)))
                                (translate-fn [0 0 (+ row-radius (nth fixed-z column))])
                                (rotate-y-fn  fixed-tenting)
                                (translate-fn [0 (second (column-offset column)) 0]))]
    (->> (case column-style
           :orthographic placed-shape-ortho
           :fixed        placed-shape-fixed
           placed-shape)
         (rotate-y-fn  tenting-angle)
         (translate-fn [0 0 keyboard-z-offset]))))

(defn key-place [column row shape]
  (apply-key-geometry translate
                      (fn [angle obj] (rotate angle [1 0 0] obj))
                      (fn [angle obj] (rotate angle [0 1 0] obj))
                      column row shape))

(defn rotate-around-x [angle position]
  (mmul
   [[1 0 0]
    [0 (Math/cos angle) (- (Math/sin angle))]
    [0 (Math/sin angle)    (Math/cos angle)]]
   position))

(defn rotate-around-y [angle position]
  (mmul
   [[(Math/cos angle)     0 (Math/sin angle)]
    [0                    1 0]
    [(- (Math/sin angle)) 0 (Math/cos angle)]]
   position))

(defn key-position [column row position]
  (apply-key-geometry (partial map +) rotate-around-x rotate-around-y column row position))

(def key-holes
  (apply union
         (for [column columns
               row rows
               :when (or (.contains [2 3] column)
                         (not= row lastrow))]
           (->> single-plate
                (key-place column row)))))

(def caps
  (apply union
         (for [column columns
               row rows
               :when (or (.contains [2 3] column)
                         (not= row lastrow))]
           (->> (dsa-cap (if (= column 5) 1 1))
                (key-place column row)))))

; (pr (rotate-around-y π [10 0 1]))
; (pr (key-position 1 cornerrow [(/ mount-width 2) (- (/ mount-height 2)) 0]))

;;;;;;;;;;;;;;;;;;;;
;; Web Connectors ;;
;;;;;;;;;;;;;;;;;;;;

(def web-thickness 3.5)
(def post-size 0.1)
(def web-post (->> (cube post-size post-size web-thickness)
                   (translate [0 0 (+ (/ web-thickness -2)
                                      plate-thickness)])))

(def post-adj (/ post-size 2))
(def web-post-tr (translate [(- (/ mount-width 2) post-adj) (- (/ mount-height 2) post-adj) 0] web-post))
(def web-post-tl (translate [(+ (/ mount-width -2) post-adj) (- (/ mount-height 2) post-adj) 0] web-post))
(def web-post-bl (translate [(+ (/ mount-width -2) post-adj) (+ (/ mount-height -2) post-adj) 0] web-post))
(def web-post-br (translate [(- (/ mount-width 2) post-adj) (+ (/ mount-height -2) post-adj) 0] web-post))

(defn triangle-hulls [& shapes]
  (apply union
         (map (partial apply hull)
              (partition 3 1 shapes))))

(def connectors
  (apply union
         (concat
          ;; Row connections
          (for [column (range 0 (dec ncols))
                row (range 0 lastrow)]
            (triangle-hulls
             (key-place (inc column) row web-post-tl)
             (key-place column row web-post-tr)
             (key-place (inc column) row web-post-bl)
             (key-place column row web-post-br)))

          ;; Column connections
          (for [column columns
                row (range 0 cornerrow)]
            (triangle-hulls
             (key-place column row web-post-bl)
             (key-place column row web-post-br)
             (key-place column (inc row) web-post-tl)
             (key-place column (inc row) web-post-tr)))

          ;; Diagonal connections
          (for [column (range 0 (dec ncols))
                row (range 0 cornerrow)]
            (triangle-hulls
             (key-place column row web-post-br)
             (key-place column (inc row) web-post-tr)
             (key-place (inc column) row web-post-bl)
             (key-place (inc column) (inc row) web-post-tl))))))

;;;;;;;;;;;;
;; Thumbs ;;
;;;;;;;;;;;;

(def thumborigin
  (map + (key-position 1 cornerrow [(/ mount-width 2) (- (/ mount-height 2)) 0])
       thumb-offsets))
; (pr thumborigin)

(defn thumbkey-rotation [shape]
  (->> shape
       (rotate (deg2rad  10) [1 0 0])
       (rotate (deg2rad -10) [0 1 0])
       (rotate (deg2rad  10) [0 0 1])))

(defn thumbkey-base [shape]
  (->> shape
       (thumbkey-rotation)
       (translate thumborigin)))

(defn thumb-t1-place [shape]
  (->> shape
       (thumbkey-base)
       (translate [-14 -10 7])))

(defn thumb-t2-place [shape]
  (->> shape
       (thumbkey-base)
       (translate [-34 -13 2])))

(defn thumb-t3-place [shape]
  (->> shape
       (thumbkey-base)
       (translate [-54 -16 -3])))

(defn thumb-b1-place [shape]
  (->> shape
       (thumbkey-base)
       (translate [-9 -29 1])))

(defn thumb-b2-place [shape]
  (->> shape
       (thumbkey-base)
       (translate [-29 -32 -4])))

(defn thumb-b3-place [shape]
  (->> shape
       (thumbkey-base)
       (translate [-49 -35 -9])))

(defn thumb-top-row-layout [shape]
  (union
   (thumb-t1-place shape)
   (thumb-t2-place shape)
   (thumb-t3-place shape)))

(defn thumb-bottom-row-layout [shape]
  (union
   (thumb-b1-place shape)
   (thumb-b2-place shape)
   (thumb-b3-place shape)))

(def thumbcaps
  (union
   (thumb-top-row-layout (dsa-cap 1))
   (thumb-bottom-row-layout (dsa-cap 1))))

(def thumb
  (union
   (thumb-top-row-layout single-plate)
   (thumb-bottom-row-layout single-plate)))

;;;;;;;;;;
;; Case ;;
;;;;;;;;;;

(defn bottom [height p]
  (->> (project p)
       (extrude-linear {:height height :twist 0 :convexity 0})
       (translate [0 0 (- (/ height 2) 10)])))

(defn bottom-hull [& p]
  (hull p (bottom 0.001 p)))

(def left-wall-x-offset 10)
(def left-wall-z-offset  3)

(defn left-key-position [row direction]
  (map - (key-position 0 row [(* mount-width -0.5) (* direction mount-height 0.5) 0]) [left-wall-x-offset 0 left-wall-z-offset]))

(defn left-key-place [row direction shape]
  (translate (left-key-position row direction) shape))

(defn wall-locate1 [dx dy] [(* dx wall-thickness) (* dy wall-thickness) -1])
(defn wall-locate2 [dx dy] [(* dx wall-xy-offset) (* dy wall-xy-offset) wall-z-offset])
(defn wall-locate3 [dx dy] [(* dx (+ wall-xy-offset wall-thickness)) (* dy (+ wall-xy-offset wall-thickness)) wall-z-offset])

(defn wall-brace [place1 dx1 dy1 post1 place2 dx2 dy2 post2]
  (union
   (hull
    (place1 post1)
    (place1 (translate (wall-locate1 dx1 dy1) post1))
    (place1 (translate (wall-locate2 dx1 dy1) post1))
    (place1 (translate (wall-locate3 dx1 dy1) post1))
    (place2 post2)
    (place2 (translate (wall-locate1 dx2 dy2) post2))
    (place2 (translate (wall-locate2 dx2 dy2) post2))
    (place2 (translate (wall-locate3 dx2 dy2) post2)))
   (bottom-hull
    (place1 (translate (wall-locate2 dx1 dy1) post1))
    (place1 (translate (wall-locate3 dx1 dy1) post1))
    (place2 (translate (wall-locate2 dx2 dy2) post2))
    (place2 (translate (wall-locate3 dx2 dy2) post2)))))

(defn key-wall-brace [x1 y1 dx1 dy1 post1 x2 y2 dx2 dy2 post2]
  (wall-brace (partial key-place x1 y1) dx1 dy1 post1
              (partial key-place x2 y2) dx2 dy2 post2))

(def case-walls
  (union
   ; back wall
   (for [x (range 0 ncols)] (key-wall-brace x 0 0 1 web-post-tl x       0 0 1 web-post-tr))
   (for [x (range 1 ncols)] (key-wall-brace x 0 0 1 web-post-tl (dec x) 0 0 1 web-post-tr))
   (key-wall-brace lastcol 0 0 1 web-post-tr lastcol 0 1 0 web-post-tr)
   ; right wall
   (for [y (range 0 lastrow)] (key-wall-brace lastcol y 1 0 web-post-tr lastcol y       1 0 web-post-br))
   (for [y (range 1 lastrow)] (key-wall-brace lastcol (dec y) 1 0 web-post-br lastcol y 1 0 web-post-tr))
   (key-wall-brace lastcol cornerrow 0 -1 web-post-br lastcol cornerrow 1 0 web-post-br)
   ; left wall
   (for [y (range 0 lastrow)] (union (wall-brace (partial left-key-place y 1)       -1 0 web-post (partial left-key-place y -1) -1 0 web-post)
                                     (hull (key-place 0 y web-post-tl)
                                           (key-place 0 y web-post-bl)
                                           (left-key-place y  1 web-post)
                                           (left-key-place y -1 web-post))))
   (for [y (range 1 lastrow)] (union (wall-brace (partial left-key-place (dec y) -1) -1 0 web-post (partial left-key-place y  1) -1 0 web-post)
                                     (hull (key-place 0 y       web-post-tl)
                                           (key-place 0 (dec y) web-post-bl)
                                           (left-key-place y        1 web-post)
                                           (left-key-place (dec y) -1 web-post))))
   (wall-brace (partial key-place 0 0) 0 1 web-post-tl (partial left-key-place 0 1) 0 1 web-post)
   (wall-brace (partial left-key-place 0 1) 0 1 web-post (partial left-key-place 0 1) -1 0 web-post)
   ; front wall
   (key-wall-brace lastcol 0 0 1 web-post-tr lastcol 0 1 0 web-post-tr)
   (key-wall-brace 3 lastrow   0 -1 web-post-bl 3 lastrow 0.5 -1 web-post-br)
   (key-wall-brace 3 lastrow 0.5 -1 web-post-br 4 cornerrow 1 -1 web-post-bl)
   (for [x (range 4 ncols)] (key-wall-brace x cornerrow 0 -1 web-post-bl x       cornerrow 0 -1 web-post-br))
   (for [x (range 5 ncols)] (key-wall-brace x cornerrow 0 -1 web-post-bl (dec x) cornerrow 0 -1 web-post-br))
   ; thumb walls
   (hull
    (left-key-place cornerrow -1 web-post)
    (thumb-t3-place web-post-tr)
    (thumb-t3-place web-post-tl))
   (hull
    (left-key-place cornerrow -1 web-post)
    (left-key-place cornerrow -1 (translate (wall-locate1 -1 0) web-post))
    (left-key-place cornerrow -1 (translate (wall-locate2 -1 0) web-post))
    (left-key-place cornerrow -1 (translate (wall-locate3 -1 0) web-post))
    (thumb-t3-place (translate (wall-locate1 -1 0) web-post-tl))
    (thumb-t3-place web-post-tl))
   (hull
    (left-key-place cornerrow -1 (translate (wall-locate2 -1 0) web-post))
    (left-key-place cornerrow -1 (translate (wall-locate3 -1 0) web-post))
    (thumb-t3-place web-post-tl)
    (thumb-t3-place (translate (wall-locate1 -1 0) web-post-tl))
    (thumb-t3-place (translate (wall-locate2 -1 0) web-post-tl))
    (thumb-t3-place (translate (wall-locate3 -1 0) web-post-tl)))
   (bottom-hull
    (left-key-place cornerrow -1 (translate (wall-locate2 -1 0) web-post))
    (left-key-place cornerrow -1 (translate (wall-locate3 -1 0) web-post))
    (thumb-t3-place (translate (wall-locate2 -1 0) web-post-tl))
    (thumb-t3-place (translate (wall-locate3 -1 0) web-post-tl)))
   (wall-brace thumb-t3-place -1  0 web-post-tl thumb-t3-place -1  0 web-post-bl)
   (wall-brace thumb-t3-place -1  0 web-post-bl thumb-b3-place -1  0 web-post-tl)
   (wall-brace thumb-b3-place -1  0 web-post-tl thumb-b3-place -1  0 web-post-bl)
   (wall-brace thumb-b3-place -1  0 web-post-bl thumb-b3-place  0 -1 web-post-bl)
   (wall-brace thumb-b3-place  0 -1 web-post-bl thumb-b3-place  0 -1 web-post-br)
   (wall-brace thumb-b3-place  0 -1 web-post-br thumb-b2-place  0 -1 web-post-bl)
   (wall-brace thumb-b2-place  0 -1 web-post-bl thumb-b2-place  0 -1 web-post-br)
   (wall-brace thumb-b2-place  0 -1 web-post-br thumb-b1-place  0 -1 web-post-bl)
   (wall-brace thumb-b1-place  0 -1 web-post-bl thumb-b1-place  0 -1 web-post-br)
   (wall-brace thumb-b1-place  0 -1 web-post-br (partial key-place 3 lastrow)  0 -1 web-post-bl)

   ; thumb fill to match plate
   ;(hull
   ; (thumb-b3-place (translate (wall-locate1 -1 0) web-post-tl))
   ; (thumb-b3-place (translate (wall-locate1 -1 0) web-post-bl))
   ; (thumb-b3-place (translate (wall-locate1 0 -1) web-post-bl))
   ; (thumb-b3-place (translate (wall-locate3 0 -1) web-post-bl))
   ; (translate [-92.6341, -79.1564 0] (cube post-size post-size post-size))
   ; (translate [-92.6906, -79.1101 0] (cube post-size post-size post-size))
   ; (translate [-93.05110000000001, -75.61320000000001 0] (cube post-size post-size post-size))
   ; (translate [-93.2728, -73.46250000000001 0] (cube post-size post-size post-size))
   ; (translate [-93.2131, -73.35420000000001 0] (cube post-size post-size post-size))
   ; (translate [-89.9849, -67.4988 0] (cube post-size post-size post-size))

   ; (translate [-88, -60.5 0] (cube post-size post-size post-size)))
    ; thumb connect insert (inner)
  ; (bottom-hull
  ;  (translate [0 0 -20] (left-key-place cornerrow -1 (translate (wall-locate3 -1 0) web-post)))
  ;  (thumb-t3-place (translate (wall-locate3 -1 0) web-post-tl))
  ;  (translate [0 0 3] (thumb-t3-place (translate (wall-locate3 -1 0) web-post-bl)))
  ;  (translate [12 0 3] (thumb-t3-place (translate (wall-locate3 -1 0) web-post-tl))))
  ;  ; thumb connect insert (inner)
  ; (bottom-hull
  ;  (thumb-b1-place (translate (wall-locate3 0 -1) web-post-br))
  ;  (translate [0 6 0] (thumb-b1-place (translate (wall-locate3 0 -1) web-post-br)))
  ;  (key-place 3 lastrow (translate (wall-locate3 0 -1) web-post-bl)))
   ))
(def thumb-connectors
  (union
   (triangle-hulls
    (thumb-t2-place web-post-tr)
    (thumb-t1-place web-post-tl)
    (thumb-t2-place web-post-br)
    (thumb-t1-place web-post-bl)
    (thumb-b1-place web-post-tl)
    (thumb-t1-place web-post-br)
    (thumb-b1-place web-post-tr))
   (triangle-hulls
    (thumb-t3-place web-post-br)
    (thumb-t2-place web-post-bl)
    (thumb-b2-place web-post-tl)
    (thumb-t2-place web-post-br)
    (thumb-b2-place web-post-tr)
    (thumb-b1-place web-post-tl)
    (thumb-b2-place web-post-br)
    (thumb-b1-place web-post-bl))
   (triangle-hulls
    (thumb-t3-place web-post-bl)
    (thumb-b3-place web-post-tl)
    (thumb-t3-place web-post-br)
    (thumb-b3-place web-post-tr)
    (thumb-b2-place web-post-tl)
    (thumb-b3-place web-post-br)
    (thumb-b2-place web-post-bl))
   (triangle-hulls
    (thumb-t1-place web-post-br)
    (thumb-t1-place web-post-tr)
    (key-place 2 lastrow web-post-bl)
    (key-place 1 cornerrow web-post-br)
    (key-place 2 lastrow web-post-tl))
   (triangle-hulls
    (thumb-t1-place web-post-tl)
    (key-place 0 cornerrow web-post-br)
    (thumb-t2-place web-post-tr)
    (key-place 0 cornerrow web-post-bl)
    (thumb-t2-place web-post-tl)
    (left-key-place cornerrow -1 web-post)
    (thumb-t3-place web-post-tr)
    (thumb-t2-place web-post-tl)
    (thumb-t3-place web-post-br)
    (thumb-t2-place web-post-bl))
   (triangle-hulls
    (key-place 1 cornerrow web-post-br)
    (thumb-t1-place web-post-tr)
    (key-place 0 cornerrow web-post-br)
    (thumb-t1-place web-post-tl))
   (triangle-hulls
    (key-place 2 lastrow web-post-br)
    (thumb-t1-place web-post-br)
    (key-place 2 lastrow web-post-bl))
   (triangle-hulls
    (key-place 2 lastrow web-post-br)
    (key-place 3 lastrow web-post-bl)
    (thumb-t1-place web-post-br)
    (thumb-b1-place web-post-tr))
   (triangle-hulls
    (key-place 3 lastrow web-post-bl)
    (thumb-b1-place web-post-br)
    (thumb-b1-place web-post-tr))
   (triangle-hulls
    (key-place 4 cornerrow web-post-bl)
    (key-place 3 cornerrow web-post-br)
    (key-place 3 lastrow web-post-tr)
    (key-place 3 cornerrow web-post-bl)
    (key-place 3 lastrow web-post-tl)
    (key-place 2 lastrow web-post-tr)
    (key-place 3 lastrow web-post-bl)
    (key-place 2 lastrow web-post-br))
   (triangle-hulls
    (key-place 1 cornerrow web-post-br)
    (key-place 2 lastrow web-post-tl)
    (key-place 2 cornerrow web-post-bl)
    (key-place 2 lastrow web-post-tr)
    (key-place 2 cornerrow web-post-br)
    (key-place 3 cornerrow web-post-bl))
   (triangle-hulls
    (key-place 3 lastrow web-post-tr)
    (key-place 3 lastrow web-post-br)
    (key-place 3 lastrow web-post-tr)
    (key-place 4 cornerrow web-post-bl))))

(def rj9-mount-cyl-size (cylinder [1.625 2] 3.51))
(def rj9-box-thickness [4 -0.1 6])
(def rj9-size-exact [11.18 20.60 19.5]); 16 hight + 3.5 pins
(def rj9-size [11.2 14 17])
(def rj9-size-outer (map + rj9-size rj9-box-thickness))
(def rj9-start  (map + [0 5 0] (key-position 0 0 (map + (wall-locate2 0 1) [0 (/ mount-height  2) 0]))))
(def rj9-position  [(first rj9-start) (- (second rj9-start) (/ (second rj9-size) 2)) (/ (nth rj9-size-outer 2) 2)])
(def rj9-space  (translate rj9-position (union
                                         (translate [3.31 (- (/ (second rj9-size) 2) 9.2) 7.15] rj9-mount-cyl-size)
                                         (translate [-3.31 (- (/ (second rj9-size) 2) 9.2) 7.15] rj9-mount-cyl-size)
                                         (translate [3.31 (- (/ (second rj9-size) 2) 8) 7.15] rj9-mount-cyl-size)
                                         (translate [-3.31 (- (/ (second rj9-size) 2) 8) 7.15] rj9-mount-cyl-size)
                                         (translate [0 0 -3.01] (apply cube rj9-size)))))
(def rj9-holder (translate rj9-position (apply cube rj9-size-outer)))

(def usb-holder-thickness [4 -0.1 4])
(def usb-holder-size [5.3 10.0 10.2])
(def usb-holder-size-outer (map + usb-holder-size usb-holder-thickness))
(def usb-holder-start (map + [-10 5 0] (key-position 0 0 (map + (wall-locate2 0 1) [0 (/ mount-height 2) 0]))))
(def usb-holder-position [(first usb-holder-start)
                          (- (second usb-holder-start) (/ (second usb-holder-size) 2))
                          ;(/ (nth usb-holder-size-outer 2) 2)
                          13])
(def usb-holder (translate (map + [0 0 0] usb-holder-position) (apply cube usb-holder-size-outer)))
(def usb-holder-hole (translate usb-holder-position (apply cube (map + [0 5 0] usb-holder-size))))

(def power-switch-thickness [4 -0.1 4])
(def power-switch-size [7.4 8.0 16.2])
(def power-switch-size-outer (map + power-switch-size power-switch-thickness))
(def power-switch-start (map + [-1.5 5 0] (key-position 0 0 (map + (wall-locate2 0 1) [0 (/ mount-height 2) 0]))))
(def power-switch-position [(first power-switch-start)
                            (- (second power-switch-start) (/ (second power-switch-size) 2))
                          ;(/ (nth power-switch-size-outer 2) 2)
                            10.0])
(def power-switch (translate (map + [0 0 0] power-switch-position) (apply cube power-switch-size-outer)))
(def power-switch-hole (translate power-switch-position (apply cube (map + [0 5 0] power-switch-size))))

(def reset-switch-thickness [4 -0.1 4])
(def reset-switch-size [6.8 8.0 6.8])
(def reset-switch-size-outer (map + reset-switch-size reset-switch-thickness))
(def reset-switch-start (map + [+7 5 0] (key-position 0 0 (map + (wall-locate2 0 1) [0 (/ mount-height 2) 0]))))
(def reset-switch-position [(first reset-switch-start)
                            (- (second reset-switch-start) (/ (second reset-switch-size) 2))
                          ;(/ (nth reset-switch-size-outer 2) 2)
                            5])
(def reset-switch (translate (map + [0 0 0] reset-switch-position) (apply cube reset-switch-size-outer)))
(def reset-switch-hole (translate (map + [0 4 0] reset-switch-position) (apply cube (map + [0 0 0] reset-switch-size))))
;innder hold
(def reset-switch-hole-2 (translate reset-switch-position (apply cube (map + [-2 0 -0.2] reset-switch-size))))

(def teensy-width 20)
(def teensy-pcb-thickness 6)
(def teensy-holder-width  (+ 7 teensy-pcb-thickness))
(def teensy-holder-top-length 18)
(def teensy-top-xy (key-position 0 (- centerrow 1) (wall-locate3 -1 0)))
(def teensy-bot-xy (key-position 0 (+ centerrow 1) (wall-locate3 -1 0)))
(def teensy-holder-length (- (second teensy-top-xy) (second teensy-bot-xy)))
(def teensy-holder-offset (/ teensy-holder-length -2))
(def teensy-holder-top-offset (- (/ teensy-holder-top-length 2) teensy-holder-length))

(def teensy-holder
  (->>
   (union
    (->> (cube 3 teensy-holder-length (+ 6 teensy-width))
         (translate [1.5 teensy-holder-offset 0]))
    (->> (cube (+ 8 teensy-pcb-thickness) teensy-holder-length 3)
         (translate [(+ (/ teensy-pcb-thickness 2) 7) teensy-holder-offset (- -1.5 (/ teensy-width 2))]))
    (->> (cube teensy-pcb-thickness teensy-holder-top-length 3)
         (translate [(+ (/ teensy-pcb-thickness 2) 3) teensy-holder-top-offset (+ 1.5 (/ teensy-width 2))])))
   (translate [(- teensy-holder-width) 0 0])
   (translate [2.4 0 0])
   (translate [(first teensy-top-xy)
               (- (second teensy-top-xy) 1)
               (/ (+ 6 teensy-width) 2)])))

(defn screw-insert-shape [bottom-radius top-radius height]
  (union (cylinder [bottom-radius top-radius] height)
         (translate [0 0 (/ height 2)] (sphere top-radius))))

(defn screw-insert [column row bottom-radius top-radius height]
  (let [shift-right   (= column lastcol)
        shift-left    (= column 0)
        shift-up      (and (not (or shift-right shift-left)) (= row 0))
        shift-down    (and (not (or shift-right shift-left)) (>= row lastrow))
        position      (if shift-up     (key-position column row (map + (wall-locate2  0  1) [0 (/ mount-height 2) 0]))
                          (if shift-down  (key-position column row (map - (wall-locate2  0 -1) [0 (/ mount-height 2) 0]))
                              (if shift-left (map + (left-key-position row 0) (wall-locate3 -1 0))
                                  (key-position column row (map + (wall-locate2  1  0) [(/ mount-width 2) 0 0])))))]
    (->> (screw-insert-shape bottom-radius top-radius height)
         (translate [(first position) (second position) (/ height 2)]))))

(defn screw-insert-all-shapes [bottom-radius top-radius height]
  (union
   (screw-insert 0 0         bottom-radius top-radius height) ; inner top
   (screw-insert 0 (- lastrow 0.5)   bottom-radius top-radius height) ; inner bottom (thumb)
   (screw-insert 2 (+ lastrow 0.5)  bottom-radius top-radius height) ; mid bottom (thumb)
   (screw-insert 3 0         bottom-radius top-radius height) ; mid top
   (screw-insert lastcol 1   bottom-radius top-radius height); outer
   ))
(def screw-insert-height 3.8)
(def screw-insert-bottom-radius (/ 5.31 2))
(def screw-insert-top-radius (/ 5.1 2))
(def screw-insert-holes  (screw-insert-all-shapes screw-insert-bottom-radius screw-insert-top-radius screw-insert-height))
(def screw-insert-outers (screw-insert-all-shapes (+ screw-insert-bottom-radius 1.6) (+ screw-insert-top-radius 1.6) (+ screw-insert-height 1.5)))
(def screw-insert-screw-holes  (screw-insert-all-shapes 1.7 1.7 350))

(def plate-shape
  [[-82, 22]
   [-81, 31]
   [-70, 41]
   [-35, 41]
   [-32, 44]
   [-15, 44]
   [-10, 41]
   [7, 40]
   [13, 29]
   [30, 28]
   [37, 21]
   [35, -12]

   [37, -46]
   [41, -64]
   [48, -78]
   [65, -89]
   [70, -90]
   [70, -90]
   [76, -92]
   ; top outer edge
   [79, -96]
   [80, -180]
   [79, -185]
   [71, -191]
   [-50, -190]
   [-56, -187]
   [-58, -185]
   [-60, -181]
   [-59, -110]
   ; thumb
   [-61.0333, -103.124]
   [-64.6126, -99.95910000000001]
   [-74.3008, -91.3929]
   [-77.4233, -91.05]
   [-79.22020000000001, -90.1725]
   [-79.2465, -90.15089999999999]
   [-83.5008, -86.6571]
   ; start - plate larger than case
   [-92.6131, -79.1737]
   [-92.6341, -79.1564]
   [-92.6906, -79.1101]
   [-93.05110000000001, -75.61320000000001]
   [-93.2728, -73.46250000000001]
   [-93.2131, -73.35420000000001]
   [-89.9849, -67.4988]
   ; end - plate larger than case

   [-86.17, -60.5793]
   [-86.17, -60.5793]
   [-85.4941, -59.7142]
   [-85.4939, -59.714]
   [-83.44580000000001, -57.0925]
   [-84.3565, -54.8219]
   [-87.3252, -47.4205]
   [-86.812, -46.7948]
   [-86.812, -46.7947]
   [-86.0693, -45.889]
   [-81.7552, -42.2691]
   [-81.6729, -42.2001]
   [-81.5772, -41.7806]
   [-81.5694, -41.7467]
   [-81.54770000000001, -41.7194]
   [-80.38930000000001, -40.2668]
   [-78.7154, -39.4607]
   [-78.684, -39.4456]
   [-78.39019999999999, -39.4456]
   [-77.8158, -38.9635]
   [-79.69589999999999, -34.2967]
   [-81.0166, -31.0184]
   [-81.75190000000001, -25.53]
   [-83.2683, -14.2113]
;;
   [-83.5 -9]
   [-83.5 9]])

;(def dongle-controller-height 3.2)
(def dongle-controller-height (+ 1.6 2.54 0.5)) ; 2.56 is a thickness of breakout pin mount and 1.6 is an estimation of thickness of a PCB itself
(def dongle-controller-size [33 19.3 dongle-controller-height])
(def dongle-thickness 4)
(def dongle-half-hight  (+ dongle-thickness (/ dongle-controller-height 2)))
(def dongle-layer-size [(+ (* dongle-thickness 2)  (first dongle-controller-size))
                        (+ (* dongle-thickness 2)  (second dongle-controller-size))
                        dongle-thickness])
(def dongle-screw-mount-element (difference
                                 (union (cylinder 10  dongle-half-hight))
                                 (translate [5 0 0] (cylinder 1.7 100))
                                 (translate [-50 0 0] (cube 100 100 100)) ; half cylinder
                                 ))
(def dongle-screw-mount (translate [(/ (nth dongle-layer-size 0) 2) 0 0] dongle-screw-mount-element))
(def dongle-screw-insert-element (difference
                                  (union (cylinder 10  dongle-half-hight))
                                  (translate [5 0 1] (cylinder (/ 7.31 2) dongle-half-hight))
                                  (translate [-50 0 0] (cube 100 100 100)) ; half cylinder
                                  ))
(def dongle-screw-insert (translate [(/ (nth dongle-layer-size 0) 2) 0 0] dongle-screw-insert-element))
(def dongle-row-thickness 0.8)
(def dongle-bottom-walls (translate
                          [0 0 (+ (/ dongle-controller-height 2) dongle-thickness)]
                          (union
                           (translate
                            [0 (/ (+ dongle-thickness (second dongle-controller-size)) 2) 0]
                            (cube (first dongle-layer-size) dongle-thickness dongle-controller-height))
                           (translate
                            [0 (/ (+ dongle-thickness (second dongle-controller-size)) 2) (/ (+ dongle-controller-height dongle-row-thickness) 2)]
                            (cube (first dongle-controller-size) dongle-row-thickness dongle-row-thickness))
                           (translate
                            [0 (* (/ (+ dongle-thickness (second dongle-controller-size)) 2) -1) 0]
                            (cube (first dongle-layer-size) dongle-thickness dongle-controller-height))
                           (translate
                            [0 (* (/ (+ dongle-thickness (second dongle-controller-size)) 2) -1) (/ (+ dongle-controller-height dongle-row-thickness) 2)]
                            (cube (first dongle-controller-size) dongle-row-thickness dongle-row-thickness))
                           (translate
                            [(/ (+ dongle-thickness (first dongle-controller-size)) 2) 0 0]
                            (cube dongle-thickness (second dongle-layer-size) dongle-controller-height)))))

(def dongle-top-walls-cutout (translate
                              [0 0 (- (/ dongle-thickness 2) dongle-row-thickness)]
                              (union

                               (translate
                                [0 (/ (+ dongle-thickness (second dongle-controller-size)) 2) (/ dongle-row-thickness 2)]
                                (cube (+ (first dongle-controller-size) 1) (+ dongle-row-thickness 0.05) (+ dongle-row-thickness 0.2)))
                               (translate
                                [0 (* (/ (+ dongle-thickness (second dongle-controller-size)) 2) -1) (/ dongle-row-thickness  2)]
                                (cube (+ (first dongle-controller-size) 1) (+ dongle-row-thickness 0.05) (+ dongle-row-thickness  0.2))))))

(def dongle-breakout-pin-full-length 11.43)
(def dongle-breakout-pin-partembed-in-layer (/ (- dongle-breakout-pin-full-length dongle-controller-height) 2))
(def dongle-controller-pin-distance 2.54)
(def dongle-controller-pin-rows-distance (* 6 dongle-controller-pin-distance))
(def dongle-controller-pin (cylinder 1 (+ 0.01 dongle-breakout-pin-partembed-in-layer)))
(def dongle-pins-row
  (union
   (translate [(* -7 dongle-controller-pin-distance) 0 0] dongle-controller-pin)
   (translate [(* -6 dongle-controller-pin-distance) 0 0] dongle-controller-pin)
   (translate [(* -5 dongle-controller-pin-distance) 0 0] dongle-controller-pin)
   (translate [(* -4 dongle-controller-pin-distance) 0 0] dongle-controller-pin)
   (translate [(* -3 dongle-controller-pin-distance) 0 0] dongle-controller-pin)
   (translate [(* -2 dongle-controller-pin-distance) 0 0] dongle-controller-pin)
   (translate [(* -1 dongle-controller-pin-distance) 0 0] dongle-controller-pin)
   (translate [(* 0 dongle-controller-pin-distance) 0 0] dongle-controller-pin)
   (translate [(* 1 dongle-controller-pin-distance) 0 0] dongle-controller-pin)
   (translate [(* 2 dongle-controller-pin-distance) 0 0] dongle-controller-pin)
   (translate [(* 3 dongle-controller-pin-distance) 0 0] dongle-controller-pin)
   (translate [(* 4 dongle-controller-pin-distance) 0 0] dongle-controller-pin)
   (translate [(* 5 dongle-controller-pin-distance) 0 0] dongle-controller-pin)
   (translate [(* 6 dongle-controller-pin-distance) 0 0] dongle-controller-pin)))

(def dongle-controller-pins-cutout (translate
                                    [0 0 (+
                                          (- dongle-thickness  dongle-breakout-pin-partembed-in-layer)
                                          (/ dongle-breakout-pin-partembed-in-layer 2))]
                                    (union
                                     (translate [0 (/ dongle-controller-pin-rows-distance 2) 0] dongle-pins-row)
                                     (translate [0 (* (/ dongle-controller-pin-rows-distance 2) -1) 0] dongle-pins-row))))

(def dongle-bottom-layer (union
                          (translate [0 0 (/ dongle-half-hight 2)] dongle-screw-insert)
                          dongle-bottom-walls
                          (difference
                           (translate [0 0 (/ dongle-thickness 2)] (apply cube dongle-layer-size))
                           dongle-controller-pins-cutout)))

(def dongle-top-layer (union
                       (translate [0 0 (/ dongle-half-hight 2)] dongle-screw-mount)
                       (difference
                        (translate
                         [0 0 (/ dongle-thickness 2)]
                         (difference
                          (apply cube dongle-layer-size)
                          dongle-top-walls-cutout
                          ; cutout for reset button
                          (translate
                           [-11.5 (/ dongle-controller-pin-rows-distance -2) 0]
                           (cube (first reset-switch-size) (nth reset-switch-size 2) 10))))
                        dongle-controller-pins-cutout)))

(def dongle (union
             (translate [0 30 0] dongle-top-layer)
             dongle-bottom-layer))

(def plate (color platecolor
                  (->> (polygon plate-shape)
                       (extrude-linear {:height 5 :twist 0 :convexity 10})
                       (translate [-20 0 -2.5]))))

(def model-right  (union
                   ;plate
                   (difference
                    (union
                     (color
                      casecolor
                      (union
                       key-holes
                       connectors
                       thumb
                       thumb-connectors
                       (difference (union case-walls
                                          screw-insert-outers
                                          teensy-holder
                                          power-switch
                                          reset-switch
                                          ;rj9-holder
                                          usb-holder)
                                   ;rj9-space
                                   usb-holder-hole
                                   power-switch-hole
                                   reset-switch-hole
                                   reset-switch-hole-2
                                   screw-insert-holes)))
                    ; (union thumbcaps caps)
                     )

                    (translate [0 0 -20] (cube 350 350 40)))))

(spit "things/right.scad"
      (write-scad model-right))

(spit "things/left.scad"
      (write-scad (mirror [-1 0 0] model-right)))

(spit "things/dongle.scad"
      (write-scad dongle))

(spit "things/right-test.scad"
      (write-scad
       (union
        key-holes
        connectors
        thumb
        thumb-connectors
        case-walls
        thumbcaps
        caps
        teensy-holder
        rj9-holder
        usb-holder-hole)))

(spit "things/right-plate.scad"
      (write-scad
       (cut
        (translate [0 0 -0.1]
                   (difference (union case-walls
                                      teensy-holder
                                          ; rj9-holder
                                      screw-insert-outers)
                               (translate [0 0 -10] screw-insert-screw-holes))))))

(spit "things/test.scad"
      (write-scad
       (difference usb-holder usb-holder-hole)))

(defn -main [dum] 1)  ; dummy to make it easier to batch
