package org.sbrubbles.conditio.restarts;

import org.sbrubbles.conditio.Restart;
import org.sbrubbles.conditio.Scope;

import java.util.Objects;
import java.util.function.Function;

/**
 * Some general use restarts and restart options, ready for consumption.
 * <p>
 * This class acts as a namespace, and isn't meant to be inherited or instantiated.
 */
public final class Restarts {
  private Restarts() { }

  @SuppressWarnings("rawtypes")
  private static final Resume RESUME = new Resume<>();

  /**
   * A restart, that also works as its own restart option, which {@linkplain Resume continues execution "without"
   * returning a value}.
   *
   * @return a pre-built instance of {@link Resume}.
   * @see Resume
   */
  @SuppressWarnings("unchecked")
  public static <R> Resume<R> resume() {
    return RESUME;
  }

  /**
   * A {@linkplain UseValue restart option} which indicates that {@link Scope#signal Scope.signal} should return the
   * given value.
   *
   * @param value the value to be returned by {@code signal}.
   * @return a restart option.
   * @see #useValue()
   */
  public static <R> UseValue<R> use(R value) { return new UseValue<>(value); }

  /**
   * A restart which matches {@link UseValue}, extracting its value.
   *
   * @return a restart matching {@code UseValue}.
   * @see #use(Object)
   */
  public static <R> Restart<R> useValue() { return on(UseValue.class, UseValue<R>::getValue); }

  /**
   * Creates and returns a new restart, with a default implementation.
   *
   * @param optionType the type of {@linkplain Restart.Option restart options} accepted.
   * @param body       the code which will take an instance of {@code optionType} and generate a result.
   * @param <R>        the type of the value returned by {@code body}.
   * @param <O>        a subtype of {@code Restart.Option}.
   * @param <S>        a subtype of {@code O}, so that {@code body} is still compatible with {@code O} but may accept
   *                   subtypes
   *                   other than {@code S}.
   * @return an instance of Restart, using a default implementation.
   * @throws NullPointerException if one or both parameters are {@code null}.
   */
  public static <R, O extends Restart.Option, S extends O> Restart<R> on(Class<S> optionType, Function<O, R> body) {
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
