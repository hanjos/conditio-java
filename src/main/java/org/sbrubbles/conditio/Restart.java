package org.sbrubbles.conditio;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Provides a strategy to deal with some exceptional situations.
 */
public interface Restart {
  boolean matches(Object data);

  Object run(Object data);

  class Impl implements Restart {
    private final Predicate<?> matcher;
    private final Function<?, ?> body;

    public <T, S extends T> Impl(Predicate<T> matcher, Function<S, ?> body) {
      Objects.requireNonNull(matcher, "matcher");
      Objects.requireNonNull(body, "body");

      this.matcher = matcher;
      this.body = body;
    }

    @Override
    public boolean matches(Object data) {
      return ((Predicate) this.matcher).test(data);
    }

    @Override
    public Object run(Object data) {
      return ((Function) this.body).apply(data);
    }
  }
}
