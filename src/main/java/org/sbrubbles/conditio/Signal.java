package org.sbrubbles.conditio;

/**
 * {@code Signal} and its implementations are <em>unchecked conditions</em>, meaning that if no handler is found, they
 * simply do nothing instead of erroring out.
 * <p>
 * As a consequence, {@link Scope#signal signal}ing them has no meaningful return value. So {@code Signal}s work better
 * as notifications, which can be {@linkplain Handler.Operations#resume() acknowledged} and maybe generate some useful
 * side effects.
 *
 * @see Condition
 * @see Handler.Operations#resume()
 */
public interface Signal extends Condition {
  /**
   * This implementation does not error out, doing nothing otherwise.
   * <p>
   * There's nothing meaningful for a {@code Signal} to return, so the actual value here may change in further versions.
   * Just ignore it, and you'll be fine. Hell, I might change it in future versions just to break code using it :P
   *
   * @return a garbage value, to be ignored.
   */
  @Override
  default Object onHandlerNotFound(Scope scope) {
    return null;
  }
}
