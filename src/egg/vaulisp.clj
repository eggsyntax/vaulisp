(ns egg.vaulisp
  (:require [clojure.edn :as edn])
  (:gen-class))

;; Much cribbed from http://gliese1337.blogspot.com/2012/04/schrodingers-equation-of-software.html
;;   along with https://axisofeval.blogspot.com and and, most importantly, all
;;   of the late John Shutt's work. This project and all of the above are really
;;   all just explorations of Shutt's insights.

(declare global-env, evau, appuy)

(defn map-evau [env args] (map (partial evau env) args))

(defn str-fn [___ & args]
  ;; (prn "args to str-fn:" args)
  (apply str args))

(defn my-map [env f args]
  ;; (prn "args to my-map:" args)
  (map (fn [x] (f x)) args))

(defn map-fn
  [env & args]
  ;; (prn "args to map-fn:" args)
  ;; (prn "(second args) to map-fn:" (second args))
  (let [f (evau env (first args))]
    (my-map env (partial f env) (second args))))

;; TODO support recursive defs
(defn def-fn
  [env & [name expr]]
  (swap! global-env assoc name (evau env expr))) ; TODO do I really want to evau here?

(def global-env
  (atom {
         'eval  evau
         'apply appuy
         '+     (fn [env & args] (apply + (map-evau env args)))
         '-     (fn [env & args] (apply - (map-evau env args)))
         '*     (fn [env & args] (apply * (map-evau env args)))
         '/     (fn [env & args] (apply / (map-evau env args)))
         '%     (fn [env & args] (map-evau env args))
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
                       (appuy car* env (rest form)) ;; TODO env shouldn't pass down past fn boundaries
                       (catch Exception e
                         (throw (Exception. (str "Unknown function " car "\n" (.getMessage e)))))))
    :else form)) ; keywords, numbers are self-evaluating

(defn appuy
  "Renaming `apply` with analogy to `eval`=>`evau`"
  [operator env operands]
  (try
    (apply operator env operands)
    (catch Exception e (throw (Exception. (str "Unknown operator: " operator "\n" (.getMessage e)))))))

(defn repl
  "Print prompt once before calling."
  []
  (let [env {}
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

(defn -main
  "Run a vau repl"
  [& args]
  (println "Welcome to vaulisp!")
  (println "  q to exit")
  (println)
  (prompt)
  (repl)
  )
