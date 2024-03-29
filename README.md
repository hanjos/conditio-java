[![CI](https://github.com/hanjos/conditio-java/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/hanjos/conditio-java/actions/workflows/ci.yml) [![Javadocs](https://img.shields.io/static/v1?label=Javadocs&message=0.6.0&color=informational&logo=read-the-docs)][vLatest] [![Maven package](https://img.shields.io/static/v1?label=Maven&message=0.6.0&color=orange&logo=apache-maven)](https://github.com/hanjos/conditio-java/packages/1543701)

A simple condition system for Java, without dynamic variables or reflection wizardry.

## What

Exception systems divide responsibilities in two parts: _signalling_ the exception (like `throw`), and _handling_ it (like `try/catch`), unwinding the call stack until a handler is found. The problem is, by the time the error reaches the right handler, the context that signalled the exception is mostly gone. This limits the recovery options available.

A condition system, like the one in Common Lisp, provides a more general solution by splitting responsibilities in _three_ parts: _signalling_ the condition, _handling_ it, and _restarting_ execution. The call stack is unwound only if that was the handling strategy chosen; it doesn't have to be. This enables novel recovery strategies and protocols, and can be used for things other than error handling.

[Beyond Exception Handling: Conditions and Restarts][beh-cl], chapter 19 of Peter Seibel's [Practical Common Lisp][pract-cl], informs much of the descriptions (as one can plainly see; I hope he doesn't mind :grin:), terminology and tests.

## Why

Although Common Lisp and at least [some](https://github.com/clojureman/special) [Clojure](https://github.com/pangloss/pure-conditioning) [libraries](https://github.com/bwo/conditions) use dynamic variables, Java has nothing of the sort. But it occurred to me one day that Java's [`try`-with-resources](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html) would be enough for a simple condition/restart system. So I gave it a shot :shrug:

## How

`try`-with-resources for the win: `Scope` is a resource which nests and closes scopes as execution enters and leaves `try` clauses, and provides a place to hang the signalling, handling and restarting machinery. In practice, the end result looks something like this:

```java
public void analyzeLog(String filename) throws Exception {
  try (Scope scope = Scopes.create()) {
    // establish a handler, which here picked a restart to use
    scope.handle(MalformedLogEntry.class, (signal, ops) -> ops.restart(new RetryWith("...")));

    // load file content and parse it
    InputStream in = // ...
    List<Entry> entries = parseLogFile(in);

    // ...
  }
}

public List<Entry> parseLogFile(InputStream in) throws Exception {
  try (BufferedReader br = new BufferedReader(new InputStreamReader(in));
       Scope scope = Scopes.create()) {
    List<String> lines = // ...
    List<Entry> entries = new ArrayList<>();

    // create a restart, for skipping entries
    final Restart SKIP_ENTRY = Restarts.on(SkipEntry.class, r -> SKIP_ENTRY_MARKER);

    // parse each line, and create an entry
    for (String line : lines) {
      // establishing a new restart
      Entry entry = scope.call(() -> parseLogEntry(line), SKIP_ENTRY);

      // this is how the skipping is done
      if (!SKIP_ENTRY_MARKER.equals(entry)) {
        entries.add(entry);
      }
    }

    // ...
  }
}

public Entry parseLogEntry(String text) throws Exception {
  try (Scope scope = Scopes.create()) {
    if (isWellFormed(text)) {
      return new Entry(text);
    }

    // signal a condition, and establish a restart
    return scope.raise(
      new MalformedLogEntry(text),
      Entry.class,
      Restarts.on(RetryWith.class, r -> parseLogEntry(r.getText())));
  }
}
```

## Using

Basically, Maven (or Gradle; anything compatible with Maven repos, really) and [GitHub Packages](https://docs.github.com/en/packages/guides/configuring-apache-maven-for-use-with-github-packages) for the actual [repo](https://github.com/hanjos/conditio-java/packages/1543701).

## Caveats and stuff to mull over

* There is no attempt whatsoever to make this thread-safe; to be honest, I'm not even sure what that'd look like.
* As far as I can tell, a _full_ condition system would [need continuations](https://news.ycombinator.com/item?id=20496043), or some form of nonlocal transfer of control without stack unwinding. Which in Java is... [complicated](https://stackoverflow.com/questions/1456083/continuations-in-java). So, I'll punt on fullness for the moment. Let's see how far I get...


[beh-cl]: https://gigamonkeys.com/book/beyond-exception-handling-conditions-and-restarts.html
[pract-cl]: https://gigamonkeys.com/book/
[vLatest]: https://sbrubbles.org/conditio-java/docs/0.6.0/apidocs/index.html
