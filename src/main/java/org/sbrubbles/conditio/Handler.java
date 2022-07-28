package org.sbrubbles.conditio;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Handles conditions, producing the result to be returned by {@link Scope#signal(Condition, Restart...) signal}. This
 * result is communicated, and maybe computed, by invoking one of several {@linkplain Operations operations}.
 * <p>
 * A handler can do two things:
 * <ul>
 *   <li>check if it can handle a given condition (with {@link #test(Object) test}); and</li>
 *   <li>given a condition and the {@linkplain Operations operations available}, make a decision (with
 *       {@link #apply(Object, Object) apply}).</li>
 * </ul>
 * <p>
 * Since a handler works both as a {@linkplain Predicate predicate} and as a {@linkplain BiFunction (bi)function}, this
 * interface extends both.
 *
 * @see Condition
 * @see Restart
 * @see Operations
 * @see Decision
 */
public interface Handler extends Predicate<Condition>, BiFunction<Condition, Handler.Operations, Handler.Decision> {
  /**
   * The ways a handler can handle a condition.
   */
  interface Operations {
    /**
     * Invokes a previously set recovery strategy. This method will search for a compatible
     * {@linkplain Restart restart} and run it, returning the result.
     *
     * @param restartOption identifies which restart to run, and holds any data required for that restart's operation.
     * @return (a decision representing) the result of the selected restart's execution.
     * @throws RestartNotFoundException if no restart compatible with {@code restartOption} could be found.
     */
    Decision restart(Restart.Option restartOption) throws RestartNotFoundException;

    /**
     * Signals for execution to proceed, "without" returning a value. Careful; this is meant for situations where the
     * result of {@link Scope#signal(Condition, Restart...) signal} isn't used, and the handler means only to
     * acknowledge the condition, like
     * <pre>
     *   try(Scope scope = Stack.create()) {
     *     scope.handle(Progress.class, (c, ops) -&gt; {
     *       // do something
     *       showProgressToUser(c.getValue());
     *
     *       // condition acknowledged; carry on
     *       return ops.resume();
     *     });
     *
     *     // note that result of signal() is ignored and thrown away
     *     scope.signal(new Progress(0.6));
     *
     *     // ...
     *   }
     * </pre>
     * <p>
     * There's no useful value to "return". There's also no way to tell Java to "not return" a value here, so in this
     * case {@code signal} will return a "garbage" object.
     *
     * @return an object representing the decision to continue execution.
     */
    Decision resume();

    /**
     * When a handler opts to not handle a particular condition. By calling this, other handlers, bound later in the
     * stack, will have the chance instead.
     *
     * @return an object representing the decision to skip.
     */
    Decision skip();

    /**
     * Provides a value for {@link Scope#signal(Condition, Restart...) signal} to return directly. This may cause a
     * later {@link ClassCastException} if this value's type doesn't fit the one {@code signal} expects.
     *
     * @param object the value to be returned by {@code signal}.
     * @return (a decision holding) the given {@code object}.
     */
    Decision use(Object object);

    /**
     * Returns the scope backing this instance's operations.
     *
     * @return the scope backing this instance's operations.
     */
    Scope getScope();
  }

  /**
   * Encodes how a {@linkplain Handler handler} decided to handle a condition. Instances are consumed by
   * {@link Scope#signal(Condition, Restart...) signal}.
   *
   * @see Operations
   * @see Scope#signal(Condition, Restart...)
   */
  class Decision {
    static final Decision SKIP = new Decision(null);
    static final Decision RESUME = new Decision(new Object());

    private final Object result;

    // Package-private for a reason; only classes in this package should create instances.
    Decision(Object result) { this.result = result; }

    // Package-private for a reason; only classes in this package should use this.
    Object get() { return result; }
  }
}

class HandlerImpl implements Handler {
  private final Class<? extends Condition> conditionType;
  private final BiFunction<? extends Condition, Operations, Decision> body;

  /**
   * Creates a new instance, ensuring statically that the given parameters are type-compatible.
   *
   * @param conditionType the type of {@link Condition} this handler expects.
   * @param body          a function which receives a condition and the available operations, and returns the result.
   * @throws NullPointerException if any of the arguments are {@code null}.
   */
  <T extends Condition, S extends T> HandlerImpl(Class<S> conditionType, BiFunction<T, Operations, Decision> body) {
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

  public Class<? extends Condition> getConditionType() {
    return conditionType;
  }

  public BiFunction<? extends Condition, Operations, Decision> getBody() {
    return body;
  }
}

class HandlerOperationsImpl implements Handler.Operations {
  private final Scope scope;

  public HandlerOperationsImpl(Scope scope) {
    Objects.requireNonNull(scope, "scope");

    this.scope = scope;
  }

  @Override
  public Handler.Decision restart(Restart.Option restartOption) throws RestartNotFoundException {
    for (Restart r : getScope().getAllRestarts()) {
      if (r.test(restartOption)) {
        return new Handler.Decision(r.apply(restartOption));
      }
    }

    throw new RestartNotFoundException(restartOption);
  }

  @Override
  public Handler.Decision resume() {
    return Handler.Decision.RESUME;
  }

  @Override
  public Handler.Decision skip() {
    return Handler.Decision.SKIP;
  }

  @Override
  public Handler.Decision use(Object object) {
    return new Handler.Decision(object);
  }

  @Override
  public Scope getScope() {
    return scope;
  }
}
