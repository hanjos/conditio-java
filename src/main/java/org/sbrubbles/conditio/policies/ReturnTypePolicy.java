package org.sbrubbles.conditio.policies;

import org.sbrubbles.conditio.Condition;
import org.sbrubbles.conditio.Restart;
import org.sbrubbles.conditio.Scope;

import java.util.Objects;

/**
 * The type a {@link Scope#signal(Condition, Policies, Restart[]) signal} invocation expects to return.
 *
 * @param <T> the type to be returned by {@code signal}.
 */
@FunctionalInterface
public interface ReturnTypePolicy<T> {

  /**
   * The type {@code signal} expects to return.
   *
   * @return the type {@code signal} expects to return.
   */
  Class<T> getExpectedType();

  /**
   * An instance of this interface which uses the given type.
   *
   * @param type the expected return type.
   * @return an instance of this interface.
   */
  static <R> ReturnTypePolicy<R> expects(Class<R> type) {
    return new ReturnTypePolicyImpl<>(type);
  }
}

class ReturnTypePolicyImpl<T> implements ReturnTypePolicy<T> {
  private final Class<T> expectedReturnType;

  ReturnTypePolicyImpl(Class<T> expectedReturnType) {
    Objects.requireNonNull(expectedReturnType, "type");

    this.expectedReturnType = expectedReturnType;
  }

  @Override
  public Class<T> getExpectedType() {
    return expectedReturnType;
  }
}
