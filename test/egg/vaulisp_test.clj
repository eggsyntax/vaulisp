(ns egg.vaulisp-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [is deftest testing]]
            [egg.vaulisp :as vau]))

(defn read-evau
  ([s] (read-evau {} s))
  ([env s] (vau/evau env (edn/read-string s))))

(deftest evau-test
  (testing "Basic math"
    (is (= 23 (read-evau "(+ 3 (* 4 5))")))
    (is (= 42 (read-evau "(* (- (* 3 8) 3) 2)"))))
  (testing "Inner eval"
    ;; Problem: this ends up hitting line 106 as (apply <evau-fn> the-env the-env (list 3 4 5)).
    ;; That's one env too many. Possible solutions:
    ;; - remove the env from the operator on the assumption that it'll always receive an
    ;;   env as the first operand
    ;; - pass to appuy dropping the first *two* from the form, like (appuy car* env (drop 2 form))
    ;; - ...merge the envs? Are they ever actually different?
    ;;
    ;; But is it different in different cases?
    #_(read-evau "(eval {} (list 3 4 5))")
    ))
