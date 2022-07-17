package org.sbrubbles.conditio;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Handles {@linkplain Condition conditions} by selecting which {@linkplain Restart.Option restart} to use.
 * <p>
 * A handler can do two things: check if it can handle a given signal (with {@link #test(Object)}), and
 * analyze a given condition, returning which restart should be used (with {@link #apply(Object)}).
 * <p>
 * Since a handler works both as a {@linkplain Predicate<Object> predicate} and as a
 * {@linkplain Function function}, this interface extends both.
 *
 * @see Condition
 * @see Restart.Option
 * @see Restart
 * @see Scope
 */
public interface Handler extends Predicate<Condition>, Function<Condition, Restart.Option> {
  /**
   * Returned when a handler, for whatever reason, can't handle a particular condition. By returning this, other
   * handlers, bound later in the stack, will have the chance to handle the condition.
   */
  Restart.Option SKIP = new Restart.Option() {
    @Override
    public String toString() {
      return "Handler.SKIP";
    }
  };
}
