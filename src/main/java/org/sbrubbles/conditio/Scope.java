package org.sbrubbles.conditio;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * The <a href='https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html'>resource</a>
 * responsible for managing the signalling machinery and the available handlers and restarts.
 * <p></p>
 * Its instantiation is handled by {@link Scope#create()}, which, along with Java's {@code try-with-resources},
 * is used to create nested scopes and {@linkplain #close() leave them} when appropriate. This class creates and manages
 * a stack of nested {@code Scope}s, and provides ways to search for handlers and restarts throughout this stack.
 * <p></p>
 * As a consequence, calling {@code Scope.create()} without {@code close}ing it properly will <strong>break</strong>
 * the nesting. Use it only in a {@code try-with-resources}, and you'll be fine :)
 * <p></p>
 * The main operations are:
 * <ul>
 *   <li>{@link #signal(Condition)}: signals that something happened, and (eventually) returns the result ;</li>
 *   <li>{@link #handle(Class, BiFunction)}: establishes a handler, which deals with conditions by either directly
 *   providing an end result for {@code signal} or choosing a restart to do so;</li>
 *   <li>{@link #on(Class, Function)}: establishes a restart, which can provide a result;
 *   and</li>
 *   <li>{@link #restart(Restart.Option)}: finds and runs a restart compatible with the given option.</li>
 * </ul>
 * <p>
 * Usage in practice should look something like this:
 * <pre>
 *   try(Scope scope = Scope.create()) {
 *     // establish a new handler
 *     scope.handle(MalformedEntry.class, (c, s) -&gt; s.restart(new RetryWith("FAIL: " + c.getText())));
 *
 *     // ...somewhere deeper in the call stack...
 *     try(Scope scope = Scope.create()) {
 *       // establish a new restart
 *       scope.on(RetryWith.class, r -&gt; func(r.getValue()));
 *
 *       // ...somewhere deeper still...
 *       try(Scope scope = Scope.create()) {
 *         // signal a condition, and wait for the result
 *         Entry entry = (Entry) scope.signal(new MalformedEntry(scope, "NOOOOOOOO"));
 *
 *         // carry on...
 *       }
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

  /**
   * Establishes a new {@linkplain Restart restart} in this scope.
   *
   * @param optionType the type of {@linkplain Restart.Option restart options} accepted.
   * @param body       the code which will take an instance of {@code optionType} and generate a result for
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
   * @param conditionType the type of conditions handled.
   * @param body          the code which will take a {@linkplain Condition condition} and its scope of origin, and
   *                      return a result for {@link #signal(Condition)}, either by itself or by calling
   *                      {@link #restart(Restart.Option)}.
   * @return this instance, for method chaining.
   * @throws NullPointerException if one or both parameters are {@code null}.
   */
  public <T extends Condition, S extends T> Scope handle(Class<S> conditionType, BiFunction<T, Scope, ?> body) {
    this.handlers.add(new HandlerImpl(conditionType, body, this));

    return this;
  }

  /**
   * Signals a situation which the currently running code doesn't know how to handle. This method will search for
   * a compatible {@linkplain Handler handler}, using this instance as the scope of origin, and return its result.
   *
   * @param condition a condition, representing a situation which higher-level code in the call stack will decide how
   *                  to handle.
   * @return the end result, as given by the handler.
   * @throws NullPointerException     if no condition was given.
   * @throws HandlerNotFoundException if no available handler was able to handle this condition.
   */
  public Object signal(Condition condition) throws HandlerNotFoundException, RestartNotFoundException {
    Objects.requireNonNull(condition, "condition");

    for (Handler h : getAllHandlers()) {
      if (!h.test(condition)) {
        continue;
      }

      Object result = h.apply(condition, this);
      if (result == Handler.SKIP) {
        continue;
      }

      return result;
    }

    throw new HandlerNotFoundException(condition);
  }

  /**
   * Searches for a restart which accepts the given restart option, from the inside (this scope) out
   * (the root scope), and runs it.
   *
   * @param restartOption identifies which restart to run, and holds any data required for that restart's operation.
   * @return the result of the found restart's execution.
   * @throws RestartNotFoundException if no restart compatible with {@code restartOption} could be found.
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
  private final BiFunction<? extends Condition, Scope, ?> body;
  private final Scope scope;

  /**
   * Creates a new instance, ensuring statically that the given parameters are type-compatible.
   *
   * @param conditionType the type of {@link Condition} this handler expects.
   * @param body          a function which receives a condition and returns the end result.
   * @param scope         the {@link Scope} instance where this handler was created.
   * @throws NullPointerException if any of the arguments are {@code null}.
   */
  public <T extends Condition, S extends T> HandlerImpl(Class<S> conditionType, BiFunction<T, Scope, ?> body, Scope scope) {
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
  public Object apply(Condition c, Scope s) {
    return ((BiFunction) getBody()).apply(c, s);
  }

  public Class<? extends Condition> getConditionType() {
    return conditionType;
  }

  public BiFunction<? extends Condition, Scope, ?> getBody() {
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
