package org.sbrubbles.conditio;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Provides a strategy to deal with some exceptional situations.
 */
public interface Restart {
  boolean matches(Option data);

  Object run(Option data);

  class Impl implements Restart {
    private final Predicate<? extends Option> matcher;
    private final Function<? extends Option, ?> body;

    public <T extends Option, S extends T> Impl(Predicate<T> matcher, Function<S, ?> body) {
      Objects.requireNonNull(matcher, "matcher");
      Objects.requireNonNull(body, "body");

      this.matcher = matcher;
      this.body = body;
    }

    @Override
    public boolean matches(Option data) {
      return ((Predicate) this.matcher).test(data);
    }

    @Override
    public Object run(Option data) {
      return ((Function) this.body).apply(data);
    }
  }

  interface Option {}
}
