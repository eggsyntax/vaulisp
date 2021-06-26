(ns egg.vaulisp
  (:require [clojure.edn :as edn])
  (:gen-class))

;; Code convention: foo is a symbol, foo* is the evaluation of foo.
;; Much cribbed from http://gliese1337.blogspot.com/2012/04/schrodingers-equation-of-software.html
;;   along with https://axisofeval.blogspot.com and and, most importantly, all
;;   of the late John Shutt's work. This project and all of the above are really
;;   all just explorations of Shutt's insights.

(declare evau, appuy)

(defn map-evau [env args] (map #(evau % env) args))

(def default-env
  (atom {
         '+ (fn [env & args] (apply + (map-evau env args)))
         '- (fn [env & args] (apply - (map-evau env args)))
         '* (fn [env & args] (apply * (map-evau env args)))
         '/ (fn [env & args] (apply / (map-evau env args)))
         }))

(defn prompt []
  (print "vau> ")
  (flush))

(defn evau [form env]
  (cond
    (symbol? form) (get env form)
    (list? form)   (let [car (first form)
                         _ (prn "car:" car)
                         car* (evau car env)
                         _ (prn "car*:" car*)
                         ]
                     (try
                       (appuy car* env (rest form))
                       (catch Exception e
                         (throw (Exception. (str "Unknown function " car "\n" (.getMessage e)))))))
    :else form)) ; keywords, numbers already evaluated

(defn appuy
  "Renaming `apply` with analogy to `eval`=>`evau`"
  [operator env operands]
  (try
    (apply operator env operands)
    (catch Exception e (throw (Exception. (str "Unknown operator: " operator "\n" (.getMessage e)))))))

(defn repl
  "Print prompt once before calling."
  []
  (let [env @default-env
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
              (prn (evau ast env))))
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
  (println "  Ctrl-C to exit")
  (println)
  (prompt)
  (repl)
  )
