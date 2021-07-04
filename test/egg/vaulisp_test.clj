(ns egg.vaulisp-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [is deftest testing use-fixtures]]
            [egg.vaulisp :as vau]))

;; Load core vaulisp fns into global-env (this is normally done by vau/repl)
(use-fixtures
  :once
  (fn [f] (vau/evau-str {} vau/vau-core) (f)))

(defn read-evau
  "Convenience fn: the RE part of REPL"
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
      (is (= (e1 :c) nil)))))

(deftest evau-test
  (testing "Basic math"
    (is (= 23 (read-evau "(+ 3 (* 4 5))")))
    (is (= 42 (read-evau "(* (- (* 3 8) 3) 2)"))))
  (testing "Equality"
    (is (true?  (read-evau "(= 7 7)")))
    (is (false? (read-evau "(= 7 9)")))
    (is (true?  (read-evau "(= (- 7 4) (+ 2 1))"))))
  (testing "List"
    (is (= '(1 2 3) (read-evau "(list 1 2 3)"))))
  (testing "If"
    (is (= '1 (read-evau "(if true 1 2)")))
    (is (= '2 (read-evau "(if false 1 2)"))))
  (testing "If doesn't eval untaken branch"
    (with-redefs-fn {#'vau/list-fn #(throw (Exception. "Went down the wrong branch!"))}
      #(do (is (= '1 (read-evau "(if true 1 (list 7 8))")))
           (is (= '2 (read-evau "(if false (list 7 8) 2)"))))))
  (testing "Map"
    (is (= '(3 4 5) (read-evau "(map inc (2 3 4))")))
    (is (= '("a" "b" "c") (read-evau "(map str (a b c))"))))
  (testing "Do"
    (is (= 5 (read-evau "(do 3 4 5)")))
    (is (= 18 (read-evau "(do (def x1 17) (inc x1))"))))
  )

(deftest vau-test
  (testing "Formal args substituted but the call args aren't evaled unless something in the body explicitly evals them"
    ;; recall x and y are defed in the global-env (x=3, y=18). `str` does not eval its args.
    (is (= "xy" (read-evau "(( vau (a b) (str a b) ) x y)")))
    (is (= '("x" "y") (read-evau "(( vau (a b) (map str (a b)) ) x y)"))))
  (testing "Whereas with a fn in body that evals its args (`+`), they'll be fully evaled"
    (is (= 7 (read-evau "(( vau (a b) (+ a b) ) 3 4)")))
    (is (= 21 (read-evau "(( vau (a b) (+ a b) ) x y)")))))

;; Minor TODO: test on a fn of multiple args
(deftest applicate-test
  (read-evau ; setup
   "(def a 8)
    (def non-evaling-fn
       (vau [x] (str \"<-\" x \"->\")))")
  (testing "non-evaling-fn doesn't eval (of course)"
    (is (= "<-a->" (read-evau "(non-evaling-fn a)"))))
  (testing "Works for vau literals"
    (is (= "<-8->"
         (read-evau "((applicate (vau [x] (str \"<-\" x \"->\"))) a)"))))
  (testing "Works for symbols bound to vau expressions"
    (is (= "<-8->"
           (read-evau "((applicate non-evaling-fn) a)")))))
