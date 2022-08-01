package org.sbrubbles.conditio.conditions;

import org.sbrubbles.conditio.Condition;
import org.sbrubbles.conditio.Scope;

/**
 * This class and its subclasses are <em>unchecked conditions</em>, meaning that if no handler is found, they
 * don't error out.
 * <p>
 * Signals are meant for conditions that may go unhandled entirely. They work better as hints or notifications to
 * higher-level code, which can be safely {@linkplain org.sbrubbles.conditio.restarts.Resume resumed}, and maybe
 * generate some useful side effects.
 * <p>
 * As a consequence, {@link Scope#signal signal}ing a {@code Signal} has no meaningful return value.
 *
 * @see Condition
 * @see org.sbrubbles.conditio.restarts.Resume
 */
public class Signal extends Condition {
  /**
   * This implementation does nothing.
   * <p>
   * There's nothing meaningful for a {@code Signal} to return. Just ignore the value, and you'll be fine.
   *
   * @return a garbage value, to be ignored.
   */
  @Override
  public Void onHandlerNotFound(Scope scope) {
    return null;
  }
}
