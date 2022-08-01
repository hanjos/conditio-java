package org.sbrubbles.conditio.conditions;

import org.sbrubbles.conditio.Condition;
import org.sbrubbles.conditio.Scope;

/**
 * {@code Signal} and its implementations are <em>unchecked conditions</em>, meaning that if no handler is found, they
 * don't error out.
 * <p>
 * As a consequence, {@link Scope#signal signal}ing a {@code Signal} has no meaningful return value. {@code Signal}s
 * work better as notifications, which can be safely {@linkplain org.sbrubbles.conditio.restarts.Resume resumed}, and
 * maybe generate some useful side effects.
 *
 * @see Condition
 * @see org.sbrubbles.conditio.restarts.Resume
 */
public interface Signal extends Condition {
  /**
   * This implementation does nothing.
   * <p>
   * There's nothing meaningful for a {@code Signal} to return. Just ignore the value, and you'll be fine.
   *
   * @return a garbage value, to be ignored.
   */
  @Override
  default Void onHandlerNotFound(Scope scope) {
    return null;
  }
}
