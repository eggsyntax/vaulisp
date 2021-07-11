(ns egg.vaulisp
  (:require [clojure.edn :as edn]
            [clojure.string :as string]
            [clojure.walk :refer [postwalk-replace]]
            [potemkin.collections :refer [def-map-type]])
  (:gen-class))

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
;; convenience we want it ~mutable so that `def` can be top-level (as opposed to
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

(defn =-fn
  "Test for equality" ; TODO handle > 2 args
  [env & args]
  (let [[a b] args]
    (= (evau env a) (evau env b))))

(defn list-fn [_env & args]
  ;; (prn "called list!")
  args)

(defn map-fn
  [env & args]
  ;; (prn "args to map-fn:" args)
  ;; (prn "(second args) to map-fn:" (second args))
  (let [f (evau env (first args))]
    (map (partial f env) (second args))))

(defn do-fn
  [env & args]
  (let [just-eval (butlast args)
        eval-&-return (last args)]
    (run! #(evau env %) just-eval)
    (evau env eval-&-return)))

(defn if-fn
  [env & [test consequent alt]]
  ;; defer directly to clj's `if` which should be just fine for our needs, since
  ;; it already selectively evals its arguments
  (if (evau env test) (evau env consequent) (evau env alt)))

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

(defn applicate-fn
  "What Shutt calls 'wrap' -- convert a vau expression to a lambda expression."
  [closure-env vau-exp-or-reference]
  (let [v' (evau closure-env vau-exp-or-reference)]
    (fn [env & args]
      (apply v' env (map-evau env args)))))

(defmacro vfn
  "Convenience for converting a clojure fn to a vaulisp fn -- useful for
  shorthanding fn defs (see eg the arithmetic operators in global-env below).
  Optionally evaluates its arguments."
  [form eval?]
  (if eval?
    `(fn [env# & args#] (apply ~form (map-evau env# args#)))
    `(fn [env# & args#] (apply ~form args#))))

;; Note: available edn-interpretable symbols (for short aliases) include ! % & ' | ?
;;       - already-used: $ (and of course +, -, *, /, =)
(def global-env
  (atom {'true      true
         'false     false
         'nil       nil
         'eval      #'evau
         '$         #'evau
         'vau       #'vau
         'applicate #'applicate-fn
         '+         (vfn + true)
         '-         (vfn - true)
         '*         (vfn * true)
         '/         (vfn / true)
         '<         (vfn < true)
         '>         (vfn > true)
         '=         #'=-fn
         'prn       (vfn prn true)
         'first     (vfn first false)
         'rest      (vfn next false) ; consider changing to `rest` - (rest [1]) is (); (next [1]) is nil
         'list      #'list-fn
         'map       #'map-fn
         'do        #'do-fn
         'if        #'if-fn
         'str       #'str-fn
         'def       #'def-fn
         }))

(defn prompt []
  (print "vau> ")
  (flush))

(defn evau
  "Central evaluation function, equivalent to the eval in a conventional lisp's metacircular
  evaluator; see eg http://norvig.com/lispy.html. Here the evaluation fn is much smaller,
  because more core definitions can move into the global-env (since there are many fewer
  special forms)."
  [env form]
  (cond
    (symbol? form) (or (get env form) (get @global-env form)) ; [1]
    (list? form)   (let [car (first form)
                         car* (evau env car)]
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

(defn repl
  "Print prompt once before calling."
  []
  (let [env (Env. {})
        exit (atom false)]
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

(defn do-wrap
  "Wrap a string containing one or more expressions in a do."
  [s]
  (str "(do " s " )"))

;; TODO temp to make it easier to run stuff copied from test ns
(defn read-evau
  ([s] (read-evau {} s))
  ([env s] (evau env (edn/read-string (do-wrap s)))))

(defn evau-file [filename]
  (let [init-env {}
        contents (slurp filename)]
    (evau init-env (edn/read-string (do-wrap contents)))))


  )

(evau-file "vausrc/core.vau")

(defn -main
  "Run a vau repl"
  [& args]
  (welcome!)
  (prompt)
  (repl))


'(Footnotes -
  1. alternate approach to checking in env and global env in evau -- instead of
     having :outer in an env be a direct reference to the outer env, it could be
     a thunk that returns it. If (in thunk) it saw that the value was an atom it
     could deref it. The thunk is important here so that it can always get the very
     latest version of the global-env.
  )
