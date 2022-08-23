package org.sbrubbles.conditio.policies;

import org.sbrubbles.conditio.*;

/**
 * A set of policies for a specific {@link Scope#signal(Condition, Policies, Restart[]) signal}
 * invocation.
 *
 * @param <T> the type to be returned by {@code signal}.
 */
public class Policies<T> implements HandlerNotFoundPolicy<T>, ReturnTypePolicy<T> {
  private final HandlerNotFoundPolicy<T> handlerNotFoundPolicy;
  private final ReturnTypePolicy<T> returnTypePolicy;

  /**
   * Creates a new instance using the default policies.
   */
  public Policies() {
    this(null, null);
  }

  /**
   * Creates a new instance using the given policies. A {@code null} argument represents the default policy for that
   * case.
   *
   * @param handlerNotFoundPolicy a policy for missing handlers.
   * @param returnTypePolicy      a policy for the expected return type.
   * @see #onHandlerNotFound(Handler.Context)
   * @see #getExpectedType()
   */
  public Policies(HandlerNotFoundPolicy<T> handlerNotFoundPolicy, ReturnTypePolicy<T> returnTypePolicy) {
    this.handlerNotFoundPolicy = handlerNotFoundPolicy;
    this.returnTypePolicy = returnTypePolicy;
  }

  /**
   * Applies the set policy for {@link HandlerNotFoundPolicy missing handlers}. The default policy is to throw
   * a {@link HandlerNotFoundException}.
   *
   * @return whatever the set policy returns.
   * @throws HandlerNotFoundException if the set policy decides to error out.
   */
  @Override
  public T onHandlerNotFound(Handler.Context<?> context) throws HandlerNotFoundException {
    if (handlerNotFoundPolicy != null) {
      return handlerNotFoundPolicy.onHandlerNotFound(context);
    }

    throw new HandlerNotFoundException(context != null ? context.getCondition() : null);
  }

  /**
   * Applies the set policy for {@link ReturnTypePolicy expected return type}. The default policy is to return
   * {@code null}, which indicates that any returned value will be ignored.
   *
   * @return whatever the set policy returns. {@code null} means that there is no expected return.
   */
  @Override
  public Class<T> getExpectedType() {
    if (returnTypePolicy != null) {
      return returnTypePolicy.getExpectedType();
    }

    return null;
  }
}
