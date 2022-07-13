package org.sbrubbles.conditio;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Handles {@link Condition conditions}, by selecting and returning the {@link Restart restart option} to use.
 */
public interface Handler {
  boolean accepts(Object signal);

  Restart.Option handle(Condition c);

  class Impl implements Handler {
    private final Predicate<?> checker;
    private final Function<Condition, Restart.Option> body;

    public Impl(Predicate<?> checker, Function<Condition, Restart.Option> body) {
      Objects.requireNonNull(checker, "checker");
      Objects.requireNonNull(body, "body");

      this.checker = checker;
      this.body = body;
    }

    @Override
    public boolean accepts(Object signal) {
      return ((Predicate) this.checker).test(signal);
    }

    @Override
    public Restart.Option handle(Condition c) {
      return this.body.apply(c);
    }
  }

}
