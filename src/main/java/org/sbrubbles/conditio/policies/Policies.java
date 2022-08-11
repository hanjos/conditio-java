package org.sbrubbles.conditio.policies;

import org.sbrubbles.conditio.HandlerNotFoundException;

/**
 * Some general use policies.
 */
public final class Policies {
  private static final HandlerNotFoundPolicy ERROR = (c, s) -> { throw new HandlerNotFoundException(c); };
  private static final HandlerNotFoundPolicy IGNORE = (c, s) -> null;

  /**
   * A policy which throws a {@link HandlerNotFoundException} when no handlers are found.
   *
   * @return a policy which errors out when no handler is found.
   */
  @SuppressWarnings("unchecked")
  public static <T> HandlerNotFoundPolicy<T> error() {
    return ERROR;
  }

  /**
   * A policy to do nothing if no handlers are found.
   *
   * @return a policy to do nothing if no handlers are found.
   */
  @SuppressWarnings("unchecked")
  public static <T> HandlerNotFoundPolicy<T> ignore() {
    return IGNORE;
  }
}
