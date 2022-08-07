package org.sbrubbles.conditio;

import org.sbrubbles.conditio.conditions.Notice;

import java.util.Objects;

/**
 * Represents an unusual situation, which the running code doesn't know how to deal with, but the code that called it
 * might. Conditions are meant to be {@linkplain Scope#signal(Condition, Restart...) signalled}, which is how
 * lower-level code communicates what happened.
 * <p>
 * This class is the superclass of all conditions in this library. It provides a callback for {@code Scope.signal},
 * which creates a protocol that {@code Condition} subtypes may override. See {@link Notice} for an example.
 * In the default implementation, an exception will be thrown if no handler is found.
 *
 * @see Scope#signal(Condition, Restart...)
 * @see Notice
 */
public class Condition<R> {
  private final Class<R> resultType;

  public Condition(Class<R> resultType) {
    Objects.requireNonNull(resultType, "resultType");

    this.resultType = resultType;
  }

  /**
   * The type of the result {@code signal}ing this condition should return.
   *
   * @return the type of the result {@code signal}ing this condition should return.
   */
  public Class<R> getResultType() {
    return resultType;
  }

  /**
   * Called when no handler was found.
   * <p>
   * This implementation errors out with a {@link HandlerNotFoundException}. Other subtypes may compute some values to
   * return.
   *
   * @param scope where the handler search started.
   * @return the value to be returned by {@code signal}.
   * @throws HandlerNotFoundException if no handler is found.
   */
  public R onHandlerNotFound(Scope scope) throws HandlerNotFoundException {
    throw new HandlerNotFoundException(this);
  }
}
