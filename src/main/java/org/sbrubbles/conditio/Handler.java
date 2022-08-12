package org.sbrubbles.conditio;

import org.sbrubbles.conditio.policies.HandlerNotFoundPolicy;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Handles conditions, producing the result to be returned by
 * {@link Scope#signal(Condition, HandlerNotFoundPolicy, Restart[]) signal}.
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
     * @param option identifies which restart to run, and holds any data required for that restart's operation.
     * @return (a decision representing) the result of the selected restart's execution.
     * @throws RestartNotFoundException if no restart compatible with {@code option} could be found.
     */
    Decision restart(Restart.Option option) throws RestartNotFoundException;

    /**
     * When a handler opts to not handle a particular condition. By calling this, other handlers, bound later in the
     * stack, will have the chance instead.
     *
     * @return an object representing the decision to skip.
     */
    Decision skip();

    /**
     * Aborts execution and unwinds the stack. There should be a {@code catch} clause at the desired
     * level to stop the unwinding.
     * <p>
     * Example:
     * <pre>
     *   try(Scope a = Scopes.create()) {
     *     // decides to give up on the whole operation
     *     a.handle(SomeCondition.class, (c, ops) -&gt; ops.abort())
     *
     *     try(Scope b = Scopes.create()) {
     *       // some code...
     *
     *       try(Scope c = Scopes.create()) {
     *         // signals something which may result in the interruption of c as a whole
     *         Object result = c.raise(new SomeCondition());
     *
     *         // (execution won't reach here)
     *       }
     *
     *       // (execution won't reach here)
     *     } catch(AbortException e) {
     *       // stops the stack unwinding here
     *     }
     *
     *     // (carries on in scope a)...
     *   }
     * </pre>
     *
     * @return nothing, since this method always throws.
     * @throws AbortException to interrupt execution and unwind the stack.
     */
    default Decision abort() throws AbortException {
      throw new AbortException();
    }

    /**
     * Returns the scope backing this instance's operations.
     *
     * @return the scope backing this instance's operations.
     */
    Scope getScope();
  }

  /**
   * How a handler decided to handle a condition. Instances are produced by {@linkplain Operations operations}, and
   * consumed by {@link Scope#signal(Condition, HandlerNotFoundPolicy, Restart[]) signal}.
   */
  class Decision implements Supplier<Object> {
    static final Decision SKIP = new Decision(null);

    private final Object result;

    // Only classes in this package should create instances
    Decision(Object result) { this.result = result; }

    /**
     * Returns the value produced.
     *
     * @return the value produced.
     */
    @Override
    public Object get() { return result; }
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
  <C extends Condition, S extends C> HandlerImpl(Class<S> conditionType, BiFunction<C, Operations, Decision> body) {
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
  public Handler.Decision restart(Restart.Option option) throws RestartNotFoundException {
    for (Restart<?> r : getScope().getAllRestarts()) {
      if (r.test(option)) {
        return new Handler.Decision(r.apply(option));
      }
    }

    throw new RestartNotFoundException(option);
  }

  @Override
  public Handler.Decision skip() {
    return Handler.Decision.SKIP;
  }

  @Override
  public Scope getScope() {
    return scope;
  }
}
