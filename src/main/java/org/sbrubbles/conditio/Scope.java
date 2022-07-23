package org.sbrubbles.conditio;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The <a href="https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html">resource</a>
 * responsible for managing the signalling machinery and the available handlers and restarts.
 * <p>
 * The main operation is {@link #signal(Condition, Restart...)}, which is called when lower-level code doesn't know
 * how to handle a certain {@linkplain Condition situation}. Basically, {@code signal} looks for something that can
 * {@linkplain #handle(Class, BiFunction) handle} the given condition. This {@linkplain Handler handler} may return a
 * result itself, or look for a recovery strategy (also known as a {@linkplain Restart restart}),
 * and {@linkplain #restart(Restart.Option) use} it to provide a result.
 * <p>
 * Restarts make sense for specific situations, and therefore are set only when the condition is {@code signal}led, or
 * when code calling a {@code signal}ling method wraps that call with {@link #call(Supplier, Restart...)}.
 * <p>
 * This class creates and manages a stack of nested {@code Scope}s, and provides ways to search for handlers and
 * restarts throughout this stack. This nesting is handled with {@link Scope#create()} and particularly
 * {@link Scope#close()}, which will unnest the scope as execution leaves it. As a
 * consequence, {@code create}ing a scope without {@code close}ing it properly will <strong>break</strong>
 * the nesting. Use it only in a {@code try}-with-resources, and you'll be fine :)
 * <p>
 * In practice, usage should look something like this:
 * <pre>
 *   try(Scope scope = Scope.create()) {
 *     // establishing a new handler, which delegates the work to a RetryWith-compatible restart
 *     scope.handle(MalformedEntry.class, (c, s) -&gt; s.restart(new RetryWith("FAIL: " + c.getText())));
 *
 *     // ...somewhere deeper in the call stack...
 *     try(Scope scope = Scope.create()) {
 *       // signals a condition, sets a restart, and waits for the result
 *       Entry entry = (Entry) scope.signal(new MalformedEntry("NOOOOOOOO"),
 *                                Restart.on(RetryWith.class, r -&gt; func(r.getValue())));
 *
 *       // carry on...
 *     }
 *   }
 * </pre>
 *
 * @see Condition
 * @see Handler
 * @see Restart
 */
public final class Scope implements AutoCloseable {
  private static Scope current = null;

  private final Scope parent;

  private final List<Handler> handlers;
  private final List<Restart> restarts;

  private Scope(Scope parent) {
    this.parent = parent;

    this.handlers = new ArrayList<>();
    this.restarts = new ArrayList<>();
  }

  // TODO what happens if the restart is defined in a scope above the handler's? Should it still work?

  /**
   * Establishes a new {@linkplain Handler handler} in this scope. It is responsible for handling conditions, returning
   * a result for {@link #signal(Condition, Restart...) signal}. The handler may compute this result by itself, or it may delegate
   * to a {@linkplain Restart restart}.
   *
   * @param conditionType the type of conditions handled.
   * @param body          the handler code, which takes as arguments a condition and the scope where {@code signal()}
   *                      was called, and returns a result.
   * @return this instance, for method chaining.
   * @throws NullPointerException if one or both parameters are {@code null}.
   * @see #signal(Condition, Restart...)
   * @see #restart(Restart.Option)
   * @see Handler
   */
  public <C extends Condition, S extends C> Scope handle(Class<S> conditionType, BiFunction<C, Scope, ?> body) {
    this.handlers.add(new HandlerImpl(conditionType, body));

    return this;
  }

  /**
   * Establishes some restarts, available to all code in or called by {@code body}.
   *
   * @param body     some code.
   * @param restarts some restarts, which will be available to all code in or called by {@code body}.
   * @return the result of calling {@code body}.
   * @throws NullPointerException if at least one parameter is {@code null}.
   * @see Restart#on(Class, Function)
   */
  public <T> T call(Supplier<T> body, Restart... restarts) {
    Objects.requireNonNull(body, "body");
    Objects.requireNonNull(restarts, "restarts");

    try (Scope scope = Scope.create()) {
      scope.establish(restarts);

      return body.get();
    }
  }

  /**
   * Signals a situation which the currently running code doesn't know how to handle. This method will
   * {@linkplain #getAllHandlers() search} for a compatible {@linkplain Handler handler} and run it, returning the
   * result.
   *
   * @param condition a condition, representing a situation which {@linkplain #handle(Class, BiFunction) higher-level
   *                  code} will decide how to handle.
   * @param restarts  some {@linkplain Restart restarts}, which will be available to the eventual handler.
   * @return the end result, as provided by the selected handler.
   * @throws NullPointerException     if no condition or a {@code null} restart array was given.
   * @throws HandlerNotFoundException if no available handler was able to handle this condition.
   * @see #handle(Class, BiFunction)
   * @see #getAllHandlers()
   * @see Restart
   * @see Restart#on(Class, Function)
   */
  public Object signal(Condition condition, Restart... restarts) throws HandlerNotFoundException {
    Objects.requireNonNull(condition, "condition");
    Objects.requireNonNull(restarts, "restarts");

    // add restarts, but only for this signal call
    try (Scope scope = Scope.create()) {
      scope.establish(restarts);

      for (Handler h : scope.getAllHandlers()) {
        if (!h.test(condition)) {
          continue;
        }

        Object result = h.apply(condition, scope);
        if (result == Handler.SKIP) {
          continue;
        }

        return result;
      }

      throw new HandlerNotFoundException(condition);
    }
  }

  /**
   * Establishes some restarts in this scope.
   *
   * @throws NullPointerException if a restart is null.
   */
  private void establish(Restart... restarts) {
    assert restarts != null;

    for (Restart r : restarts) {
      this.restarts.add(Objects.requireNonNull(r));
    }
  }

  /**
   * Invokes a previously set recovery strategy. This method will {@linkplain #getAllRestarts() search} for a
   * compatible {@linkplain Restart restart} and run it, returning the result.
   *
   * @param restartOption identifies which restart to run, and holds any data required for that restart's operation.
   * @return the result of the selected restart's execution.
   * @throws RestartNotFoundException if no restart compatible with {@code restartOption} could be found.
   * @see #getAllRestarts()
   */
  public Object restart(Restart.Option restartOption) throws RestartNotFoundException {
    for (Restart r : getAllRestarts()) {
      if (r.test(restartOption)) {
        return r.apply(restartOption);
      }
    }

    throw new RestartNotFoundException(restartOption);
  }

  /**
   * An object to iterate over all active handlers in the call stack, starting from this instance to the root scope.
   *
   * @return an iterable to get all active handlers in the call stack.
   */
  public Iterable<Handler> getAllHandlers() {
    return () -> new FullSearchIterator<Handler>(this) {
      @Override
      Iterator<Handler> getNextIteratorFrom(Scope scope) {
        return scope.handlers.iterator();
      }
    };
  }

  /**
   * An object to iterate over all active restarts in the call stack, starting from this instance to the root scope.
   *
   * @return an iterable to get all active restarts in the call stack.
   */
  public Iterable<Restart> getAllRestarts() {
    return () -> new FullSearchIterator<Restart>(this) {
      @Override
      Iterator<Restart> getNextIteratorFrom(Scope scope) {
        return scope.restarts.iterator();
      }
    };
  }

  /**
   * The active handlers in this scope.
   *
   * @return the active handlers in this scope, in an unmodifiable list.
   */
  public List<Handler> getHandlers() {
    return Collections.unmodifiableList(this.handlers);
  }

  /**
   * The active restarts in this scope.
   *
   * @return the active restarts in this scope, in an unmodifiable list.
   */
  public List<Restart> getRestarts() {
    return Collections.unmodifiableList(this.restarts);
  }

  /**
   * Creates and returns a new instance, nested in the (now former) current scope.
   *
   * @return a new instance.
   */
  public static Scope create() {
    current = new Scope(current);

    return current;
  }

  /**
   * The {@link Scope} instance wrapping this one. May be {@code null} if this is the topmost {@code Scope}.
   *
   * @return the {@link Scope} instance wrapping this one, or {@code null} if this is a root scope.
   */
  public Scope getParent() {
    return parent;
  }

  /**
   * If this is the topmost scope in its execution.
   *
   * @return {@code true} if this is the topmost scope.
   */
  public boolean isRoot() {
    return getParent() == null;
  }

  /**
   * Updates the current scope when execution leaves the {@code try} block.
   */
  @Override
  public void close() {
    current = getParent();
  }
}

