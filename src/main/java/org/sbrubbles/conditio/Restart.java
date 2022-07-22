package org.sbrubbles.conditio;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Provides a recovery strategy. It takes a restart option, which typically also holds useful data for this recovery,
 * and computes a result.
 * <p>
 * Similarly to a handler, a restart can do two things:
 * <ul>
 *   <li>check if it accepts a given restart option (with {@link #test(Object)}); and</li>
 *   <li>analyze said option, producing the end result (with {@link #apply(Object)}). </li>
 * </ul>
 * <p>
 * Since handlers are expected to know about the available restarts, it is good practice for a method to document
 * the restarts it sets, along with those set by any methods it calls.
 * <p>
 * Since a restart works both as a {@linkplain Predicate predicate} and as a {@linkplain Function function}, this
 * interface extends both.
 *
 * @see Restart.Option
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
