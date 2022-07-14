package org.sbrubbles.conditio;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Provides a strategy to deal with some exceptional situations.
 */
public interface Restart extends Predicate<Restart.Option>, Function<Restart.Option, Object> {
  class Impl implements Restart {
    private final Class<? extends Option> optionType;
    private final Function<? extends Option, ?> body;
    private final Scope scope;

    public <T extends Option, S extends T> Impl(Class<S> optionType, Function<T, ?> body, Scope scope) {
      Objects.requireNonNull(optionType, "optionType");
      Objects.requireNonNull(body, "body");
      Objects.requireNonNull(scope, "scope");

      this.optionType = optionType;
      this.body = body;
      this.scope = scope;
    }

    @Override
    public boolean test(Option data) {
      return getOptionType().isInstance(data);
    }

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

    public Scope getScope() {
      return scope;
    }
  }

  /**
   * Marker interface "implemented" by all valid restart options.
   */
  interface Option { /**/ }
}
