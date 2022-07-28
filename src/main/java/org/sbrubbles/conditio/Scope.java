package org.sbrubbles.conditio;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The <a href="https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html">resource</a>
 * providing the main machinery.
 * <p>
 * The main operation is {@link #signal(Condition, Restart...)}, which is called when lower-level code doesn't know
 * how to handle a {@linkplain Condition condition}. In a nutshell, {@code signal} looks for something that can
 * {@linkplain #handle(Class, BiFunction) handle} the given condition in the call stack. This
 * {@linkplain Handler handler} then chooses {@linkplain Handler.Operations what to do}, like returning a result
 * directly, or looking for a recovery strategy (also known as a {@linkplain Restart restart}) and using it to provide
 * a result.
 * <p>
 * Scopes are resources, with controlled {@linkplain Stack creation and closing} to ensure proper nesting. As a
 * consequence, {@link Stack#create() create}ing a scope without {@link Scope#close() close}ing it properly will
 * <strong>break</strong> the nesting. Just use it only in a {@code try}-with-resources, and you'll be fine :)
 * <p>
 * Restarts only make sense for specific invocations. Therefore, they're set only when a condition is
 * {@code signal}led, or when code calling a {@code signal}ling method wraps that call with
 * {@link #call(Supplier, Restart...)} to provide more restarts.
 * <p>
 * In practice, usage should look something like this:
 * <pre>
 *   try(Scope scope = Stack.create()) {
 *     // establishing a new handler, which delegates the work to a RetryWith-compatible restart
 *     scope.handle(MalformedEntry.class, (c, ops) -&gt; ops.restart(new RetryWith("FAIL: " + c.getText())));
 *
 *     // ...somewhere deeper in the call stack...
 *     try(Scope scope = Stack.create()) {
 *       // signals a condition, sets a restart, and waits for the result
 *       Entry entry = (Entry) scope.signal(new MalformedEntry("NOOOOOOOO"),
 *                                Restart.on(RetryWith.class, r -&gt; func(r.getValue())));
 *
 *       // carry on...
 *     }
 *   }
 * </pre>
 *
 * @see Stack
 * @see Condition
 * @see Handler
 * @see Restart
 */
public interface Scope extends AutoCloseable {
  /**
   * Establishes a new {@linkplain Handler handler} in this scope. It is responsible for handling conditions, returning
   * a result for {@link #signal(Condition, Restart...) signal}.
   *
   * @param conditionType the type of conditions handled.
   * @param body          the handler code.
   * @return this instance, for method chaining.
   * @throws NullPointerException if one or both parameters are {@code null}.
   * @see #signal(Condition, Restart...)
   * @see Handler
   */
  <C extends Condition, T extends C> Scope handle(Class<T> conditionType, BiFunction<C, Handler.Operations, Handler.Decision> body);

  /**
   * Establishes some restarts, available to all handlers above in the call stack. It's useful for adding recovery
   * strategies to calls that may signal conditions.
   * <p>
   * Usage example:
   * <pre>
   * final Restart SKIP_ENTRY = Restart.on(SkipEntry.class, r -&gt; SKIP_ENTRY_MARKER);
   *
   * for (String line : lines) {
   *   // parseLogEntry may signal a condition. This code doesn't handle it,
   *   // but it provides SKIP_ENTRY as one more possible restart
   *   Entry entry = scope.call(() -&gt; parseLogEntry(line), SKIP_ENTRY);
   *
   *   if (!SKIP_ENTRY_MARKER.equals(entry)) {
   *     entries.add(entry);
   *   }
   * }
   * </pre>
   *
   * @param body     some code.
   * @param restarts some restarts, which will be available to all handlers above in the call stack.
   * @return the result of calling {@code body}.
   * @throws NullPointerException if at least one parameter is {@code null}.
   * @see Restart#on(Class, Function)
   */
  <T> T call(Supplier<T> body, Restart... restarts);

  /**
   * Signals a situation which the currently running code doesn't know how to deal with. This method will
   * {@linkplain #getAllHandlers() search} for a compatible {@linkplain Handler handler} and run it, returning the
   * end result.
   *
   * @param condition a condition, representing a situation which {@linkplain #handle(Class, BiFunction) higher-level
   *                  code} will decide how to handle.
   * @param restarts  some {@linkplain Restart restarts}, which will be available to the eventual handler.
   * @return the end result, as provided by the selected handler.
   * @throws NullPointerException     if at least one argument was {@code null}.
   * @throws HandlerNotFoundException if no available handler was able to handle this condition.
   * @see #handle(Class, BiFunction)
   * @see #getAllHandlers()
   * @see Restart
   * @see Restart#on(Class, Function)
   */
  Object signal(Condition condition, Restart... restarts) throws HandlerNotFoundException;

  /**
   * An object to iterate over all active handlers in the call stack, starting from this instance to the root scope.
   *
   * @return an iterable to get all active handlers in the call stack.
   */
  Iterable<Handler> getAllHandlers();

  /**
   * An object to iterate over all active restarts in the call stack, starting from this instance to the root scope.
   *
   * @return an iterable to get all active restarts in the call stack.
   */
  Iterable<Restart> getAllRestarts();

  /**
   * The active handlers in this scope.
   *
   * @return the active handlers in this scope, in an unmodifiable list.
   */
  List<Handler> getHandlers();

  /**
   * The active restarts in this scope.
   *
   * @return the active restarts in this scope, in an unmodifiable list.
   */
  List<Restart> getRestarts();

  /**
   * The {@link Scope} instance wrapping this one. May be {@code null} if this is the topmost {@code Scope}.
   *
   * @return the {@link Scope} instance wrapping this one, or {@code null} if this is a root scope.
   */
  Scope getParent();

  /**
   * If this is the topmost scope in its execution.
   *
   * @return {@code true} if this is the topmost scope.
   */
  boolean isRoot();

  /**
   * Updates the scope nesting when execution leaves the {@code try} block. Subtypes which override this should still
   * call this method to ensure the proper nesting.
   */
  @Override
  default void close() {
    Stack.close();
  }
}

final class ScopeImpl implements Scope {
  private final Scope parent;

  private final List<Handler> handlers;
  private final List<Restart> restarts;

  ScopeImpl(Scope parent) {
    this.parent = parent;

    this.handlers = new ArrayList<>();
    this.restarts = new ArrayList<>();
  }

  @Override
  public <C extends Condition, S extends C> Scope handle(Class<S> conditionType, BiFunction<C, Handler.Operations, Handler.Decision> body) {
    this.handlers.add(new HandlerImpl(conditionType, body));

    return this;
  }

  @Override
  public <T> T call(Supplier<T> body, Restart... restarts) {
    Objects.requireNonNull(body, "body");
    Objects.requireNonNull(restarts, "restarts");

    try (Scope scope = Stack.create()) {
      ScopeWithRestarts scopeWithRestarts = new ScopeWithRestarts((ScopeImpl) scope);
      scopeWithRestarts.set(restarts);

      return body.get();
    }
  }

  @Override
  public Object signal(Condition condition, Restart... restarts) throws HandlerNotFoundException {
    Objects.requireNonNull(condition, "condition");
    Objects.requireNonNull(restarts, "restarts");

    try (Scope scope = Stack.create()) {
      ScopeWithRestarts scopeWithRestarts = new ScopeWithRestarts((ScopeImpl) scope);
      scopeWithRestarts.set(restarts); // add restarts, but only for this signal call

      condition.onStart(scopeWithRestarts);

      Handler.Operations ops = new HandlerOperationsImpl(scope);
      for (Handler h : scope.getAllHandlers()) {
        if (!h.test(condition)) {
          continue;
        }

        Handler.Decision result = h.apply(condition, ops);
        if (result == Handler.Decision.SKIP) {
          continue;
        }

        return result.get();
      }

      return condition.onHandlerNotFound(scope);
    }
  }

  void set(Restart... restarts) {
    for (Restart r : restarts) {
      this.restarts.add(Objects.requireNonNull(r));
    }
  }

  @Override
  public Iterable<Handler> getAllHandlers() {
    return () -> new FullSearchIterator<Handler>(this) {
      @Override
      Iterator<Handler> getNextIteratorFrom(Scope scope) {
        return scope.getHandlers().iterator();
      }
    };
  }

  @Override
  public Iterable<Restart> getAllRestarts() {
    return () -> new FullSearchIterator<Restart>(this) {
      @Override
      Iterator<Restart> getNextIteratorFrom(Scope scope) {
        return scope.getRestarts().iterator();
      }
    };
  }

  @Override
  public List<Handler> getHandlers() {
    return Collections.unmodifiableList(this.handlers);
  }

  @Override
  public List<Restart> getRestarts() {
    return Collections.unmodifiableList(this.restarts);
  }

  @Override
  public Scope getParent() {
    return parent;
  }

  @Override
  public boolean isRoot() {
    return getParent() == null;
  }
}

/**
 * A single iterator to run through all values available in the active call stack. Which values to use is determined
 * by the implementation of {@link #getNextIteratorFrom(Scope)}.
 */
abstract class FullSearchIterator<T> implements Iterator<T> {
  private Iterator<T> currentIterator;
  private Scope currentScope;
  private Scope endScope;

  FullSearchIterator(ScopeImpl currentScope) {
    this(currentScope, null);
  }

  FullSearchIterator(ScopeImpl currentScope, Scope upToScope) {
    this.currentScope = Objects.requireNonNull(currentScope, "currentScope");
    this.currentIterator = getNextIteratorFrom(currentScope);
    this.endScope = (upToScope == null) ? null : upToScope.getParent();
  }

  /**
   * Gets an iterator from {@code scope} with the next values to iterate over.
   *
   * @param scope the new scope "holding" the desired values.
   * @return the iterator "holding" the values in {@code scope}.
   */
  abstract Iterator<T> getNextIteratorFrom(Scope scope);

  @Override
  public boolean hasNext() {
    if (this.currentIterator.hasNext()) {
      return true;
    }

    do {
      if (this.currentScope.getParent() == null || this.currentScope.getParent() == endScope) {
        return false;
      }

      this.currentScope = this.currentScope.getParent();
      this.currentIterator = getNextIteratorFrom(this.currentScope);
    } while (!this.currentIterator.hasNext());

    return true;
  }

  @Override
  public T next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }

    return this.currentIterator.next();
  }
}

/**
 * Enables "adding" the ability to set restarts to a scope. It's a simple decorator, with some privileged access to
 * set the restarts. Not my proudest code, but it seems to work...
 */
class ScopeWithRestarts implements Scope, WithRestarts {
  private final ScopeImpl scope;

  public ScopeWithRestarts(ScopeImpl scope) {
    this.scope = scope;
  }

  @Override
  public <C extends Condition, S extends C> Scope handle(Class<S> conditionType, BiFunction<C, Handler.Operations, Handler.Decision> body) { return scope.handle(conditionType, body); }

  @Override
  public <T> T call(Supplier<T> body, Restart... restarts) { return scope.call(body, restarts); }

  @Override
  public Object signal(Condition condition, Restart... restarts) throws HandlerNotFoundException { return scope.signal(condition, restarts); }

  @Override
  public Iterable<Handler> getAllHandlers() { return scope.getAllHandlers(); }

  @Override
  public Iterable<Restart> getAllRestarts() { return scope.getAllRestarts(); }

  @Override
  public List<Handler> getHandlers() { return scope.getHandlers(); }

  @Override
  public List<Restart> getRestarts() { return scope.getRestarts(); }

  @Override
  public Scope getParent() { return scope.getParent(); }

  @Override
  public boolean isRoot() { return scope.isRoot(); }

  @Override
  public void close() { scope.close(); }

  @Override
  public void set(Restart... restarts) {
    scope.set(restarts);
  }
}