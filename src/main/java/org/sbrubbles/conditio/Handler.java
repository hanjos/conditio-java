package org.sbrubbles.conditio;

import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Handles conditions, producing the end result to be returned by {@link Scope#signal(Condition) signal}. It may
 * compute this result by itself, or by {@linkplain Scope#restart(Restart.Option) delegating the work} to an available
 * restart.
 * <p>
 * A handler doesn't necessarily <em>need</em> to handle a condition; it can decline to do so by returning
 * {@link Handler#SKIP}. The signalling machinery will then search for another handler, bound later in the stack.
 * <p>
 * A handler can do two things: check if it can handle a given signal (with {@link #test(Object)}), and
 * analyze a given condition in its scope of origin, returning the end result (with {@link #apply(Object, Object)}).
 * Since it works both as a {@linkplain Predicate<Object> predicate} and as a {@linkplain BiFunction (bi)function},
 * this interface extends both.
 *
 * @see Condition
 * @see Restart
 * @see Scope
 */
public interface Handler extends Predicate<Condition>, BiFunction<Condition, Scope, Object> {
  /**
   * Returned when a handler, for whatever reason, opts to not handle a particular condition. By returning this, other
   * handlers, bound later in the stack, will have the chance instead.
   */
  Object SKIP = new Object() {
    @Override
    public String toString() {
      return "Handler.SKIP";
    }
  };
}
