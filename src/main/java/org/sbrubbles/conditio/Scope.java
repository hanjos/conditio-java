package org.sbrubbles.conditio;

import java.util.*;
import java.util.function.Function;

/**
 * The <a href='https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html'>resource</a>
 * responsible for managing the signalling machinery and the available handlers and restarts.
 * <p></p>
 * Its instantiation is handled by {@link Scope#create()}, which, along with Java's {@code try-with-resources},
 * is used to create nested scopes and {@linkplain #close() leave them} when appropriate. As a
 * consequence, this class creates and manages a stack of nested {@code Scope}s, and provides ways to search
 * for handlers and restarts throughout this stack.
 * <p></p>
 * Also as a consequence, calling {@code Scope.create()} without {@code close}ing it properly will <b>break</b> the
 * nesting. Use it only in a {@code try-with-resources}, and you'll be fine :)
 * <p></p>
 * The three main operations are:
 * <ul>
 *   <li>{@link #signal(Condition)}: signals that something happened, and (eventually) returns the result given by
 *   the restart;</li>
 *   <li>{@link #handle(Class, Function)}: establishes a handler, that deals with conditions by choosing which
 *   restart to use; and</li>
 *   <li>{@link #on(Class, Function)}: establishes a restart, which provides the end result for {@code signal}.</li>
 * </ul>
 * <p>
 * Example of usage:
 * <pre>
 *   try(Scope scope = Scope.create()) {
 *     // establish a new restart
 *     scope.on(UseValue.class, u -&gt; u.getValue());
 *
 *     // establish a new handler
 *     scope.handle(MalformedEntry.class, condition -&gt; new UseValue("FAIL: " + condition.getEntry()));
 *
 *     // signal a condition, and wait for the result
 *     Object result = (Entry) scope.signal(new MalformedEntry(scope, "NOOOOOOOO"));
 *
 *     // carry on...
 *   }
 * </pre>
 * <p>
 * Of course, although all three operations <em>can</em> be in the same scope, the common case is for each call, or at
 * least {@code handle}, to happen at different points in the stack.
 * <p>
 * This class is kind of like Common Lisp's {@code restart-case}; for {@code handler-case}, I guess there's already
 * {@code try/catch} ;)
 *
 * @see Condition
 * @see Handler
 * @see Restart
 * @see <a href='https://gigamonkeys.com/book/beyond-exception-handling-conditions-and-restarts.html'>Beyond Exception Handling: Conditions and Restarts</a>
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
    this.restarts.add(new RestartImpl(optionType, body, this));

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
  public <T extends Condition, S extends T> Scope handle(Class<S> conditionType, Function<T, ? extends Restart.Option> body) {
    this.handlers.add(new HandlerImpl(conditionType, body, this));

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


/**
 * A simple implementation of {@link Handler}, which delegates its functionality to its attributes.
 */
class HandlerImpl implements Handler {
  private final Class<? extends Condition> conditionType;
  private final Function<? extends Condition, ? extends Restart.Option> body;
  private final Scope scope;

  /**
   * Creates a new instance, ensuring statically that the given parameters are type-compatible.
   *
   * @param conditionType the type of {@link Condition} this handler expects.
   * @param body          a function which receives a condition and returns the restart to use.
   * @param scope         the {@link Scope} instance where this handler was created.
   * @throws NullPointerException if any of the arguments are {@code null}.
   */
  public <T extends Condition, S extends T> HandlerImpl(Class<S> conditionType, Function<T, ? extends Restart.Option> body, Scope scope) {
    Objects.requireNonNull(conditionType, "conditionType");
    Objects.requireNonNull(body, "body");
    Objects.requireNonNull(scope, "scope");

    this.conditionType = conditionType;
    this.body = body;
    this.scope = scope;
  }

  @Override
  public boolean test(Condition condition) {
    return getConditionType().isInstance(condition);
  }

  @Override
  public Restart.Option apply(Condition c) {
    return (Restart.Option) ((Function) getBody()).apply(c);
  }

  public Class<? extends Condition> getConditionType() {
    return conditionType;
  }

  public Function<? extends Condition, ? extends Restart.Option> getBody() {
    return body;
  }

  public Scope getScope() {
    return scope;
  }

  @Override
  public String toString() {
    return "HandlerImpl{" +
      "conditionType=" + conditionType +
      ", body=" + body +
      ", scope=" + scope +
      '}';
  }
}

/**
 * A simple implementation of {@link Restart}, which delegates its functionality to its attributes.
 */
class RestartImpl implements Restart {
  private final Class<? extends Option> optionType;
  private final Function<? extends Option, ?> body;
  private final Scope scope;

  /**
   * Creates a new instance, ensuring statically that the given parameters are type-compatible.
   *
   * @param optionType the type of {@link Option} this restart expects.
   * @param body       a function which receives a restart option and returns the result of
   *                   {@link Scope#signal(Condition) signal}.
   * @param scope      the {@link Scope} instance where this restart was created.
   * @throws NullPointerException if any of the arguments are {@code null}.
   */
  public <T extends Option, S extends T> RestartImpl(Class<S> optionType, Function<T, ?> body, Scope scope) {
    Objects.requireNonNull(optionType, "optionType");
    Objects.requireNonNull(body, "body");
    Objects.requireNonNull(scope, "scope");

    this.optionType = optionType;
    this.body = body;
    this.scope = scope;
  }

  @Override
  public boolean test(Option data) {
    return getOptionType().isInstance(data);
  }

  @Override
  public Object apply(Option data) {
    return ((Function) getBody()).apply(data);
  }

  public Class<? extends Option> getOptionType() {
    return optionType;
  }

  public Function<? extends Option, ?> getBody() {
    return body;
  }

  public Scope getScope() {
    return scope;
  }
}
