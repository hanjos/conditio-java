package org.sbrubbles.conditio;

/**
 * {@code Signal} and its implementations are <em>unchecked conditions</em>. This means if no handler is found, they
 * simply do nothing. As a consequence, {@link Scope#signal signal}ing them has no meaningful return value.
 * {@code Signal}s are better positioned as notifications, which can be
 * {@linkplain Handler.Operations#resume() acknowledged} and maybe generate some useful side effects.
 *
 * @see Condition
 * @see Handler.Operations#resume()
 */
public interface Signal extends Condition {
  /**
   * This implementation does not error out, and returns a "garbage" value, to be ignored.
   *
   * @return a garbage value, to be ignored.
   * @apiNote There's nothing meaningful for a {@code Signal} to return, so the actual value here may change in
   * further versions. Just ignore it, and you'll be fine. Hell, I might change it in future versions just to
   * break code using it :P
   */
  @Override
  default Object onHandlerNotFound(Scope scope) {
    return null;
  }
}
