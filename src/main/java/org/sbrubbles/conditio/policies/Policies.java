package org.sbrubbles.conditio.policies;

import org.sbrubbles.conditio.Condition;
import org.sbrubbles.conditio.Restart;
import org.sbrubbles.conditio.Scope;

import java.util.Objects;

/**
 * A set of policies for a specific {@link Scope#signal(Condition, Policies, Restart[]) signal}
 * invocation.
 *
 * @param <T> the type to be returned by {@code signal}.
 */
public class Policies<T> implements ReturnTypePolicy<T> {
  private final ReturnTypePolicy<T> returnTypePolicy;

  /**
   * Creates a new instance using the default policies. These are:
   *
   * <ul>
   *   <li>{@linkplain ReturnTypePolicy#ignore() ignore returns} on expected return type.</li>
   * </ul>
   *
   * @see ReturnTypePolicy#ignore()
   */
  public Policies() {
    this(ReturnTypePolicy.ignore());
  }

  /**
   * Creates a new instance using the given policies.
   *
   * @param returnTypePolicy      a policy for the expected return type.
   * @throws NullPointerException if one or both arguments is null.
   * @see #getExpectedType()
   */
  public Policies(ReturnTypePolicy<T> returnTypePolicy) {
    this.returnTypePolicy = Objects.requireNonNull(returnTypePolicy, "returnTypePolicy");
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
