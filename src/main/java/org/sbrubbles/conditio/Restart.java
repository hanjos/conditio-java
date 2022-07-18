package org.sbrubbles.conditio;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Provides a recovery strategy for conditions. It takes a restart option, which typically also holds useful data for
 * the recovery, and computes the end result to be returned.
 * <p>
 * Similarly to a handler, a restart can do two things: check if it accepts a given restart option (with
 * {@link #test(Object)}), and analyze said option, producing the end result. Since it works both as a
 * {@linkplain Predicate predicate} and as a {@linkplain Function function}, this interface extends both.
 *
 * @see Restart.Option
 * @see Scope
 */
public interface Restart extends Predicate<Restart.Option>, Function<Restart.Option, Object> {
  /**
   * Serves both as input for restarts, and a way to select which restart to use: the earliest-bound one in the stack
   * which {@linkplain Restart#test(Object) accepts} it.
   * <p>
   * This is merely a marker interface, with no fields or methods of its own. Implementations typically hold extra
   * fields and data.
   */
  interface Option { /**/ }
}
