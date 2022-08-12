package org.sbrubbles.conditio;

import org.sbrubbles.conditio.policies.HandlerNotFoundPolicy;
import org.sbrubbles.conditio.policies.Policies;
import org.sbrubbles.conditio.restarts.Restarts;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * The <a href="https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html">resource</a>
 * providing the main machinery.
 * <p>
 * Scopes are resources, with controlled {@linkplain Scopes creation and closing} to ensure proper nesting. As a
 * consequence, {@link Scopes#create() create}ing a scope without {@link Scope#close() close}ing it properly will
 * <strong>break</strong> the nesting. Use it only in a {@code try}-with-resources, and you'll be fine :)
 * <p>
 * The core operation is {@link #signal(Condition, HandlerNotFoundPolicy, Restart[]) signal}, which is called when
 * lower-level code doesn't know how to handle a {@linkplain Condition condition}. First, {@code signal} looks
 * for something that can {@linkplain #handle(Class, BiFunction) handle} the given condition in the call stack. This
 * {@linkplain Handler handler} then chooses {@linkplain Handler.Operations what to do}, like returning a result
 * directly, or looking for a recovery strategy (also known as a {@linkplain Restart restart}) and using it to provide
 * a result.
 * <p>
 * In practice, {@code signal} is quite low-level, and works better as a primitive operation.
 * {@link #raise(Condition, Restart[]) raise} and {@link #notify(Condition, Restart[]) notify} provide better
 * ergonomics, and should cover most use cases.
 * <p>
 * Restarts only make sense for specific invocations. Therefore, they're set only when a condition is
 * {@code signal}led, or when code calling a {@code signal}ling method wraps that call with
 * {@link #call(Supplier, Restart...) call} to provide more restarts.
 * <p>
 * Usage should look something like this:
 * <pre>
 *   try(Scope scope = Scopes.create()) {
 *     // establishing a new handler, which delegates the work to a RetryWith-compatible restart
 *     scope.handle(MalformedEntry.class, (c, ops) -&gt; ops.restart(new RetryWith("FAIL: " + c.getText())));
 *
 *     // ...somewhere deeper in the call stack...
 *     try(Scope scope = Scopes.create()) {
 *       // signals a condition, sets a restart, and waits for the result
 *       Entry entry = scope.raise(new MalformedEntry("NOOOOOOOO"),
 *                        Restart.on(RetryWith.class, r -&gt; func(r.getValue())));
 *
 *       // carry on...
 *     }
 *   }
 * </pre>
 */
public interface Scope extends AutoCloseable {
  /**
   * Establishes a new {@linkplain Handler handler} in this scope. It is responsible for handling conditions, returning
   * a result for {@link #signal(Condition, HandlerNotFoundPolicy, Restart[]) signal}.
   *
   * @param conditionType the type of conditions handled.
   * @param body          the handler code.
   * @param <C>           a subtype of {@code Condition}.
   * @param <S>           a subtype of {@code C}, so that {@code body} is still compatible with {@code C} but may accept subtypes
   *                      other than {@code S}.
   * @return this instance, for method chaining.
   * @throws NullPointerException if one or both parameters are {@code null}.
   */
  <C extends Condition, S extends C> Scope handle(Class<S> conditionType, BiFunction<C, Handler.Operations, Handler.Decision> body);

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
   * <p>
   * This method is a primitive operation. Common use cases can use other methods, with better ergonomics.
   *
   * @param <T>                   the expected type of the object to be returned.
   * @param condition             a condition, representing a situation which {@linkplain #handle(Class, BiFunction)
   *                              higher-level code} will decide how to handle.
   * @param handlerNotFoundPolicy what to do if no handler is found.
   * @param restarts              some {@linkplain Restart restarts}, which will be available to the eventual handler.
   * @return the end result, as provided by the selected handler.
   * @throws NullPointerException     if one of the arguments, or the selected handler's decision is {@code null}.
   * @throws HandlerNotFoundException if the policy opts to error out.
   * @throws ClassCastException       if the value provided by the handler isn't type-compatible with {@code T}.
   * @throws AbortException           if the eventual handler {@linkplain Handler.Operations#abort() aborts execution}.
   * @see #notify(Condition, Restart[])
   * @see #raise(Condition, Restart[])
   */
  @SuppressWarnings("unchecked")
  <T> T signal(Condition condition, HandlerNotFoundPolicy<T> handlerNotFoundPolicy, Restart<T>... restarts)
    throws NullPointerException, HandlerNotFoundException, ClassCastException, AbortException;

  /**
   * {@linkplain #signal(Condition, HandlerNotFoundPolicy, Restart[]) Signals} a condition which may go unhandled and
   * returns no useful value.
   * <p>
   * This method is a way to provide hints or notifications to higher-level code, which can be safely resumed and
   * maybe trigger some useful side effects. This method always provides a
   * {@link org.sbrubbles.conditio.restarts.Resume Resume} restart.
   *
   * @param condition a condition, which here acts as a notice that something happened.
   * @param restarts  some restarts, which, along with {@code Resume}, will be available to the eventual handler.
   * @throws NullPointerException if one of the arguments, or the selected handler's decision is {@code null}.
   * @throws AbortException       if the eventual handler {@linkplain Handler.Operations#abort() aborts execution}.
   * @see #signal(Condition, HandlerNotFoundPolicy, Restart[])
   */
  @SuppressWarnings("unchecked")
  default void notify(Condition condition, Restart<?>... restarts)
    throws NullPointerException, AbortException {
    Restart[] args = new Restart[restarts.length + 1];
    args[0] = Restarts.resume();
    System.arraycopy(restarts, 0, args, 1, restarts.length);

    signal(condition, Policies.ignore(), args);
  }

  /**
   * {@linkplain #signal(Condition, HandlerNotFoundPolicy, Restart[]) Signals} a condition that must be handled and
   * return a result. This method always provides a {@link org.sbrubbles.conditio.restarts.UseValue UseValue} restart.
   *
   * @param condition a condition that must be handled.
   * @param restarts  some restarts, which, along with {@code UseValue}, will be available to the eventual handler.
   * @return the end result, as provided by the selected handler.
   * @throws NullPointerException     if one of the arguments, or the selected handler's decision is {@code null}.
   * @throws HandlerNotFoundException if no available handler was able to handle this condition.
   * @throws ClassCastException       if the value provided by the handler isn't type-compatible with {@code T}.
   * @throws AbortException           if the eventual handler {@linkplain Handler.Operations#abort() aborts execution}.
   */
  @SuppressWarnings("unchecked")
  default <T> T raise(Condition condition, Restart<T>... restarts)
    throws NullPointerException, HandlerNotFoundException, ClassCastException, AbortException {
    Restart<T>[] args = new Restart[restarts.length + 1];
    args[0] = Restarts.useValue();
    System.arraycopy(restarts, 0, args, 1, restarts.length);

    return signal(condition, Policies.error(), args);
  }

  /**
   * An object to iterate over all reachable handlers in the call stack, starting from this instance to the root scope.
   *
   * @return an iterable to get all reachable handlers in the call stack.
   */
  Iterable<Handler> getAllHandlers();

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

  private final List<Handler> handlers;
  private final List<Restart<?>> restarts;

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
  public <T> T call(Supplier<T> body, Restart<?>... restarts) {
    Objects.requireNonNull(body, "body");
    Objects.requireNonNull(restarts, "restarts");

    try (ScopeImpl scope = (ScopeImpl) Scopes.create()) {
      scope.set(restarts);

      return body.get();
    }
  }

  @SuppressWarnings("unchecked")
  @SafeVarargs
  @Override
  public final <T> T signal(Condition condition, HandlerNotFoundPolicy<T> handlerNotFoundPolicy, Restart<T>... restarts)
    throws HandlerNotFoundException, NullPointerException, ClassCastException {
    Objects.requireNonNull(condition, "condition");
    Objects.requireNonNull(restarts, "restarts");

    try (ScopeImpl scope = (ScopeImpl) Scopes.create()) {
      scope.set(restarts);

      Handler.Operations ops = new HandlerOperationsImpl(scope);
      for (Handler h : scope.getAllHandlers()) {
        if (!h.test(condition)) {
          continue;
        }

        Handler.Decision result = h.apply(condition, ops);
        if (result == null) {
          throw new NullPointerException("Null decisions are not recognized!");
        } else if (result == Handler.Decision.SKIP) {
          continue;
        }

        return (T) result.get();
      }

      return handlerNotFoundPolicy.onHandlerNotFound(condition, scope);
    }
  }

  /**
   * Sets the given restarts in this scope.
   *
   * @param restarts some restarts to set.
   * @throws NullPointerException if any of the given restarts is {@code null}.
   */
  public void set(Restart<?>... restarts) {
    for (Restart<?> r : restarts) {
      this.restarts.add(Objects.requireNonNull(r));
    }
  }

  @Override
  public Iterable<Handler> getAllHandlers() {
    return () -> new FullSearchIterator<Handler>(this) {
      @Override
      Iterator<Handler> getNextIteratorFrom(ScopeImpl scope) {
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