package org.sbrubbles.conditio;

/**
 * Thrown when no working handler for a given condition was found. This may happen either when no available handler
 * could {@linkplain Handler#test(Object) handle} the condition, or when the ones that could opted to
 * {@linkplain Handler.Operations#skip() skip} instead.
 *
 * @see Handler
 * @see Signal
 */
public class HandlerNotFoundException extends RuntimeException {
  private final Signal<?> signal;

  /**
   * Creates a new instance.
   *
   * @param signal the signal that could not be handled.
   */
  public HandlerNotFoundException(Signal<?> signal) {
    super("No handler found for " + signal);

    this.signal = signal;
  }

  /**
   * The signal that could not be handled.
   *
   * @return the signal that could not be handled.
   */
  public Signal<?> getSignal() {
    return signal;
  }
}
