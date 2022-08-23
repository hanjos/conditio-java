package org.sbrubbles.conditio.policies;

import org.sbrubbles.conditio.Condition;
import org.sbrubbles.conditio.Restart;
import org.sbrubbles.conditio.Scope;

/**
 * The type a {@link Scope#signal(Condition, Policies, Restart[]) signal} invocation expects to return.
 *
 * @param <T> the type to be returned by {@code signal}.
 */
@FunctionalInterface
public interface ReturnTypePolicy<T> {
  /**
   * The type {@code signal} expects to return. May be {@code null} if {@code signal} doesn't expect to return a value.
   *
   * @return the type {@code signal} expects to return. May be {@code null} if {@code signal} doesn't expect to return
   * a value.
   */
  Class<T> getExpectedType();

  /**
   * Casts an object to the class or interface in this policy. Returns {@code null} if this policy doesn't define an
   * expected type.
   *
   * @param value the object to be cast.
   * @return the given value after casting, or {@code null} if this policy doesn't define an expected type.
   * @throws ClassCastException if the given value is not null and is not assignable to {@code T}.
   */
  default T cast(Object value) {
    if (getExpectedType() == null) {
      return null;
    }

    return getExpectedType().cast(value);
  }

  /**
   * A policy to expect values compatible with the given type {@code T}.
   *
   * @param type the expected return type.
   * @return a policy to expect values compatible with the given type {@code T}.
   */
  static <R> ReturnTypePolicy<R> expects(Class<R> type) {
    return new ReturnTypePolicyImpl<>(type);
  }

  /**
   * A policy to ignore any returned values. Therefore, there is no expected return type.
   *
   * @return a policy to ignore any returned values.
   */
  @SuppressWarnings("unchecked")
  static <R> ReturnTypePolicy<R> ignore() { return ReturnTypePolicyImpl.IGNORE; }
}

class ReturnTypePolicyImpl<T> implements ReturnTypePolicy<T> {
  static final ReturnTypePolicyImpl IGNORE = new ReturnTypePolicyImpl(null);

  private final Class<T> type;

  ReturnTypePolicyImpl(Class<T> type) {
    this.type = type;
  }

  @Override
  public Class<T> getExpectedType() {
    return type;
  }
}
