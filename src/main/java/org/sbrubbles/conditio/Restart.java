package org.sbrubbles.conditio;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Provides a strategy to deal with {@linkplain Condition conditions}.
 * <p>
 * Similarly to a handler, a restart can do two things: check if it accepts a given restart option (with
 * {@link #test(Object)}), and analyze said option, producing the result to be returned by
 * {@link Scope#signal(Condition) Scope.signal}.
 * <p>
 * Since a restart works both as a {@linkplain Predicate predicate} and as a {@linkplain Function function}, this
 * interface extends both.
 *
 * @see Condition
 * @see Restart.Option
 * @see Scope
 */
public interface Restart extends Predicate<Restart.Option>, Function<Restart.Option, Object> {
  /**
   * A marker interface, implemented by all valid restart options.
   */
  interface Option { /**/ }
}
