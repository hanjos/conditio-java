package org.sbrubbles.conditio;

/**
 * Thrown when no working handler for a given condition was found. This may happen either when no available handler
 * could {@linkplain Handler#test(Object) handle} the condition, or when the ones that could opted to
 * {@linkplain Handler.Operations#skip() skip} instead.
 *
 * @see Handler
 * @see Condition
 */
public class HandlerNotFoundException extends RuntimeException {
  private final Condition condition;

  /**
   * Creates a new instance.
   *
   * @param condition the condition that could not be handled.
   */
  public HandlerNotFoundException(Condition condition) {
    super("No handler found for condition " + condition);

    this.condition = condition;
  }

  /**
   * The condition that could not be handled.
   *
   * @return the condition that could not be handled.
   */
  public Condition getCondition() {
    return condition;
  }
}
