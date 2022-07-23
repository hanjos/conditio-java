package org.sbrubbles.conditio;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Represents a recovery strategy, to be selected by a {@linkplain Handler handler} via an appropriate
 * {@linkplain Restart.Option restart option}.
 * <p>
 * Similarly to a handler, a restart can do two things:
 * <ul>
 *   <li>check if it accepts a given restart option (with {@link #test(Object) test}); and, if so,</li>
 *   <li>use said option, computing a result (with {@link #apply(Object) apply}). </li>
 * </ul>
 * <p>
 * Restart options have two purposes: they both carry useful data for the restart, and are a way to select which
 * restart to use: the earliest-bound one in the stack that accepts it.
 * <p>
 * Handlers are expected to know about the available restarts. It is good practice for a method to document
 * the restarts it establishes, along with those established by any methods it calls.
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

  /**
   * Creates and returns a new restart, with a default implementation.
   *
   * @param optionType the type of {@linkplain Restart.Option restart options} accepted.
   * @param body       the code which will take an instance of {@code optionType} and generate a result.
   * @return an instance of Restart, using a default implementation.
   * @throws NullPointerException if one or both parameters are {@code null}.
   */
  static <O extends Restart.Option, S extends O> Restart on(Class<S> optionType, Function<O, ?> body) {
    return new RestartImpl(optionType, body);
  }
}
