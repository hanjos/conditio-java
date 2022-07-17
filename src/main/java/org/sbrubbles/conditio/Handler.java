package org.sbrubbles.conditio;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Handles conditions by selecting which restart to use. This selection is done by returning a restart option, which is
 * then used to find the appropriate restart, and can hold useful data for the recovery code.
 * <p>
 * A handler doesn't necessarily <em>need</em> to handle a condition; it can decline to do so by returning
 * {@link Handler#SKIP}. The signalling machinery will then search for another handler, bound later in the stack.
 * <p>
 * A handler can do two things: check if it can handle a given signal (with {@link #test(Object)}), and
 * analyze a given condition, returning which restart should be used (with {@link #apply(Object)}). Since it works both
 * as a {@linkplain Predicate<Object> predicate} and as a {@linkplain Function function}, this interface
 * extends both.
 *
 * @see Condition
 * @see Restart.Option
 * @see Restart
 * @see Scope
 */
public interface Handler extends Predicate<Condition>, Function<Condition, Restart.Option> {
  /**
   * Returned when a handler, for whatever reason, opts to not handle a particular condition. By returning this, other
   * handlers, bound later in the stack, will have the chance instead.
   */
  Restart.Option SKIP = new Restart.Option() {
    @Override
    public String toString() {
      return "Handler.SKIP";
    }
  };
}
