[![CI](https://github.com/hanjos/conditio-java/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/hanjos/conditio-java/actions/workflows/ci.yml) [![Javadocs](https://img.shields.io/static/v1?label=Javadocs&message=0.1.0&color=informational&logo=read-the-docs)][vLatest] [![Maven package](https://img.shields.io/static/v1?label=Maven&message=0.1.0&color=orange&logo=apache-maven)](https://github.com/hanjos/conditio-java/packages/1543701)

A simple condition system for Java, without dynamic variables or reflection wizardry.

## What?

In a nutshell, exception systems deal with exceptional situations by dividing responsibilities in two parts: _
signalling_ the exception (like `throw`), and _handling_ it (like `try/catch`). The problem with this setup is, by the
time the error reaches the right handler, the context that signalled the exception is mostly gone, because the call
stack unwinds until the handler is found. This limits the recovery options available.

A condition system, like the one in Common Lisp, provides a more general solution, by splitting the responsibilities
in _three_ parts: _signalling_ the condition, _handling_ it, and _restarting_ execution. The call stack is unwound only
if that was the handling strategy chosen; it doesn't have to be. This enables novel recovery strategies and protocols,
and can be used for things other than error handling.

Therefore, a condition is more general than an exception, representing anything that happened during execution and may
be of interest to code at different levels on the call stack.

[Beyond Exception Handling: Conditions and Restarts][beh-cl], chapter 19 of Peter Seibel's
[Practical Common Lisp][pract-cl], informs much of the descriptions (as one can plainly see; I hope he doesn't mind :),
terminology and tests.

## Why?

Although Common Lisp and at
least [some](https://github.com/clojureman/special) [Clojure](https://github.com/pangloss/pure-conditioning) [libraries](https://github.com/bwo/conditions)
use dynamic variables, Java has nothing of the sort. But it occurred to me one day that
Java's [`try-with-resources`](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html) would
be enough for a simple condition/restart system. So I gave it a shot :)

## How?

`try-with-resources` for the win: `Scope` is a resource which nests and closes scopes as execution enters and
leaves `try` clauses, and provides a place to hang the signalling, handling and restarting machinery.

## Usage?

Basically, Maven (or Gradle; anything compatible with Maven repos, really)
and [GitHub Packages](https://docs.github.com/en/packages/guides/configuring-apache-maven-for-use-with-github-packages)
for the actual [repo](https://github.com/hanjos/conditio-java/packages/1543701).

[beh-cl]: https://gigamonkeys.com/book/beyond-exception-handling-conditions-and-restarts.html

[pract-cl]: https://gigamonkeys.com/book/

[vLatest]: https://sbrubbles.org/conditio-java/docs/0.1.0/apidocs/index.html
