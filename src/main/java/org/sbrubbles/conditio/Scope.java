package org.sbrubbles.conditio;

import java.util.*;
import java.util.function.Function;

/**
 * The <a href='https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html'>resource</a>
 * responsible for managing all the signalling machinery and the available {@linkplain Handler handlers} and
 * {@linkplain Restart restarts}.
 * <p></p>
 * Instantiation is handled by {@link Scope#create()}, which, along with Java's
 * try-with-resources, is used to create nested scopes and {@linkplain #close() leave them} when appropriate. As a
 * consequence, this class creates and manages a stack of nested {@code Scope}s, and provides operations to search
 * for handlers and restarts throughout this stack.
 * <p></p>
 * Also as a consequence, calling {@code Scope.create()} without {@code close}ing it properly will <b>break</b> the
 * nesting machinery. Please do not do so :(
 * <p></p>
 * Use it only in a try-with-resources, and you'll be fine :)
 * <p></p>
 * The three main operations are:
 * <ul>
 *   <li>{@link #signal(Condition)}, which signals that something happened, and (eventually) returns the result given by
 *   the restart;</li>
 *   <li>{@link #handle(Class, Function)}, which establishes a handler that handles conditions by choosing which
 *   restart to use; and</li>
 *   <li>{@link #on(Class, Function)}, which establishes a restart, which (when selected), provides the desired result
 *   for the condition.</li>
 * </ul>
 * <p>
 * Example of expected usage:
 * <pre>
 *   try(Scope scope = Scope.create()) {
 *     // register a new restart
 *     scope.on(UseValue.class, u -&gt; u.getValue());
 *
 *     // register a new handler
 *     scope.handle(MalformedEntry.class, condition -&gt; new UseValue("FAIL: " + condition.getSignal()));
 *
 *     // signal a condition, and wait for the result
 *     Object result = (Entry) scope.signal(new MalformedEntry("NOOOOOOOO"));
 *   }
 * </pre>
 *
 * @see <a href='https://gigamonkeys.com/book/beyond-exception-handling-conditions-and-restarts.html'>Beyond Exception Handling: Conditions and Restarts</a>
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

  /**
   * Establishes a new {@linkplain Restart restart} in this scope.
   *
   * @param optionType the type of {@linkplain Restart.Option restart options} accepted.
   * @param body       the code which will take an instance of {@code optionType} and generate the result to be returned in
   *                   {@link #signal(Condition)}.
   * @return this instance, for method chaining.
   * @throws NullPointerException if one or both parameters are {@code null}.
   */
  public <T extends Restart.Option, S extends T> Scope on(Class<S> optionType, Function<T, ?> body) {
    this.restarts.add(new Restart.Impl(optionType, body, this));

    return this;
  }

  /**
   * Establishes a new {@linkplain Handler handler} in this scope.
   *
   * @param conditionType the type of signals handled.
   * @param body          the code which will take a {@linkplain Condition condition} wrapping the signal and return
   *                      which restart should be used, as an instance of {@link Restart.Option}.
   * @return this instance, for method chaining.
   * @throws NullPointerException if one or both parameters are {@code null}.
   */
  public <T extends Condition, S extends T> Scope handle(Class<S> conditionType, Function<T, Restart.Option> body) {
    this.handlers.add(new Handler.Impl(conditionType, body, this));

    return this;
  }

  /**
   * Signals a situation which the currently running code doesn't know how to handle.
   * <p></p>
   * This method will:
   * <ul>
   *   <li>search for an {@linkplain Handler handler} to handle it, by deciding on a restart
   *   {@linkplain Restart.Option to use}, and</li>
   *   <li>search for the selected {@linkplain Restart restart} and run it, returning its result.</li>
   * </ul>
   *
   * @param condition a condition, representing a situation which higher-level code in the call stack will decide how
   *                  to handle.
   * @return the end result, as given by the selected restart.
   * @throws NullPointerException     if no condition was given.
   * @throws HandlerNotFoundException if no available handler was able to handle this condition.
   * @throws RestartNotFoundException if the selected restart could not be found.
   */
  public Object signal(Condition condition) throws HandlerNotFoundException, RestartNotFoundException {
    Objects.requireNonNull(condition, "condition");

    Restart.Option restartOption = selectRestartFor(condition);
    return runRestartWith(restartOption);
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
   * An unmodifiable view of the active handlers in this scope.
   *
   * @return the active handlers in this scope, in an unmodifiable list.
   */
  public List<Handler> getHandlers() {
    return Collections.unmodifiableList(this.handlers);
  }

  /**
   * An unmodifiable view of the active restarts in this scope.
   *
   * @return the active restarts in this scope, in an unmodifiable list.
   */
  public List<Restart> getRestarts() {
    return Collections.unmodifiableList(this.restarts);
  }

  /**
   * Searches for an active handler which can handle the given condition, from the inside (this scope) out
   * (the root scope). Returns the restart option selected.
   *
   * @param c a condition.
   * @return the restart option selected.
   * @throws HandlerNotFoundException if no handler was able to handle the given condition.
   */
  private Restart.Option selectRestartFor(Condition c) throws HandlerNotFoundException {
    assert c != null;

    for (Handler h : getAllHandlers()) {
      if (!h.test(c)) {
        continue;
      }

      // TODO Is null a valid restart option? It would work with runRestartWith... What would the semantics be?
      Restart.Option restartOption = h.apply(c);
      if (restartOption == Handler.SKIP) {
        continue;
      }

      return restartOption;
    }

    throw new HandlerNotFoundException(c);
  }

  /**
   * Searches for an active restart which can take the given restart option, from the inside (this scope) out
   * (the root scope). Returns the result to be used in signal().
   *
   * @param restartOption identifies which restart to run, ands holds any data required for that restart's operation.
   * @return the result to be returned by {@link #signal(Condition) signal}.
   * @throws RestartNotFoundException if no restart compatible with {@code restartOption} could be found.
   */
  private Object runRestartWith(Restart.Option restartOption) throws RestartNotFoundException {
    for (Restart r : getAllRestarts()) {
      if (r.test(restartOption)) {
        return r.apply(restartOption);
      }
    }

    throw new RestartNotFoundException(restartOption);
  }

  /**
   * Creates and returns a new {@link Scope} instance. Keeps track of the current {@code scope} in... well, scope :)
   *
   * @return a new instance of {@link Scope}.
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

/**
 * A single iterator to run through all values available in the active call stack. Which values to use is determined
 * by the implementation of {@link #getNextIteratorFrom(Scope)}.
 */
abstract class FullSearchIterator<T> implements Iterator<T> {
  private Iterator<T> currentIterator;
  private Scope currentScope;

  public FullSearchIterator(Scope currentScope) {
    this.currentScope = Objects.requireNonNull(currentScope, "currentScope");
    this.currentIterator = getNextIteratorFrom(currentScope);
  }

  /**
   * Gets an iterator from {@code scope} with the values to iterate over.
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
      if (this.currentScope.getParent() == null) {
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