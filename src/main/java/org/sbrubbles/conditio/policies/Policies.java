package org.sbrubbles.conditio.policies;

import org.sbrubbles.conditio.*;

/**
 * A set of policies for a specific {@link Scope#signal(Condition, Policies, Restart[]) signal}
 * invocation.
 *
 * @param <T> the type to be returned by {@code signal}.
 */
public class Policies<T> implements HandlerNotFoundPolicy<T>, ReturnTypePolicy<T> {
  private HandlerNotFoundPolicy<T> handlerNotFoundPolicy;
  private ReturnTypePolicy<T> returnTypePolicy;

  /**
   * Sets the policy for missing handlers. {@code null} is understood to be the default policy.
   *
   * @param policy the new policy. {@code null} is understood to be the default policy.
   * @return this instance, for method chaining.
   */
  public Policies<T> set(HandlerNotFoundPolicy<T> policy) {
    handlerNotFoundPolicy = policy;

    return this;
  }

  /**
   * Sets the policy for the expected return type. {@code null} is understood to be the default policy.
   *
   * @param policy the new policy. {@code null} is understood to be the default policy.
   * @return this instance, for method chaining.
   */
  public Policies<T> set(ReturnTypePolicy<T> policy) {
    returnTypePolicy = policy;

    return this;
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
   * {@code null}, which indicates that there is no expected return.
   *
   * @return whatever the set policy returns. {@code null} means that there is no expected type.
   */
  @Override
  public Class<T> getExpectedType() {
    if (returnTypePolicy != null) {
      return returnTypePolicy.getExpectedType();
    }

    return null;
  }
}
