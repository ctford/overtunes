(ns overtunes.pitch
  (:use [clojure.set :only [difference union]]))

; Basic intervals
(def unison 0)
(def semitone 1)
(def tone (* semitone 2))
(def octave (* 12 semitone))

; Plumbing
(defmacro defall
  "Define multiple values at once."
  [names values]
  `(do ~@(map
           (fn [name value] `(def ~name ~value))
           names
           (eval values))))

(defn update-values 
  "Apply f to all the values of m corresponding to keys in ks."
  [m [& ks] f] (if ks
                 (update-values
                   (update-in m [(first ks)] f)
                   (rest ks)
                   f)
                 m)) 

(defn update-all [m f] (update-values m (keys m) f))

(defn keys-except
  "Gets the keys from m, excluding any that are present in ks."
  [m ks] (difference (set (keys m)) ks))

(defn grounding [offset]
  "Takes an offset and produces a function for producing concrete sounds."
  (fn
    ([octave-number]
     (+ offset (* octave-number octave)))
    ([octave-number chord & transformations]
     (let [octave-offset (* octave-number octave)
          transform (apply comp (reverse transformations))
          transformed-chord (transform chord)]
       (update-all transformed-chord #(+ offset octave-offset %))))))

; Define a major scale. We could easily define other modes,
; but they aren't needed at present.
(defn scale 
  "Define a scale as a cumulative sum of intervals."
  ([] [])
  ([interval & intervals]
   (cons interval (map #(+ interval %) (apply scale intervals)))))

(def major-scale (zipmap
                   [:i :ii :iii :iv :v :vi :vii :viii]
                   (scale unison tone tone semitone tone tone tone semitone)))

; Name notes
(defall [C C# D D# E F F# G G# A A# B B#]
        (map
          grounding
          (range (+ 1 octave))))

(defall [Cb Db Eb E# Fb Gb Ab Bb]
        [D  C# D# F  E  F# G# A#])

; Operations on intervals
(def sharp #(+ % semitone))
(def flat #(- % semitone))
(def raise #(+ % octave))
(def lower #(- % octave))

; Paramatised transformations on chords
(defn flattened [key] #(update-in % [key] flat))
(defn sharpened [key] #(update-in % [key] sharp))
(defn raised [key] #(update-in % [key] raise))
(defn lowered [key] #(update-in % [key] lower))
(defn omit [key] #(dissoc % key)) 
(defn bass [key] (comp (lowered :bass) #(assoc % :bass (key major-scale)))) 

; Transformations on chords
(def suspended-second #(assoc % :iii (:ii major-scale))) 
(def suspended-fourth #(assoc % :iii (:iv major-scale)))
(def sixth #(assoc % :vi (:vi major-scale)))
(def seventh #(assoc % :vii (+ (:v %) (:iii %))))
(def dominant-seventh #(assoc % :vii (flat (:vii major-scale))))
(def ninth #(assoc (seventh %) :ix (raise (:ii major-scale))))
(def eleventh #(assoc (ninth %) :xi (raise (:iv major-scale))))
(def thirteenth #(assoc (eleventh %) :xiii (raise (:vi major-scale))))
(def fifteenth #(assoc (thirteenth %) :xv (raise (:viii major-scale))))
(def first-inversion #(update-values % (keys-except % [:i]) lower))
(def second-inversion #(update-values % (keys-except % [:i :iii]) lower))
(def third-inversion #(update-values % (keys-except % [:i :iii :v]) lower))
(def with-bass #(assoc % :bass (flat (:i %))))

(defn add [key] #(assoc % key (key (union major-scale (fifteenth %))))) 

; Qualities
(def major (select-keys major-scale [:i :iii :v]))
(def minor ((flattened :iii) major)) 
(def augmented ((sharpened :v) major)) 
(def diminished ((flattened :v) minor)) 
(def power (select-keys major-scale [:i :v :viii]))

; The Hendrix chord
; (chord# (G 4 major ninth (flattened :vii) (sharpened :ix)))
