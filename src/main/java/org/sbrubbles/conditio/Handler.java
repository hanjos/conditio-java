package org.sbrubbles.conditio;

import java.util.Objects;
import java.util.function.Function;

/**
 * Handles {@link Condition conditions}, by selecting and returning the {@link Restart restart option} to use.
 */
public interface Handler {
  boolean accepts(Object signal);

  Restart.Option handle(Condition c);

  class Impl implements Handler {
    private final Class<?> checker;
    private final Function<Condition, Restart.Option> body;
    private final Scope scope;

    public Impl(Class<?> checker, Function<Condition, Restart.Option> body, Scope scope) {
      Objects.requireNonNull(checker, "checker");
      Objects.requireNonNull(body, "body");

      this.checker = checker;
      this.body = body;
      this.scope = scope;
    }

    @Override
    public boolean accepts(Object signal) {
      return this.checker.isInstance(signal);
    }

    @Override
    public Restart.Option handle(Condition c) {
      return this.body.apply(c);
    }

    @Override
    public String toString() {
      return "Handler.Impl{" +
        "checker=" + checker +
        ", body=" + body +
        ", scope=" + scope +
        '}';
    }
  }

  /**
   * To be returned when a handler, for whatever reason, can't handle a particular condition. Then, other handlers,
   * bound later, will have the chance to handle the condition.
   */
  Restart.Option SKIP = new Restart.Option() {
    @Override
    public String toString() {
      return "Handler.SKIP";
    }
  };
}
