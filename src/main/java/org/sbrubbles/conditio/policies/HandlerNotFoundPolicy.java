package org.sbrubbles.conditio.policies;

import org.sbrubbles.conditio.*;

/**
 * What to do if no handler is found for a given {@linkplain Scope#signal(Condition, Policies, Restart[])
 * signalling}.
 *
 * @param <T> the type to be returned by {@code signal}.
 * @see Scope#signal(Condition, Policies, Restart[])
 */
@FunctionalInterface
public interface HandlerNotFoundPolicy<T> {
  /**
   * A policy which throws a {@link HandlerNotFoundException} when no handlers are found.
   *
   * @return a policy which errors out when no handler is found.
   */
  @SuppressWarnings("unchecked")
  static <R> HandlerNotFoundPolicy<R> error() {
    return HandlerNotFoundPolicyImpl.ERROR;
  }

  /**
   * A policy to do nothing if no handlers are found.
   *
   * @return a policy to do nothing if no handlers are found.
   */
  @SuppressWarnings("unchecked")
  static <R> HandlerNotFoundPolicy<R> ignore() { return HandlerNotFoundPolicyImpl.IGNORE; }

  /**
   * Called when no handler is found.
   * <p>
   * Implementations which error out are expected to do so with a {@link HandlerNotFoundException}.
   *
   * @param signal the signal of the {@code signal} invocation.
   * @return the value to be returned by {@code signal}.
   * @throws HandlerNotFoundException may be thrown if no handler is found.
   */
  T onHandlerNotFound(Signal<?, ?> signal) throws HandlerNotFoundException;
}

class HandlerNotFoundPolicyImpl {
  @SuppressWarnings("rawtypes")
  static final HandlerNotFoundPolicy ERROR = signal -> {
    throw new HandlerNotFoundException(signal);
  };

  @SuppressWarnings("rawtypes")
  static final HandlerNotFoundPolicy IGNORE = signal -> null;
}
