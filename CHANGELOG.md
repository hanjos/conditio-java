# 0.3

* Restarts only apply for specific calls and signals, so scope-wide restarts don't really make sense. Therefore, Scope.on no longer exists; in its place there's Scope.call, and Scope.signal now takes restarts too.
* Restart.on provides a default implementation for restarts without having to expose RestartImpl.

# 0.2

* Handlers are now ultimately responsible for returning a value to signal. A restart is merely an option for doing so, and Scope offers a restart() method for that.
* Handlers now take a condition and a scope (where signal() was called) as parameters.
* Conditions no longer hold their scope of origin, and are now merely marker interfaces.

# 0.1

Had to get started somewhere :)