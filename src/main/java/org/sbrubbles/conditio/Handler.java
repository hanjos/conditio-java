package org.sbrubbles.conditio;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Handles conditions, producing the result to be returned by {@link Scope#signal(Condition, Restart...) signal}.
 * It may compute this result by itself, or invoke one of several {@link Operations operations}.
 * <p>
 * A handler can do two things:
 * <ul>
 *   <li>check if it can handle a given condition (with {@link #test(Object) test}); and</li>
 *   <li>given a condition and the {@linkplain Operations operations available}, make a decision (with
 *       {@link #apply(Object, Object) apply}).</li>
 * </ul>
 * <p>
 * Since a handler works both as a {@linkplain Predicate predicate} and as a
 * {@linkplain BiFunction (bi)function}, this interface extends both.
 *
 * @see Condition
 * @see Restart
 * @see Operations
 */
public interface Handler extends Predicate<Condition>, BiFunction<Condition, Handler.Operations, Object> {
  /**
   * The decisions a handler can take.
   */
  interface Operations {
    /**
     * A handler may opt to signal a condition itself. This is a convenience method, to avoid the need of creating
     * another scope inside the handler and signalling from there.
     *
     * @param condition a condition.
     * @param restarts  some {@linkplain Restart restarts}, which will be available to the eventual handler.
     * @return the end result, as provided by the selected handler.
     * @throws NullPointerException     if no condition or a {@code null} restart array was given.
     * @throws HandlerNotFoundException if no available handler was able to handle this condition.
     * @see Scope#signal(Condition, Restart...)
     */
    Object signal(Condition condition, Restart... restarts) throws NullPointerException, HandlerNotFoundException;

    /**
     * Invokes a previously set recovery strategy. This method will search for a compatible
     * {@linkplain Restart restart} and run it, returning the result.
     *
     * @param restartOption identifies which restart to run, and holds any data required for that restart's operation.
     * @return the result of the selected restart's execution.
     * @throws RestartNotFoundException if no restart compatible with {@code restartOption} could be found.
     */
    Object restart(Restart.Option restartOption) throws RestartNotFoundException;

    /**
     * When a handler opts to not handle a particular condition. By calling this, other handlers, bound later in the
     * stack, will have the chance instead.
     */
    Object skip();
  }
}

/**
 * A simple implementation of {@link Handler}, which delegates its functionality to its attributes.
 */
class HandlerImpl implements Handler {
  private final Class<? extends Condition> conditionType;
  private final BiFunction<? extends Condition, Operations, ?> body;

  /**
   * Creates a new instance, ensuring statically that the given parameters are type-compatible.
   *
   * @param conditionType the type of {@link Condition} this handler expects.
   * @param body          a function which receives a condition and the available operations, and returns the result.
   * @throws NullPointerException if any of the arguments are {@code null}.
   */
  <T extends Condition, S extends T> HandlerImpl(Class<S> conditionType, BiFunction<T, Operations, ?> body) {
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
  public Object apply(Condition c, Operations s) {
    return ((BiFunction) getBody()).apply(c, s);
  }

  public Class<? extends Condition> getConditionType() {
    return conditionType;
  }

  public BiFunction<? extends Condition, Operations, ?> getBody() {
    return body;
  }
}

class HandlerOperationsImpl implements Handler.Operations {
  static final Object SKIP = new Object();

  private final Scope scope;

  public HandlerOperationsImpl(Scope scope) {
    Objects.requireNonNull(scope, "scope");

    this.scope = scope;
  }

  @Override
  public Object signal(Condition condition, Restart... restarts) {
    return getScope().signal(condition, restarts);
  }

  @Override
  public Object restart(Restart.Option restartOption) throws RestartNotFoundException {
    for (Restart r : getScope().getAllRestarts()) {
      if (r.test(restartOption)) {
        return r.apply(restartOption);
      }
    }

    throw new RestartNotFoundException(restartOption);
  }

  @Override
  public Object skip() {
    return SKIP;
  }

  public Scope getScope() {
    return scope;
  }
}
