package org.sbrubbles.conditio;

import org.sbrubbles.conditio.conditions.Signal;

/**
 * Represents an unusual situation, which the running code doesn't know how to deal with, but the code that called it
 * might. Conditions are meant to be {@linkplain Scope#signal(Condition, Restart...) signalled}, which is how
 * lower-level code communicates what happened.
 * <p>
 * This class is the superclass of all conditions in this library. It provides a callback for {@code Scope.signal},
 * which creates a protocol that {@code Condition} subtypes may override. See {@link Signal} for an example.
 * In the default implementation, an exception will be thrown if no handler is found.
 *
 * @see Scope#signal(Condition, Restart...)
 * @see Signal
 */
public class Condition {
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
  public Object onHandlerNotFound(Scope scope) throws HandlerNotFoundException {
    throw new HandlerNotFoundException(this);
  }
}
