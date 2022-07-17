package org.sbrubbles.conditio;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Provides a recovery strategy for conditions. It takes a restart option, which holds useful data for the recovery,
 * and computes an end result, to be returned by {@link Scope#signal(Condition) Scope.signal}.
 * <p>
 * Similarly to a handler, a restart can do two things: check if it accepts a given restart option (with
 * {@link #test(Object)}), and analyze said option, producing the end result. Since it works both as a
 * {@linkplain Predicate predicate} and as a {@linkplain Function function}, this interface extends both.
 *
 * @see Condition
 * @see Restart.Option
 * @see Scope
 */
public interface Restart extends Predicate<Restart.Option>, Function<Restart.Option, Object> {
  /**
   * Serves as input for restarts, and a way to select which restart to use; basically, the earliest-bound one in the
   * stack which {@linkplain Restart#test(Object) accepts} it.
   * <p>
   * This is merely a marker interface; implementations typically hold extra fields and data.
   */
  interface Option { /**/ }
}
