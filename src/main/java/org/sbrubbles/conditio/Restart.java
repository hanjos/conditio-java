package org.sbrubbles.conditio;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Represents a recovery strategy. One uses a recovery strategy by
 * {@linkplain Handler.Context#restart(Restart.Option) calling it} from a
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
   * @see org.sbrubbles.conditio.restarts.Restarts
   */
  interface Option { }

  /**
   * Creates and returns a new restart, with a default implementation.
   *
   * @param optionType the type of {@linkplain Restart.Option restart options} accepted.
   * @param body       the code which will take an instance of {@code optionType} and generate a result.
   * @param <R>        the type of the value returned by {@code body}.
   * @param <O>        a subtype of {@code Restart.Option}.
   * @param <S>        a subtype of {@code O}, so that {@code body} is still compatible with {@code O} but may accept subtypes
   *                   other than {@code S}.
   * @return an instance of Restart, using a default implementation.
   * @throws NullPointerException if one or both parameters are {@code null}.
   */
  static <R, O extends Restart.Option, S extends O> Restart<R> on(Class<S> optionType, Function<O, R> body) {
    return new RestartImpl<>(optionType, body);
  }
}

/**
 * A simple implementation of {@link Restart}, which delegates its functionality to its attributes.
 */
class RestartImpl<R> implements Restart<R> {
  private final Class<? extends Option> optionType;
  private final Function<? extends Option, R> body;

  /**
   * Creates a new instance, ensuring statically that the given parameters are type-compatible.
   *
   * @param optionType the type of {@link Option} this restart expects.
   * @param body       a function which receives a restart option and returns a result.
   * @throws NullPointerException if any of the arguments are {@code null}.
   */
  <O extends Option, S extends O> RestartImpl(Class<S> optionType, Function<O, R> body) {
    Objects.requireNonNull(optionType, "optionType");
    Objects.requireNonNull(body, "body");

    this.optionType = optionType;
    this.body = body;
  }

  @Override
  public boolean test(Option data) {
    return getOptionType().isInstance(data);
  }

  @SuppressWarnings("unchecked")
  @Override
  public R apply(Option data) {
    return (R) ((Function) getBody()).apply(data);
  }

  public Class<? extends Option> getOptionType() {
    return optionType;
  }

  public Function<? extends Option, R> getBody() {
    return body;
  }
}
