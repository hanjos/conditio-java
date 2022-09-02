# 0.6.0

* Instead of just a condition, a handler now takes a `Signal`, which holds data about the signal (like the condition and the scope from where it came), and a `Handler.Operations`, which provides only the available operations.
* Scope operations fail on closed scopes.
* `ReturnTypePolicy` indicates the type `signal` expects to return.
* `Policies` is now an object which implements all `*Policy` interfaces, with ways to inject specific policies (via constructor).
* `HandlerNotFoundException` now takes a `Signal`, not a `Condition`.

# 0.5.0

* Several substitutions:
  * `Resume.INSTANCE` became a method in `Restarts`.
  * `Handler.Operations.use()` became a restart, `UseValue`.
  * `Condition.onHandlerNotFound` no longer exists; now `Scope.signal` takes a _policy_ as an extra parameter. So `Notice`s and `Warning`s are no more as well.
  * `Abort` became a method, `Handler.Operations.abort()`.
* `Scope.signal` is now generic, and so is `Restart`.
* `Handlers` provides handler bodies for common uses.
* `Scope.notify` and `Scope.raise` cover common `Scope.signal` uses, with better ergonomics. `Scope.signal` works better as a primitive operation, backing the others.

# 0.4.0

* Handlers now take a `Handler.Operations` instead of a scope, which delimits the operations available.
* Handlers now return a `Handler.Decision`, which `Scope.signal` knows how to unwrap.
* `Scope` is now an interface, with the `Scopes` class managing the stack.
* `Condition` is now a class which provides a callback for `signal`, paving the way for `Notice` (the `RuntimeException` to `Condition`'s `Exception`), `Warning` and other possible subtype protocols.
* There's some general use restart options, like `Resume`.
* Reorganizing the tests to improve legibility, and adding some possible usages.

# 0.3.0

* Restarts only apply for specific calls and signals, so scope-wide restarts don't really make sense. Therefore, `Scope.on` no longer exists; in its place there's `Scope.call`, and `Scope.signal` now takes restarts too.
* `Restart.on` provides a default implementation for restarts without having to expose `RestartImpl`.

# 0.2.0

* Handlers are now ultimately responsible for returning a value to signal. A restart is merely an option for doing so, and `Scope` offers a `restart()` method for that.
* Handlers now take a condition and a scope (where `signal()` was called) as parameters.
* Conditions no longer hold their scope of origin, and are now merely marker interfaces.

# 0.1.0

Had to get started somewhere :)