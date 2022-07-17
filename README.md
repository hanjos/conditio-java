[![CI](https://github.com/hanjos/conditio-java/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/hanjos/conditio-java/actions/workflows/ci.yml) [![Javadocs](https://img.shields.io/static/v1?label=Javadocs&message=0.1.0&color=informational&logo=read-the-docs)][vLatest] [![Maven package](https://img.shields.io/static/v1?label=Maven&message=0.1.0&color=orange&logo=apache-maven)](https://github.com/hanjos/conditio-java/packages/1543701)

A simple condition system for Java, without dynamic variables or reflection wizardry.

## What

In a nutshell, exception systems deal with exceptional situations by dividing responsibilities in two parts: _signalling_ the exception (like `throw`), and _handling_ it (like `try/catch`). The problem with this setup is, by the time the error reaches the right handler, the context that signalled the exception is mostly gone, because the call stack unwinds until the handler is found. This limits the recovery options available.

A condition system, like the one in Common Lisp, provides a more general solution by splitting responsibilities in _three_ parts: _signalling_ the condition, _handling_ it, and _restarting_ execution. The call stack is unwound only if that was the handling strategy chosen; it doesn't have to be. This enables novel recovery strategies and protocols, and can be used for things other than error handling.

Therefore, a condition can represent anything that happened during execution and may be of interest to code at different levels on the call stack.

[Beyond Exception Handling: Conditions and Restarts][beh-cl], chapter 19 of Peter Seibel's [Practical Common Lisp][pract-cl], informs much of the descriptions (as one can plainly see; I hope he doesn't mind :), terminology and tests.

## Why

Although Common Lisp and at least [some](https://github.com/clojureman/special) [Clojure](https://github.com/pangloss/pure-conditioning) [libraries](https://github.com/bwo/conditions) use dynamic variables, Java has nothing of the sort. But it occurred to me one day that Java's [`try-with-resources`](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html) would be enough for a simple condition/restart system. So I gave it a shot :)

## How

`try-with-resources` for the win: `Scope` is a resource which nests and closes scopes as execution enters and leaves `try` clauses, and provides a place to hang the signalling, handling and restarting machinery. In practice, the end result looks something like this:

```java
public void analyzeLog(String filename) throws Exception{
  try(Scope scope = Scope.create()) {
    // establish a handler, which picks a restart to use
    scope.handle(MalformedLogEntry.class, condition -> new UseValue(new Entry(...)));

    // load file content and parse it
    InputStream in = // ...
    List<Entry> entries = parseLogFile(in);

    // ...
  }
}

public List<Entry> parseLogFile(InputStream in) throws Exception{
  try(BufferedReader br = new BufferedReader(new InputStreamReader(in));
      Scope scope = Scope.create()) {
    List<String> lines = // ...
    List<Entry> entries = new ArrayList<>();

    // establish a restart, which skips entries
    scope.on(SkipEntry.class, r -> SKIP_ENTRY);

    // parse each line, and create an entry
    for(String line : lines) {
      Entry entry = parseLogEntry(line);

      // this is how the skipping is done
      if(! SKIP_ENTRY.equals(entry)) {
        entries.add(entry);
      }
    }

    // ...
  }
}

public Entry parseLogEntry(String text) throws Exception{
  try(Scope scope = Scope.create()) {
    if(isWellFormed(text)) {
      return new Entry(text);
    }

    // establishing some restarts
    scope.on(UseValue.class, u -> u.getValue()) // just use the given value
         .on(RetryWith.class, r -> parseLogEntry(r.getText())); // retry with different input

    // signal a condition
    return (Entry) scope.signal(new MalformedLogEntry(scope, text));
  }
}
```

## Using

Basically, Maven (or Gradle; anything compatible with Maven repos, really) and [GitHub Packages](https://docs.github.com/en/packages/guides/configuring-apache-maven-for-use-with-github-packages) for the actual [repo](https://github.com/hanjos/conditio-java/packages/1543701).

## Caveats and stuff to mull over

* There is no attempt whatsoever to make this thread-safe; to be honest, I'm not even sure what that'd look like.
* This does no stack unwinding at all. I figured _that_ could be done by straight-up throwing an exception.
* AFAICT, in some Clojure implementations I've looked at, a handler can either explicitly call a restart or just return a value directly. Here, all a handler can do is either skip handling or call a restart; its job is merely to decide _which_ restart to call. The current approach seems simpler to implement and understand, but may be too cumbersome in practice.
* Providing some general-use restarts would be nice :)

[beh-cl]: https://gigamonkeys.com/book/beyond-exception-handling-conditions-and-restarts.html
[pract-cl]: https://gigamonkeys.com/book/
[vLatest]: https://sbrubbles.org/conditio-java/docs/0.1.0/apidocs/index.html
