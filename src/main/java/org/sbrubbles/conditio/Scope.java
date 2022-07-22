package org.sbrubbles.conditio;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * The <a href="https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html">resource</a>
 * responsible for managing the signalling machinery and the available handlers and restarts.
 * <p>
 * The main operation is {@link #signal(Condition)}, which is called when lower-level code doesn't know how to handle a
 * certain {@linkplain Condition situation}, and needs a value to proceed. Basically, {@code signal} looks for
 * something that can {@linkplain #handle(Class, BiFunction) handle} the given condition. This
 * {@linkplain Handler handler} may return a result itself, or look for a previously set
 * {@linkplain #on(Class, Function) recovery strategy} (also known as a {@linkplain Restart restart}), and
 * {@linkplain #restart(Restart.Option) use} it to provide a result.
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
 *       // establishing a new restart
 *       scope.on(RetryWith.class, r -&gt; func(r.getValue()));
 *
 *       // ...somewhere deeper still...
 *       try(Scope scope = Scope.create()) {
 *         // signals a condition, and waits for the result
 *         Entry entry = (Entry) scope.signal(new MalformedEntry("NOOOOOOOO"));
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

  // TODO what happens if the restart is defined in a scope above the handler's? Should it still work?

  /**
   * Establishes a new {@linkplain Restart restart} in this scope.
   *
   * @param optionType the type of {@linkplain Restart.Option restart options} accepted.
   * @param body       the code which will take an instance of {@code optionType} and generate a result for
   *                   {@link #signal(Condition)}.
   * @return this instance, for method chaining.
   * @throws NullPointerException if one or both parameters are {@code null}.
   * @see Restart
   */
  public <T extends Restart.Option, S extends T> Scope on(Class<S> optionType, Function<T, ?> body) {
    this.restarts.add(new RestartImpl(optionType, body));

    return this;
  }

  /**
   * Establishes a new {@linkplain Handler handler} in this scope. It is responsible for handling conditions, returning
   * a result for {@link #signal(Condition) signal}. The handler may compute this result by itself, or it may delegate
   * to a {@linkplain Restart restart}.
   *
   * @param conditionType the type of conditions handled.
   * @param body          the handler code, which takes as arguments a condition and the scope where {@code signal()}
   *                      was called, and returns a result.
   * @return this instance, for method chaining.
   * @throws NullPointerException if one or both parameters are {@code null}.
   * @see #signal(Condition)
   * @see #restart(Restart.Option)
   * @see Handler
   */
  public <T extends Condition, S extends T> Scope handle(Class<S> conditionType, BiFunction<T, Scope, ?> body) {
    this.handlers.add(new HandlerImpl(conditionType, body));

    return this;
  }

  /**
   * Signals a situation which the currently running code doesn't know how to handle. This method will
   * {@linkplain #getAllHandlers() search} for a compatible {@linkplain Handler handler} and run it, returning the
   * result.
   *
   * @param condition a condition, representing a situation which {@linkplain #handle(Class, BiFunction) higher-level
   *                  code} will decide how to handle.
   * @return the end result, as provided by the selected handler.
   * @throws NullPointerException     if no condition was given.
   * @throws HandlerNotFoundException if no available handler was able to handle this condition.
   * @see #handle(Class, BiFunction)
   * @see #getAllHandlers()
   */
  public Object signal(Condition condition) throws HandlerNotFoundException {
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
   * Invokes a {@linkplain #on(Class, Function) previously set} recovery strategy. This method will
   * {@linkplain #getAllRestarts() search} for a compatible {@linkplain Restart restart} and run it, returning the
   * result.
   *
   * @param restartOption identifies which restart to run, and holds any data required for that restart's operation.
   * @return the result of the selected restart's execution.
   * @throws RestartNotFoundException if no restart compatible with {@code restartOption} could be found.
   * @see #on(Class, Function)
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

/**
 * A single iterator to run through all values available in the active call stack. Which values to use is determined
 * by the implementation of {@link #getNextIteratorFrom(Scope)}.
 */
abstract class FullSearchIterator<T> implements Iterator<T> {
  private Iterator<T> currentIterator;
  private Scope currentScope;
  private Scope endScope;

  public FullSearchIterator(Scope currentScope) {
    this(currentScope, null);
  }

  public FullSearchIterator(Scope currentScope, Scope upToScope) {
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
 * A simple implementation of {@link Handler}, which delegates its functionality to its attributes.
 */
class HandlerImpl implements Handler {
  private final Class<? extends Condition> conditionType;
  private final BiFunction<? extends Condition, Scope, ?> body;

  /**
   * Creates a new instance, ensuring statically that the given parameters are type-compatible.
   *
   * @param conditionType the type of {@link Condition} this handler expects.
   * @param body          a function which receives a condition and returns the end result.
   * @throws NullPointerException if any of the arguments are {@code null}.
   */
  public <T extends Condition, S extends T> HandlerImpl(Class<S> conditionType, BiFunction<T, Scope, ?> body) {
    Objects.requireNonNull(conditionType, "conditionType");
    Objects.requireNonNull(body, "body");

    this.conditionType = conditionType;
    this.body = body;
  }

  @Override
  public boolean test(Condition condition) {
    return getConditionType().isInstance(condition);
  }

  @SuppressWarnings("unchecked")
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

  @Override
  public String toString() {
    return "HandlerImpl{" +
      "conditionType=" + conditionType +
      ", body=" + body +
      '}';
  }
}

/**
 * A simple implementation of {@link Restart}, which delegates its functionality to its attributes.
 */
class RestartImpl implements Restart {
  private final Class<? extends Option> optionType;
  private final Function<? extends Option, ?> body;

  /**
   * Creates a new instance, ensuring statically that the given parameters are type-compatible.
   *
   * @param optionType the type of {@link Option} this restart expects.
   * @param body       a function which receives a restart option and returns the result of
   *                   {@link Scope#signal(Condition) signal}.
   * @throws NullPointerException if any of the arguments are {@code null}.
   */
  public <T extends Option, S extends T> RestartImpl(Class<S> optionType, Function<T, ?> body) {
    Objects.requireNonNull(optionType, "optionType");
    Objects.requireNonNull(body, "body");

    this.optionType = optionType;
    this.body = body;
  }

  @Override
  public boolean test(Option data) {
    return getOptionType().isInstance(data);
  }

  @SuppressWarnings("unchecked")
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

  @Override
  public String toString() {
    return "RestartImpl{" +
      "optionType=" + optionType +
      ", body=" + body +
      '}';
  }
}
