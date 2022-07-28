package org.sbrubbles.conditio;

/**
 * Represents an unusual situation, which the running code doesn't know how to deal with. Conditions are meant to
 * be {@linkplain Scope#signal(Condition, Restart...) signalled}.
 * <p>
 * This interface provides some callbacks, meant to be called by {@code Scope.signal}, with default implementations.
 * This creates a protocol for {@code Scope.signal} to use, which subtypes may override. See {@link Signal} for an
 * example.
 * <p>
 * With this default implementation, any subtypes of {@code Condition} which are not also subtypes of {@code Signal}
 * are <em>checked conditions</em>. This means that a handler must be provided, or they will throw an exception.
 *
 * @see Scope#signal(Condition, Restart...)
 * @see Signal
 */
public interface Condition {
  /**
   * Called by {@link Scope#signal(Condition, Restart...)} before the handler search has begun.
   * <p>
   * This default implementation does nothing. Other subtypes may, for example, register some default restarts for
   * handlers to use.
   *
   * @param scope where the handler search will happen.
   */
  default void onStart(Scope scope) { /**/ }

  /**
   * Called by {@link Scope#signal(Condition, Restart...)} when no handler was found.
   * <p>
   * This default implementation errors out with a {@link HandlerNotFoundException}. Other subtypes may generate or
   * compute some values, which will be returned by {@code signal}.
   *
   * @param scope where the handler search started.
   * @return the value to be returned by {@code signal}.
   * @throws HandlerNotFoundException if the implementation decides to error out.
   */
  default Object onHandlerNotFound(Scope scope) throws HandlerNotFoundException {
    throw new HandlerNotFoundException(this);
  }
}
