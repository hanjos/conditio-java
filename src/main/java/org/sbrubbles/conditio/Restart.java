package org.sbrubbles.conditio;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Represents a recovery strategy. One uses a recovery strategy by
 * {@linkplain Handler.Operations#restart(Restart.Option) calling it} from a
 * {@linkplain Handler handler}, which will select which strategy to use with a {@linkplain Restart.Option restart
 * option}.
 * <p>
 * Similarly to a handler, a restart can do two things:
 * <ul>
 *   <li>check if it accepts a given restart option (with {@link #test(Object) test}); and, if so,</li>
 *   <li>consume said option, computing a result (with {@link #apply(Object) apply}). </li>
 * </ul>
 * <p>
 * Since a restart works both as a {@linkplain Predicate predicate} and as a {@linkplain Function function}, this
 * interface extends both.
 * <p>
 * Handlers are expected to know about the available restarts. It is good practice for a method to document
 * the restarts it establishes, along with those established by any methods it calls.
 *
 * @param <R> the type returned by {@code apply}.
 */
public interface Restart<R> extends Predicate<Restart.Option>, Function<Restart.Option, R> {
  /**
   * Serves both as input for restarts, and a way to select which restart to use: the earliest-bound one in the stack
   * which {@linkplain Restart#test(Object) accepts} it.
   * <p>
   * This is merely a marker interface, with no fields or methods of its own. Implementations typically hold extra
   * fields and data.
   *
   * @see Restarts
   */
  interface Option { }

}
