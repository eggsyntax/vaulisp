(ns egg.vaulisp-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [is deftest testing]]
            [egg.vaulisp :as vau]))

(defn read-evau
  ([s] (read-evau {} s))
  ([env s] (vau/evau env (edn/read-string s))))

(deftest env-test
  (let [e1 (vau/->Env {:a :e1-a, :b :e1-b}),
        e2 (vau/->Env {:b :e2-b, :c :e2-c, :outer e1})
        e3 (vau/->Env {:c :e3-c, :d :e3-d, :outer e2})]
    (testing "Basic mappish behavior"
      (is (= (e1 :a) :e1-a))
      (is (= (e1 :b) :e1-b))
      (is (= (e2 :b) :e2-b))
      (is (= (e2 :c) :e2-c)))
    (testing "Searches outer for missing keys"
      (is (= (e2 :a) :e1-a))) ; NOT :e2-a
    (testing "Jumps to multiple layers of outer envs"
      (is (= (e3 :a) :e1-a)))
    (testing "Doesn't look in child envs"
      (is (= (e1 :c) nil)))

    ))

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
