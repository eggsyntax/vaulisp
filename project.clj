;; Note: I'm not using lein at all -- this is just here so I can run lein
;; test-refresh, since I didn't see a tools.deps equivalent. So no guarantees
;; about this file being up-to-date :)
(defproject egg.vaulisp "0.1.0-SNAPSHOT"
  :description "Experimenting with a Clojure-based fexpr-centric lisp"
  :url "https://github.com/eggsyntax/vaulisp"
  :license {:name "LGPL"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [potemkin/potemkin "0.4.5"]]
  :main ^:skip-aot egg.vaulisp
  :target-path "target/%s"
  :source-paths ["src"]
  :test-paths ["test"]
  :profiles {})
