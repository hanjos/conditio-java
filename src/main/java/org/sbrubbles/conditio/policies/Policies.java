package org.sbrubbles.conditio.policies;

import org.sbrubbles.conditio.*;

import java.util.Objects;

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
   * Creates a new instance using the default policies. These are:
   *
   * <ul>
   *   <li>{@linkplain HandlerNotFoundPolicy#error() error out} on missing handlers; and</li>
   *   <li>{@linkplain ReturnTypePolicy#ignore() ignore returns} on expected return type.</li>
   * </ul>
   *
   * @see HandlerNotFoundPolicy#error()
   * @see ReturnTypePolicy#ignore()
   */
  public Policies() {
    this(HandlerNotFoundPolicy.error(), ReturnTypePolicy.ignore());
  }

  /**
   * Creates a new instance using the given policies.
   *
   * @param handlerNotFoundPolicy a policy for missing handlers.
   * @param returnTypePolicy      a policy for the expected return type.
   * @throws NullPointerException if one or both arguments is null.
   * @see #onHandlerNotFound(Signal)
   * @see #getExpectedType()
   */
  public Policies(HandlerNotFoundPolicy<T> handlerNotFoundPolicy, ReturnTypePolicy<T> returnTypePolicy) {
    this.handlerNotFoundPolicy = Objects.requireNonNull(handlerNotFoundPolicy, "handlerNotFoundPolicy");
    this.returnTypePolicy = Objects.requireNonNull(returnTypePolicy, "returnTypePolicy");
  }

  /**
   * Applies the set policy for {@linkplain HandlerNotFoundPolicy missing handlers}.
   *
   * @return whatever the set policy returns.
   * @throws HandlerNotFoundException if the set policy decides to error out.
   */
  @Override
  public T onHandlerNotFound(Signal<?> signal) throws HandlerNotFoundException {
    return handlerNotFoundPolicy.onHandlerNotFound(signal);
  }

  /**
   * Applies the set policy for {@linkplain ReturnTypePolicy expected return type}.
   *
   * @return whatever the set policy returns. A null means that there is no expected return.
   */
  @Override
  public Class<T> getExpectedType() {
    return returnTypePolicy.getExpectedType();
  }
}
