(ns egg.vaulisp
  (:require [clojure.edn :as edn]
            [clojure.string :as string]
            [clojure.walk :refer [postwalk-replace]]
            [potemkin.collections :refer [def-map-type]])
  (:gen-class))

;; Much cribbed from http://gliese1337.blogspot.com/2012/04/schrodingers-equation-of-software.html
;;   along with https://axisofeval.blogspot.com and and, most importantly, all
;;   of the late John Shutt's work. This project and all of the above are really
;;   all just explorations of Shutt's insights.

(declare global-env, evau)

(defn welcome! []
  (println "Welcome to vaulisp!")
  (println "  q to exit")
  (println))

;; An Env is a map that has one special property in order to enable static
;; scoping: it contains a reference to an outer Env. If a key isn't found in the
;; current, innermost Env, it'll check the parent, the parent's parent, and so
;; on.
;;
;; Note that we treat the global env separately, because for the sake of
;; convenience we want it mutable so that `def` can be top-level (as opposed to
;; `let` which adds a binding in the current scope).
;;
;; We can create our env-map using clj's built-in capabilities, but it's a real
;; pain because we have to implement dozens of overlapping functions. Potemkin
;; handles the hassles for us. https://github.com/clj-commons/potemkin
(def-map-type Env [m]
  ;; get, assoc, dissoc, keys, meta, with-meta
  (get [_ k default] (or (get m k)
                         (get (:outer m) k)
                         default))
  (assoc [_ k v] (Env. (assoc m k v)))
  (dissoc [_ k] (if (= :outer k)
                  (throw (Exception. "Can't remove the outer environment from an env!"))
                  (Env. (dissoc m k))))
  (keys [_] (keys m))
  (meta [_] (meta m))
  (with-meta [_ mta] (with-meta (Env. m) mta)))

(defn map-evau
  [env args]
  (map (partial evau env) args))

(defn str-fn
  "Worth the annoyance of not evaling args? Can just app/applicate/$ (not sure
  yet what terminology I prefer there)"
  [___ & args]
  ;; (prn "args to str-fn:" args)
  (apply str args))

(defn map-fn
  [env & args]
  ;; (prn "args to map-fn:" args)
  ;; (prn "(second args) to map-fn:" (second args))
  (let [f (evau env (first args))]
    (map (partial f env) (second args))))

;; TODO support recursive defs
(defn def-fn
  [env & [name expr]]
  ;; (prn "defing" name)
  (swap! global-env assoc name (evau env expr))
  (get @global-env name))

(defn vau
  "The operative equivalent of `lambda` (or in clj, `fn`), ie a version of
  lambda that doesn't eval its args. This, rather than lambda, is the fundamental
  primitive of a fexpr-based lisp."
  [closure-env formal-args body]
  (fn closure [call-env & args]
    (let [env-args (zipmap formal-args args)
          new-env (Env. (merge closure-env env-args))
          body-substituted (postwalk-replace env-args body)]
      (evau new-env body-substituted))))

(def global-env
  (atom {
         'eval  evau
         'vau   vau
         '+     (fn [env & args] (apply + (map-evau env args)))
         '-     (fn [env & args] (apply - (map-evau env args)))
         '*     (fn [env & args] (apply * (map-evau env args)))
         '/     (fn [env & args] (apply / (map-evau env args)))
         '%     (fn [env & args] (map-evau env args))
         '=     (fn [env & args] (let [[a b] args]
                                   (= (evau env a) (evau env b))))
         'str   str-fn
         'def   def-fn
         ;; TODO no need for inc here, just wanted another 1-arg fn for test purposes
         'inc   (fn [env & args] (+ 1 (evau env (first args))))
         'map   map-fn
         'list  (fn [___ & args] args)
         }))

(defn prompt []
  (print "vau> ")
  (flush))

(defn evau [env form]
  (cond
    (symbol? form) (or (get env form) (get @global-env form))
    (list? form)   (let [car (first form)
                         ;; _ (prn "car:" car)
                         car* (evau env car)
                         ;; _ (prn "car*:" car*)
                         ]
                     (try
                       (apply car* env (rest form)) ;; TODO env shouldn't pass down past fn boundaries
                       (catch Exception e
                         (throw (Exception. (str "Unknown function " car "\n" (.getMessage e)))))))
    :else form)) ; keywords, numbers are self-evaluating

(defn evau-str
  "Accept a string containing vaulisp code (perhaps read from a file), split
  into separate lines and parsee/evau them."
  [env s]
  (run!
   (comp (partial evau env) edn/read-string)
   (string/split-lines s)))

(declare vau-core)

(defn repl
  "Print prompt once before calling."
  []
  (let [env (Env. {})
        exit (atom false)]
    (evau-str env vau-core) ; load core defs
    (while (not @exit)
      (try
        (let [in (read-line)
              ast (edn/read-string in)]
          (when ast
            ;; (prn ast)
            ;; (prn (map type ast))
            (if (= 'q ast)
              (reset! exit true)
              (prn (evau env ast))))
          (prompt))
        (catch Exception e
          (prn)
          (println "Error!")
          (println (.getMessage e))
          (prompt)
          (flush))))))

(defn -main
  "Run a vau repl"
  [& args]
  (welcome!)
  (prompt)
  (repl)
  )

;;;;; The following is additional defs written in vaulisp:

(def vau-core
  (str "

(def x 3)
(def y 18)

"))
