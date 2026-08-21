package org.sbrubbles.conditio;

import org.sbrubbles.conditio.restarts.Resume;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * A <a href="https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html">resource</a>
 * which hosts this library's main machinery.
 *
 * <h3>Resource management</h3>
 * Scopes can be nested, creating a <em>stack</em>; this library's machinery is able to navigate this stack and search
 * for the relevant objects. So, to ensure proper nesting, scopes have controlled {@linkplain Scopes creation}. As a
 * consequence, {@link Scopes#create(Restart[]) create}-ing a scope without {@link Scope#close() close}ing it will
 * <strong>break</strong> the nesting. Use it only in a {@code try}-with-resources, and you'll be fine :)
 * <p>
 * {@linkplain #close() Closing} a closed scope has no effect. Any other methods should fail with an
 * {@link UnsupportedOperationException}.
 *
 * <h3>Core mechanics</h3>
 * The core operation is {@link #signal(Condition, Optional, Restart[]) signal}, which is called when
 * lower-level code doesn't know how to handle a {@linkplain Condition condition}. {@code signal} looks
 * for something that can {@linkplain #handle(Class, BiFunction) handle} the given condition in the scope stack. The
 * chosen {@linkplain Handler handler} then decides {@linkplain Signal what to do}, like
 * {@linkplain Handler.Operations#abort() aborting} or looking for a recovery strategy (also known as a
 * {@linkplain Restart restart}) and using it to provide a result.
 * <p>
 * Restarts only make sense for specific invocations. Therefore, they're set only when a condition is
 * {@code signal}led, or when code calling a {@code signal}ling method wraps that call with
 * {@link #call(Supplier, Restart...) call} to provide more restarts.
 * <p>
 * In practice, {@code signal} is quite low-level, and works better as a primitive operation.
 * {@link #raise(Condition, Class, Restart[]) raise} and {@link #notify(Condition, Restart[]) notify} provide better
 * ergonomics, and should cover most use cases.
 * <p>
 * Common usage should look something like this:
 * <p>
 * <pre>
 * try(Scope scope = Scopes.create()) {
 *   // establishing a new handler, which accepts MalformedEntry conditions and
 *   // delegates the work to a RetryWith-compatible restart
 *   scope.handle(MalformedEntry.class, (s, ops) -&gt; ops.restart(new RetryWith("FAIL: " + s.getCondition().getText())));
 *
 *   // ...somewhere deeper in the call stack...
 *   try(Scope scope = Scopes.create()) {
 *     // signals a condition, sets a restart, and waits for the result
 *     Entry entry = scope.raise(new MalformedEntry("NOOOOOOOO"),
 *                      Entry.class,
 *                      Restarts.on(RetryWith.class, r -&gt; func(r.getValue())));
 *
 *     // carry on...
 *   }
 * }
 * </pre>
 */
public final class Scope implements AutoCloseable {
  private final Scope parent;
  private final List<Handler> handlers;
  private final List<Restart<?>> restarts;
  private final String tag;
  private boolean closed;

  /* The constructors are package-private; instantiation should be handled by Scopes.create */
  /**
   * Creates a {@linkplain #isRoot() root} scope with the given tag and restarts. A {@code null} tag gets stored
   * as the empty string.
   *
   * @param tag a tag to help identify this scope.
   * @param restarts the restarts made available.
   * @throws NullPointerException if {@code null} restarts are given.
   */
  Scope(String tag, Restart<?>... restarts) {
    this(null, tag, restarts);
  }

  /**
   * Creates a new scope with no tag, and the given parent and restarts. A {@code null} parent means that this
   * is a {@linkplain #isRoot() root} scope.
   *
   * @param parent   this scope's parent.
   * @param restarts the restarts made available.
   * @throws NullPointerException if {@code null} restarts are given.
   */
  Scope(Scope parent, Restart<?>... restarts) {
    this(parent, null, restarts);
  }

  /**
   * Creates a new scope with the given parent, tag and restarts. A {@code null} parent means that this
   * is a {@linkplain #isRoot() root} scope. A {@code null} tag gets stored as the empty string.
   *
   * @param parent this scope's parent.
   * @param tag a tag to help identify this scope.
   * @param restarts the restarts made available.
   * @throws NullPointerException if {@code null} restarts are given.
   */
  Scope(Scope parent, String tag, Restart<?>... restarts) {
    Objects.requireNonNull(restarts, "Null restarts aren't allowed");

    this.closed = false;
    this.parent = (parent != null) ? parent : this;
    this.handlers = new ArrayList<>();
    this.restarts = new ArrayList<>();
    this.tag = (tag != null) ? tag : "";

    // validating the restarts
    for (Restart<?> restart : restarts) {
      this.restarts.add(Objects.requireNonNull(restart, "Null restarts aren't allowed"));
    }
  }

  /**
   * Establishes a new {@linkplain Handler handler} in this scope, which matches on any conditions compatible with the
   * given type, and runs the given body to produce a result.
   *
   * @param conditionType the type of conditions handled.
   * @param body          the handler code.
   * @param <C>           a subtype of {@code Condition}.
   * @param <SubC>        a subtype of {@code C}, so that {@code body} is still compatible with {@code C} but may accept
   *                      subtypes other than {@code SubC}.
   * @return this instance, for method chaining.
   * @throws NullPointerException          if one or both parameters are null.
   * @throws UnsupportedOperationException if this method is called on a closed scope.
   * @see #handle(Handler)
   */
  public <C extends Condition, SubC extends C> Scope handle(Class<SubC> conditionType, BiFunction<Signal<C>, Handler.Operations, Handler.Decision> body)
      throws NullPointerException, UnsupportedOperationException {
    Objects.requireNonNull(conditionType, "conditionType");

    return handle(Handlers.on(s -> s != null && conditionType.isInstance(s.getCondition()), body));
  }

  /**
   * Establishes a new {@linkplain Handler handler} in this scope.
   *
   * @param handler the given handler.
   * @return this instance, for method chaining.
   * @throws NullPointerException          if the given handler is null.
   * @throws UnsupportedOperationException if this method is called on a closed scope.
   */
  public Scope handle(Handler handler) throws NullPointerException, UnsupportedOperationException {
    ensureOpen();

    this.handlers.add(Objects.requireNonNull(handler, "handler"));

    return this;
  }

  /**
   * Evaluates {@code body}, providing additional restarts for it. It's useful for scopes that may not know how to
   * handle a particular condition, but can provide recovery strategies for it, similarly to Common Lisp's
   * <a href="https://lispcookbook.github.io/cl-cookbook/error_handling.html#defining-restarts-restart-case">{@code restart-case}</a>.
   * <p>
   * Usage example:
   * <pre>
   * final Restart SKIP_ENTRY = Restarts.on(SkipEntry.class, r -&gt; SKIP_ENTRY_MARKER);
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
   * @param <T>      the type returned by {@code body}.
   * @return the result of calling {@code body}.
   * @throws NullPointerException          if at least one parameter is null.
   * @throws UnsupportedOperationException if this method is called on a closed scope.
   */
  public <T> T call(Supplier<T> body, Restart<?>... restarts)
      throws NullPointerException, UnsupportedOperationException {
    ensureOpen();

    Objects.requireNonNull(body, "body");
    Objects.requireNonNull(restarts, "restarts");

    try (Scope ignored = Scopes.create(restarts)) {
      return body.get();
    }
  }

  /**
   * {@linkplain #signal(Condition, Optional, Restart[]) Signals} a condition which may go unhandled and returns no
   * useful value. This method always provides a {@link org.sbrubbles.conditio.restarts.Resume Resume} restart.
   * <p>
   * This method is a way to provide hints or notifications to higher-level code, which can be safely resumed and
   * maybe trigger some useful side effects.
   * <p>
   * <b>Provides:</b>
   * <ul>
   *   <li>{@link Resume}, so that handlers may indicate that the signal was handled and execution may resume.</li>
   * </ul>
   *
   * @param condition a condition, which here acts as a notice that something happened.
   * @param restarts  some restarts, which, along with {@code Resume}, will be available to the eventual handler.
   * @throws NullPointerException          if one of the arguments, or the selected handler's decision is null.
   * @throws AbortException                if the eventual handler {@linkplain Handler.Operations#abort() aborts execution}.
   * @throws UnsupportedOperationException if this method is called on a closed scope.
   * @see org.sbrubbles.conditio.restarts.Resume Resume
   */
  public void notify(Condition condition, Restart<?>... restarts)
      throws NullPointerException, UnsupportedOperationException, AbortException {
    ensureOpen();

    try (Scope scope = Scopes.create("notify", Restarts.resume())) {
      scope.handle(HandlerNotFound.class, (s, ops) -> {
        // if the unhandled condition is this one, then execution may be safely resumed
        if (s.getCondition().getSignal().getCondition() == condition) {
          return ops.restart(Restarts.resume());
        }

        // otherwise, somebody else signaled this; keep looking
        return ops.skip();
      });

      scope.signal(condition, Optional.empty(), restarts);
    }
  }

  /**
   * {@linkplain #signal(Condition, Optional, Restart[]) Signals} a condition that must be handled and return a result.
   *
   * <p>
   * <b>Provides:</b>
   * <ul>
   *   <li>{@link org.sbrubbles.conditio.restarts.UseValue UseValue}, for handlers to provide the value to use.</li>
   * </ul>
   *
   * <b>Signals:</b>
   * <ul>
   *   <li>{@link HandlerNotFound}, when no handler is found for the given condition.</li>
   * </ul>
   *
   * @param <T>        the expected type of the object to be returned.
   * @param condition  a condition that must be handled.
   * @param returnType the expected type of the result.
   * @param restarts   some restarts, which, along with {@code UseValue}, will be available to the eventual handler.
   * @return the end result, as provided by the selected handler.
   * @throws NullPointerException          if one of the arguments, or the selected handler's decision is null.
   * @throws HandlerNotFoundException      if no available handler was able to handle this condition.
   * @throws ClassCastException            if the value provided by the handler isn't type-compatible with {@code T}.
   * @throws AbortException                if the eventual handler {@linkplain Handler.Operations#abort() aborts execution}.
   * @throws UnsupportedOperationException if this method is called on a closed scope.
   * @see org.sbrubbles.conditio.restarts.UseValue UseValue
   * @see HandlerNotFound
   */
  @SuppressWarnings({"unchecked", "varargs"})
  public <T> T raise(Condition condition, Class<T> returnType, Restart<? extends T>... restarts)
      throws NullPointerException, UnsupportedOperationException, HandlerNotFoundException, ClassCastException, AbortException {
    ensureOpen();

    try (Scope scope = Scopes.create("raise", Restarts.useValue())) {
      return scope.signal(condition, Optional.of(returnType), restarts);
    }
  }

  /**
   * Signals a situation which the currently running code doesn't know how to deal with. This method will
   * {@linkplain #getAllHandlers() search} for a compatible {@linkplain Handler handler} and run it, interpreting the
   * handler's {@linkplain Handler.Decision decision} (which is expected to be not null) and returning the end
   * result.
   * <p>
   * This method is a primitive operation. Common use cases can use other methods, with better ergonomics.
   * <p>
   * <b>Signals:</b>
   * <ul>
   *   <li>{@link HandlerNotFound}, when no handler is found for the given condition.</li>
   * </ul>
   *
   * @param <T>        the expected type of the object to be returned.
   * @param condition  a condition, representing a situation which {@linkplain #handle(Class, BiFunction)
   *                   higher-level code} will decide how to handle.
   * @param returnType whether there is an expected return type to return, or {@link Optional#empty()} if not.
   * @param restarts   some {@linkplain Restart restarts}, which will be available to the eventual handler.
   * @return the end result, as provided by the selected handler.
   * @throws NullPointerException          if one of the arguments, or the selected handler's decision is null.
   * @throws HandlerNotFoundException      if `HandlerNotFound` isn't handled.
   * @throws ClassCastException            if the value provided by the handler isn't type-compatible with {@code T}.
   * @throws AbortException                if the eventual handler
   *                                       {@linkplain Handler.Operations#abort() aborts execution}.
   * @throws UnsupportedOperationException if this method is called on a closed scope.
   * @see #notify(Condition, Restart[])
   * @see #raise(Condition, Class, Restart[])
   */
  @SuppressWarnings({"unchecked", "varargs", "rawtypes"})
  public <T> T signal(Condition condition, Optional<Class<T>> returnType, Restart<? extends T>... restarts)
      throws NullPointerException, UnsupportedOperationException, HandlerNotFoundException, ClassCastException, AbortException {
    ensureOpen();

    Objects.requireNonNull(condition, "condition");
    Objects.requireNonNull(returnType, "returnType");
    Objects.requireNonNull(restarts, "restarts");

    try (Scope scope = Scopes.create("signal", restarts);
         Handler.Operations ops = new Handler.Operations(scope)) {
      Signal<? extends Condition> s = new Signal<>(condition, (Optional) returnType, scope);

      for (Handler h : scope.getAllHandlers()) {
        if (!h.test(s)) {
          continue;
        }

        Handler.Decision result = Objects.requireNonNull(h.apply(s, ops), "Decisions cannot be null!");
        if (result == Handler.Decision.SKIP) {
          continue;
        }

        return returnType
            .map(type -> type.cast(result.get()))
            .orElse(null);
      }

      return scope.signal(new HandlerNotFound(s), returnType);
    }
  }

  /**
   * An object to iterate over all reachable handlers in the call stack, starting from this instance to the root scope.
   *
   * @return an iterable to get all reachable handlers in the call stack.
   * @throws UnsupportedOperationException if this method is called on a closed scope.
   */
  public Iterable<Handler> getAllHandlers() throws UnsupportedOperationException {
    ensureOpen();

    return () -> new FullSearchIterator<Handler>(this) {
      @Override
      Iterator<Handler> getNextIteratorFrom(Scope scope) {
        return scope.handlers.iterator();
      }
    };
  }

  /**
   * An object to iterate over all reachable restarts in the call stack, starting from this instance to the root scope.
   *
   * @return an iterable to get all reachable restarts in the call stack.
   * @throws UnsupportedOperationException if this method is called on a closed scope.
   */
  public Iterable<Restart<?>> getAllRestarts() throws UnsupportedOperationException {
    ensureOpen();

    return () -> new FullSearchIterator<Restart<?>>(this) {
      @Override
      Iterator<Restart<?>> getNextIteratorFrom(Scope scope) {
        return scope.restarts.iterator();
      }
    };
  }

  /**
   * The {@link Scope} instance wrapping this one, or {@code this} if it's a {@linkplain #isRoot() root scope}.
   *
   * @return the {@link Scope} instance wrapping this one, or {@code this} if this is a root scope.
   * @throws UnsupportedOperationException if this method is called on a closed scope.
   * @see #isRoot()
   */
  public Scope getParent() throws UnsupportedOperationException {
    ensureOpen();

    return parent;
  }

  /**
   * Checks if this scope is a root scope.
   *
   * @return {@code true} if this is a root scope, {@code false} otherwise.
   * @throws UnsupportedOperationException if this method is called on a closed scope.
   */
  public boolean isRoot() throws UnsupportedOperationException {
    ensureOpen();

    return parent == this;
  }

  /**
   * Returns a tag to help identify this scope. May be empty.
   *
   * @return a tag for this scope. May be empty.
   */
  public String getTag() {
    return tag;
  }

  @Override
  public String toString() {
    return String.format("Scope[%s]@%s", getTag(), Integer.toHexString(hashCode()));
  }

  /**
   * Updates the scope nesting when execution leaves the {@code try} block, and marks this scope as closed.
   */
  @Override
  public void close() {
    if (closed || isRoot()) {
      return;
    }

    Scopes.retire();
    closed = true;
  }

  /**
   * Errors out if this resource is closed.
   */
  private void ensureOpen() {
    if (closed) {
      throw new UnsupportedOperationException("Scope closed");
    }
  }
}

/**
 * A single iterator to run through all values available in the active call stack. Which values to use is determined
 * by the implementation of {@link #getNextIteratorFrom(Scope)}.
 */
abstract class FullSearchIterator<T> implements Iterator<T> {
  private Iterator<T> currentIterator;
  private Scope currentScope;

  FullSearchIterator(Scope currentScope) {
    this.currentScope = Objects.requireNonNull(currentScope, "currentScope");
    this.currentIterator = getNextIteratorFrom(currentScope);
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
      if (this.currentScope.isRoot()) {
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