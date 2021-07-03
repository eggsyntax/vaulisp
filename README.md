# vaulisp

Experimenting with a Clojure-based fexpr-centric lisp

## Installation

git clone

## Usage

### Start a vaulisp REPL:

    clj -M -m egg.vaulisp

or for a REPL which also starts an [nREPL server](https://nrepl.org/nrepl/0.8/usage/server.html#using-clojure-cli-tools):

    ./vau

Coming later: run a vaulisp file or open a repl in the context of such a file.

Basic functions gradually being added. See `egg.vaulisp/global-env` for the set
of currently-implemented functions. Note that some "functions" are actually
[operatives](https://fexpr.blogspot.com/2011/04/fexpr.html) (Paul Shutt's
terminology), which behave more like macros executed at runtime, in that their
arguments are only evaluated if that's done explicitly in the body of the
operative. As a result, there are almost no special forms, quoting is entirely
irrelevant, and language users can easily define operatives like `if` which only
evaluate one of their two possible branches.

### Inspirations:

* Paul Shutt (see his blog, eg [here](https://fexpr.blogspot.com/2011/04/fexpr.html).
Or for lots more see his
[Report on the Kernel Programming Language](ftp://ftp.cs.wpi.edu/pub/techreports/pdf/05-07.pdf)
(ftp link) or his [dissertation](https://web.wpi.edu/Pubs/ETD/Available/etd-090110-124904/)).
* Manuel Simoni's blog, The Axis of Eval, eg [this post](https://axisofeval.blogspot.com/2012/03/why-fexprs-part-n-or-lambda-only.html)
* [This blog post](http://gliese1337.blogspot.com/2012/04/schrodingers-equation-of-software.html)
from Gliese 1337.

While vaulisp isn't inspired by Paul Graham's language [Bel](http://www.paulgraham.com/bel.html),
it *is* inspired by one of his
[motivations](https://sep.yimg.com/ty/cdn/paulgraham/bellanguage.txt) for Bel:

> In 1960 John McCarthy described a new type of programming language
> called Lisp. I say "new type" because Lisp represented not just a new
> language but a new way of describing languages. He defined Lisp by
> starting with a small set of operators, akin to axioms, and then
> using them to write an interpreter for the language in itself.
> [...]
> So the development of Lisp happened in two parts (though they seem
> to have been interleaved somewhat): a formal phase, represented by
> the 1960 paper, and an implementation phase, in which this language
> was adapted and extended to run on computers. Most of the work, as
> measured by features, took place in the implementation phase. The
> Lisp in the 1960 paper, translated into Common Lisp, is only 53 lines
> of code. It does only as much as it has to in order to interpret
> expressions. Everything else got added in the implementation phase.
> [...]
> Bel is an attempt to answer the question: what happens if, instead of
> switching from the formal to the implementation phase as soon as
> possible, you try to delay that switch for as long as possible? If
> you keep using the axiomatic approach till you have something close
> to a complete programming language, what axioms do you need, and what
> does the resulting language look like?

In other words, like Bel, vaulisp is implemented as much as possible in
itself. "As much as possible" is subjective; for example, I've defined
the basic arithmetic operators in Clojure, and I haven't bothered with
cons cells, instead jumping straight to lists.

## License

Copyright © 2021 Egg Syntax

Distributed under the Eclipse Public License version 1.0.
