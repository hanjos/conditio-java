package org.sbrubbles.conditio;

import org.sbrubbles.conditio.policies.HandlerNotFoundPolicy;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Handles conditions, producing the result to be returned by
 * {@link Scope#signal(Condition, HandlerNotFoundPolicy, Restart[]) signal}.
 * <p>
 * A handler can do two things:
 * <ul>
 *   <li>check if it can handle a given condition (with {@link #test(Object) test}); and</li>
 *   <li>given the {@linkplain Context context}, return a {@linkplain Decision decision} object holding the result
 *   (with {@link #apply(Object) apply}).</li>
 * </ul>
 * <p>
 * Decision objects are unwrapped by {@code signal}, and are expected to be non-{@code null}.
 * <p>
 * Since a handler works both as a {@linkplain Predicate predicate} and as a {@linkplain Function function}, this
 * interface extends both.
 */
public interface Handler extends Predicate<Condition>, Function<Handler.Context<? extends Condition>, Handler.Decision> {
  /**
   * Holds information about the signalling context, and provides ways for a handler to handle a condition.
   *
   * @param <C> the condition type this context holds.
   */
  interface Context<C extends Condition> {
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
     *     a.handle(SomeCondition.class, ctx -&gt; ctx.abort())
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
     * The condition signaled.
     *
     * @return the condition signaled.
     */
    C getCondition();

    /**
     * The scope where the signal was emitted.
     *
     * @return the scope where the signal was emitted.
     */
    Scope getScope();
  }

  /**
   * How a handler decided to handle a condition. Instances are produced by {@linkplain Context operations}, and
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
  private final Function<Context<? extends Condition>, Decision> body;

  /**
   * Creates a new instance, ensuring statically that the given parameters are type-compatible.
   *
   * @param conditionType the type of {@link Condition} this handler expects.
   * @param body          a function which receives a condition and the available operations, and returns the result.
   * @param <C>           a subtype of {@code Condition}.
   * @param <S>           a subtype of {@code C}, so that {@code body} is still compatible with {@code C} but may
   *                      accept subtypes other than {@code S}.
   * @throws NullPointerException if any of the arguments are {@code null}.
   */
  <C extends Condition, S extends C> HandlerImpl(Class<S> conditionType, Function<Context<C>, Decision> body) {
    Objects.requireNonNull(conditionType, "conditionType");
    Objects.requireNonNull(body, "body");

    this.conditionType = conditionType;
    this.body = (Function) body;
  }

  @Override
  public boolean test(Condition condition) {
    return getConditionType().isInstance(condition);
  }

  @Override
  public Decision apply(Context ctx) {
    return getBody().apply(ctx);
  }

  public Class<? extends Condition> getConditionType() {
    return conditionType;
  }

  public Function<Context<? extends Condition>, Decision> getBody() {
    return body;
  }
}

class HandlerContextImpl implements Handler.Context {
  private final Condition condition;
  private final Scope scope;

  public HandlerContextImpl(Condition condition, Scope scope) {
    Objects.requireNonNull(condition, "condition");
    Objects.requireNonNull(scope, "scope");

    this.condition = condition;
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
  public Condition getCondition() {
    return condition;
  }

  @Override
  public Scope getScope() {
    return scope;
  }
}
