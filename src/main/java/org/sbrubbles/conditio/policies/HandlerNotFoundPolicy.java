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
  HandlerNotFoundPolicy ERROR = (context) -> {
    throw new HandlerNotFoundException(context != null ? context.getCondition() : null);
  };
  HandlerNotFoundPolicy IGNORE = (context) -> null;

  /**
   * A policy which throws a {@link HandlerNotFoundException} when no handlers are found.
   *
   * @return a policy which errors out when no handler is found.
   */
  @SuppressWarnings("unchecked")
  static <T> HandlerNotFoundPolicy<T> error() {
    return ERROR;
  }

  /**
   * A policy to do nothing if no handlers are found.
   *
   * @return a policy to do nothing if no handlers are found.
   */
  @SuppressWarnings("unchecked")
  static <T> HandlerNotFoundPolicy<T> ignore() {
    return IGNORE;
  }

  /**
   * Called when no handler is found.
   * <p>
   * Implementations which error out are expected to do so with a {@link HandlerNotFoundException}.
   *
   * @param context the context of the {@code signal} invocation.
   * @return the value to be returned by {@code signal}.
   * @throws HandlerNotFoundException may be thrown if no handler is found.
   */
  T onHandlerNotFound(Handler.Context<?> context) throws HandlerNotFoundException;
}
