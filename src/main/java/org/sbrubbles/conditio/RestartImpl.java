package org.sbrubbles.conditio;

import java.util.Objects;
import java.util.function.Function;

/**
 * A simple implementation of {@link Restart}, which delegates its functionality to its attributes.
 */
class RestartImpl implements Restart {
  private final Class<? extends Option> optionType;
  private final Function<? extends Option, ?> body;

  /**
   * Creates a new instance, ensuring statically that the given parameters are type-compatible.
   *
   * @param optionType the type of {@link Option} this restart expects.
   * @param body       a function which receives a restart option and returns a result.
   * @throws NullPointerException if any of the arguments are {@code null}.
   */
  <O extends Option, S extends O> RestartImpl(Class<S> optionType, Function<O, ?> body) {
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
  public Object apply(Option data) {
    return ((Function) getBody()).apply(data);
  }

  public Class<? extends Option> getOptionType() {
    return optionType;
  }

  public Function<? extends Option, ?> getBody() {
    return body;
  }
}
