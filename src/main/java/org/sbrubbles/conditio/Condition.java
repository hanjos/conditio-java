package org.sbrubbles.conditio;

/**
 * Represents an unusual situation, which the running code doesn't know how to deal with. Conditions are meant to
 * be {@linkplain Scope#signal(Condition, Restart...) signalled}.
 * <p>
 * This interface provides a callback for {@code Scope.signal}, with a default implementation. This creates a
 * protocol which {@code Condition} subtypes may override. See {@link Signal} for an example.
 * <p>
 * In the default implementation, any subtypes of {@code Condition} which are not also subtypes of {@code Signal}
 * are <em>checked conditions</em>. This means that an exception will be thrown if no handler is found.
 *
 * @see Scope#signal(Condition, Restart...)
 * @see Signal
 */
public interface Condition {
  /**
   * Called when no handler was found.
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
