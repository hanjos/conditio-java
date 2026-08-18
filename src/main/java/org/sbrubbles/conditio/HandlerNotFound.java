package org.sbrubbles.conditio;

import org.sbrubbles.conditio.policies.Policies;

/**
 * A condition signalled when no working handler for a given condition was found. This may happen either when no
 * available handler could {@linkplain Handler#test(Object) handle} the condition, or when the ones that could opt
 * to {@linkplain Handler.Operations#skip() skip} instead.
 * <p>
 * The root scope has a default handler for this condition, which throws {@link HandlerNotFoundException}. But by
 * signalling a condition, {@link Scope#signal(Condition, Policies, Restart[]) signal} enables programmers to adopt
 * different strategies, like {@link Scope#notify(Condition, Restart[]) notify}.
 *
 * @see Scope#signal(Condition, Policies, Restart[])
 * @see Scope#notify(Condition, Restart[])
 */
public class HandlerNotFound implements Condition {
  private final Signal<?> signal;

  /**
   * Creates a new instance.
   *
   * @param signal the signal that could not be handled.
   */
  public HandlerNotFound(Signal<?> signal) {
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

  @Override
  public String toString() {
    return "No handler found for " + signal;
  }
}
