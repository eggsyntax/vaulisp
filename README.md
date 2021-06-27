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

## License

Copyright © 2021 Egg Syntax

Distributed under the Eclipse Public License version 1.0.
