package org.sbrubbles.conditio;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Handles conditions, producing the result to be returned by {@link Scope#signal(Condition, Restart...) signal}.
 * <p>
 * A handler can do two things:
 * <ul>
 *   <li>check if it can handle a given condition (with {@link #test(Object) test}); and</li>
 *   <li>given a condition and the {@linkplain Operations operations available}, return a {@linkplain Decision decision}
 *       object holding the result (with {@link #apply(Object, Object) apply}).</li>
 * </ul>
 * <p>
 * Decision objects are unwrapped by {@code signal}, and are expected to be non-{@code null}.
 * <p>
 * Since a handler works both as a {@linkplain Predicate predicate} and as a {@linkplain BiFunction (bi)function}, this
 * interface extends both.
 *
 * @see Condition
 * @see Restart
 * @see Operations
 * @see Decision
 */
public interface Handler<R> extends Predicate<Condition<?>>, BiFunction<Condition<R>, Handler.Operations<R>, Handler.Decision<R>> {
  /**
   * The ways a handler can handle a condition.
   */
  interface Operations<R> {
    /**
     * Invokes a previously set recovery strategy. This method will search for a compatible
     * {@linkplain Restart restart} and run it, returning the result.
     *
     * @param restartOption identifies which restart to run, and holds any data required for that restart's operation.
     * @return (a decision representing) the result of the selected restart's execution.
     * @throws RestartNotFoundException if no restart compatible with {@code restartOption} could be found.
     */
    Decision<R> restart(Restart.Option restartOption) throws RestartNotFoundException;

    /**
     * When a handler opts to not handle a particular condition. By calling this, other handlers, bound later in the
     * stack, will have the chance instead.
     *
     * @return an object representing the decision to skip.
     */
    Decision<?> skip();

    /**
     * Provides a value for {@link Scope#signal(Condition, Restart...) signal} to return directly. This may cause a
     * later {@link ClassCastException} if this value's type doesn't fit the one {@code signal} expects.
     *
     * @param object the value to be returned by {@code signal}.
     * @return (a decision holding) the given {@code object}.
     */
    Decision<R> use(R object);

    /**
     * Returns the scope backing this instance's operations.
     *
     * @return the scope backing this instance's operations.
     */
    Scope getScope();
  }

  /**
   * How a handler decided to handle a condition. Instances are produced by {@linkplain Operations operations}, and
   * consumed by {@link Scope#signal(Condition, Restart...) signal}.
   *
   * @see Operations
   * @see Scope#signal(Condition, Restart...)
   */
  class Decision<R> implements Supplier<R> {
    static final Decision<Object> SKIP = new Decision(null);

    private final R result;

    // Only classes in this package should create instances
    Decision(R result) { this.result = result; }

    @Override
    public R get() { return result; }
  }
}

class HandlerImpl<R> implements Handler<R> {
  private final Class<? extends Condition<R>> conditionType;
  private final BiFunction<? extends Condition<R>, Operations<R>, Decision<R>> body;

  /**
   * Creates a new instance, ensuring statically that the given parameters are type-compatible.
   *
   * @param conditionType the type of {@link Condition} this handler expects.
   * @param body          a function which receives a condition and the available operations, and returns the result.
   * @throws NullPointerException if any of the arguments are {@code null}.
   */
  <C extends Condition<R>, S extends C> HandlerImpl(Class<S> conditionType, BiFunction<C, Operations<R>, Decision<R>> body) {
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
  public Decision apply(Condition c, Operations ops) {
    return (Decision) ((BiFunction) getBody()).apply(c, ops);
  }

  public Class<? extends Condition<R>> getConditionType() {
    return conditionType;
  }

  public BiFunction<? extends Condition<R>, Operations<R>, Decision<R>> getBody() {
    return body;
  }
}

class HandlerOperationsImpl<R> implements Handler.Operations<R> {
  private final Scope scope;
  private final Class<R> resultType;

  public HandlerOperationsImpl(Scope scope, Class<R> resultType) {
    Objects.requireNonNull(scope, "scope");
    Objects.requireNonNull(resultType, "resultType");

    this.scope = scope;
    this.resultType = resultType;
  }

  @Override
  public Handler.Decision<R> restart(Restart.Option restartOption) throws RestartNotFoundException {
    for (Restart r : getScope().getAllRestarts()) {
      if (r.test(restartOption)) {
        return new Handler.Decision<R>(getResultType().cast(r.apply(restartOption)));
      }
    }

    throw new RestartNotFoundException(restartOption);
  }

  @Override
  public Handler.Decision<?> skip() {
    return Handler.Decision.SKIP;
  }

  @Override
  public Handler.Decision<R> use(R object) {
    return new Handler.Decision<R>(object);
  }

  @Override
  public Scope getScope() {
    return scope;
  }

  public Class<R> getResultType() {
    return resultType;
  }
}
