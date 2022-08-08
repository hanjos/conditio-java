package org.sbrubbles.conditio;

import org.sbrubbles.conditio.util.TriFunction;

import java.util.*;
import java.util.function.Supplier;

/**
 * The <a href="https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html">resource</a>
 * providing the main machinery.
 * <p>
 * The main operation is {@link #signal(Condition, Restart...)}, which is called when lower-level code doesn't know
 * how to handle a {@linkplain Condition condition}. In a nutshell, {@code signal} looks for something that can
 * {@linkplain #handle(Class, TriFunction) handle} the given condition in the call stack. This
 * {@linkplain Handler handler} then chooses {@linkplain Handler.Operations what to do}, like returning a result
 * directly, or looking for a recovery strategy (also known as a {@linkplain Restart restart}) and using it to provide
 * a result.
 * <p>
 * Scopes are resources, with controlled {@linkplain Scopes creation and closing} to ensure proper nesting. As a
 * consequence, {@link Scopes#create() create}ing a scope without {@link Scope#close() close}ing it properly will
 * <strong>break</strong> the nesting. Just use it only in a {@code try}-with-resources, and you'll be fine :)
 * <p>
 * Restarts only make sense for specific invocations. Therefore, they're set only when a condition is
 * {@code signal}led, or when code calling a {@code signal}ling method wraps that call with
 * {@link #call(Supplier, Restart...)} to provide more restarts.
 * <p>
 * In practice, usage should look something like this:
 * <pre>
 *   try(Scope scope = Scopes.create()) {
 *     // establishing a new handler, which delegates the work to a RetryWith-compatible restart
 *     scope.handle(MalformedEntry.class, (c, t, ops) -&gt; ops.restart(new RetryWith("FAIL: " + c.getText())));
 *
 *     // ...somewhere deeper in the call stack...
 *     try(Scope scope = Scopes.create()) {
 *       // signals a condition, sets a restart, and waits for the result
 *       Entry entry = scope.signal(new MalformedEntry("NOOOOOOOO"),
 *                        Entry.class,
 *                        Restart.on(RetryWith.class, r -&gt; func(r.getValue())));
 *
 *       // carry on...
 *     }
 *   }
 * </pre>
 *
 * @see Scopes
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
   * @param <R>           the type of the result {@code signal} expects.
   * @param <C>           a subtype of {@code Condition}.
   * @param <S>           a subtype of {@code C}, so that {@code body} is still compatible with {@code C} but may accept subtypes
   *                      other than {@code S}.
   * @return this instance, for method chaining.
   * @throws NullPointerException if one or both parameters are {@code null}.
   */
  <R, C extends Condition, S extends C> Scope handle(Class<S> conditionType, TriFunction<C, Class<R>, Handler.Operations<R>, Handler.Decision<R>> body);

  /**
   * Evaluates {@code body}, providing additional restarts for it. It's useful for scopes that may not know how to
   * handle a particular condition, but can provide recovery strategies for it, similarly to Common Lisp's
   * <a href="https://lispcookbook.github.io/cl-cookbook/error_handling.html#defining-restarts-restart-case">{@code restart-case}</a>.
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
   * @param <T> the type returned by {@code body}.
   * @return the result of calling {@code body}.
   * @throws NullPointerException if at least one parameter is {@code null}.
   */
  <T> T call(Supplier<T> body, Restart<?>... restarts);

  /**
   * Signals a situation which the currently running code doesn't know how to deal with. This method will
   * {@linkplain #getAllHandlers() search} for a compatible {@linkplain Handler handler} and run it, interpreting the
   * handler's {@linkplain Handler.Decision decision} (which is expected to be not {@code null}) and returning the end
   * result.
   *
   * @param condition  a condition, representing a situation which {@linkplain #handle(Class, TriFunction) higher-level
   *                   code} will decide how to handle.
   * @param resultType a class object holding the expected type of the object to be returned.
   * @param restarts   some {@linkplain Restart restarts}, which will be available to the eventual handler.
   * @param <T>        the expected type of the object to be returned.
   * @return the end result, as provided by the selected handler.
   * @throws NullPointerException     if one of the arguments, or the selected handler's decision is {@code null}.
   * @throws HandlerNotFoundException if no available handler was able to handle this condition, and the condition
   *                                  itself doesn't provide a fallback.
   * @throws ClassCastException       if the value provided by the handler isn't type-compatible with {@code S}.
   */
  <T> T signal(Condition condition, Class<T> resultType, Restart<T>... restarts) throws NullPointerException, HandlerNotFoundException;

  /**
   * Signals a situation which the currently running code doesn't know how to deal with, but has no result to return.
   * This method will {@linkplain #getAllHandlers() search} for a compatible {@linkplain Handler handler} and run it.
   *
   * @param condition a condition, representing a situation which {@linkplain #handle(Class, TriFunction) higher-level
   *                  code} will decide how to handle.
   * @param restarts  some {@linkplain Restart restarts}, which will be available to the eventual handler.
   * @throws NullPointerException     if one of the arguments, or the selected handler's decision is {@code null}.
   * @throws HandlerNotFoundException if no available handler was able to handle this condition, and the condition
   *                                  itself doesn't provide a fallback.
   * @throws ClassCastException       if the value provided by the handler isn't type-compatible with {@code S}.
   */
  void signal(Condition condition, Restart<?>... restarts) throws NullPointerException, HandlerNotFoundException;

  /**
   * An object to iterate over all reachable handlers in the call stack, starting from this instance to the root scope.
   *
   * @return an iterable to get all reachable handlers in the call stack.
   */
  Iterable<Handler<?>> getAllHandlers();

  /**
   * An object to iterate over all reachable restarts in the call stack, starting from this instance to the root scope.
   *
   * @return an iterable to get all reachable restarts in the call stack.
   */
  Iterable<Restart<?>> getAllRestarts();

  /**
   * The {@link Scope} instance wrapping this one. May be {@code null} if this is the topmost {@code Scope}.
   *
   * @return the {@link Scope} instance wrapping this one, or {@code null} if this is a root scope.
   */
  Scope getParent();

  /**
   * Updates the scope nesting when execution leaves the {@code try} block. Subtypes which override this should still
   * call this method to preserve the proper nesting.
   */
  @Override
  default void close() {
    Scopes.retire();
  }
}

final class ScopeImpl implements Scope {
  private final Scope parent;

  private final List<Handler<?>> handlers;
  private final List<Restart<?>> restarts;

  ScopeImpl(Scope parent) {
    this.parent = parent;

    this.handlers = new ArrayList<>();
    this.restarts = new ArrayList<>();
  }

  @Override
  public <R, C extends Condition, S extends C> Scope handle(Class<S> conditionType, TriFunction<C, Class<R>, Handler.Operations<R>, Handler.Decision<R>> body) {
    this.handlers.add(new HandlerImpl<>(conditionType, body));

    return this;
  }

  @Override
  public <T> T call(Supplier<T> body, Restart<?>... restarts) {
    Objects.requireNonNull(body, "body");
    Objects.requireNonNull(restarts, "restarts");

    try (ScopeImpl scope = (ScopeImpl) Scopes.create()) {
      scope.set(restarts);

      return body.get();
    }
  }

  @SuppressWarnings("rawtypes")
  @SafeVarargs
  @Override
  public final <T> T signal(Condition condition, Class<T> resultType, Restart<T>... restarts)
    throws HandlerNotFoundException, NullPointerException, ClassCastException {
    Objects.requireNonNull(condition, "condition");
    Objects.requireNonNull(restarts, "restarts");

    try (ScopeImpl scope = (ScopeImpl) Scopes.create()) {
      scope.set(restarts);

      Handler.Operations<T> ops = new HandlerOperationsImpl<>(scope, resultType);
      for (Handler<?> h : scope.getAllHandlers()) {
        if (!h.test(condition)) {
          continue;
        }

        Handler.Decision result = h.apply(condition, (Class) resultType, (Handler.Operations) ops);
        if (result == null) {
          throw new NullPointerException("Null decisions are not recognized!");
        } else if (result == Handler.Decision.SKIP) {
          continue;
        }

        return resultType.cast(result.get());
      }

      return resultType.cast(condition.onHandlerNotFound(scope));
    }
  }

  @Override
  public void signal(Condition condition, Restart<?>... restarts) throws NullPointerException, HandlerNotFoundException {
    signal(condition, Object.class, (Restart<Object>[]) restarts);
  }

  /**
   * Sets the given restarts in this scope.
   *
   * @param restarts some restarts to set.
   */
  public void set(Restart<?>... restarts) {
    for (Restart<?> r : restarts) {
      this.restarts.add(Objects.requireNonNull(r));
    }
  }

  @Override
  public Iterable<Handler<?>> getAllHandlers() {
    return () -> new FullSearchIterator<Handler<?>>(this) {
      @Override
      Iterator<Handler<?>> getNextIteratorFrom(ScopeImpl scope) {
        return scope.handlers.iterator();
      }
    };
  }

  @Override
  public Iterable<Restart<?>> getAllRestarts() {
    return () -> new FullSearchIterator<Restart<?>>(this) {
      @Override
      Iterator<Restart<?>> getNextIteratorFrom(ScopeImpl scope) {
        return scope.restarts.iterator();
      }
    };
  }

  @Override
  public Scope getParent() {
    return parent;
  }
}

/**
 * A single iterator to run through all values available in the active call stack. Which values to use is determined
 * by the implementation of {@link #getNextIteratorFrom(ScopeImpl)}.
 */
abstract class FullSearchIterator<T> implements Iterator<T> {
  private Iterator<T> currentIterator;
  private ScopeImpl currentScope;

  FullSearchIterator(ScopeImpl currentScope) {
    this.currentScope = Objects.requireNonNull(currentScope, "currentScope");
    this.currentIterator = getNextIteratorFrom(currentScope);
  }

  /**
   * Gets an iterator from {@code scope} with the next values to iterate over.
   *
   * @param scope the new scope "holding" the desired values.
   * @return the iterator "holding" the values in {@code scope}.
   */
  abstract Iterator<T> getNextIteratorFrom(ScopeImpl scope);

  @Override
  public boolean hasNext() {
    if (this.currentIterator.hasNext()) {
      return true;
    }

    do {
      if (this.currentScope.getParent() == null) {
        return false;
      }

      this.currentScope = (ScopeImpl) this.currentScope.getParent();
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