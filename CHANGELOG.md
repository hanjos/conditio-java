# 0.3

* Scope.on no longer exists. In its place there's Restart.on, Scope.call, and Scope.signal now takes restarts. The point is that restarts only apply for specific calls and signals, and therefore a scope-wide restart doesn't really make
  sense.

# 0.2

* Handlers are now ultimately responsible for returning a value to signal. A restart is merely an option for doing so, and Scope offers a restart() method for that.
* Handlers now take a condition and a scope (where signal() was called) as parameters.
* Conditions no longer hold their scope of origin, and are now merely marker interfaces.

# 0.1

Had to get started somewhere :)