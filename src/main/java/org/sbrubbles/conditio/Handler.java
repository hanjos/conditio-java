package org.sbrubbles.conditio;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Handles {@link Condition conditions}, by selecting and returning the {@link Restart restart option} to use.
 */
public interface Handler extends Predicate<Object>, Function<Condition, Restart.Option> {
  class Impl implements Handler {
    private final Class<?> signalType;
    private final Function<Condition, Restart.Option> body;
    private final Scope scope;

    public Impl(Class<?> signalType, Function<Condition, Restart.Option> body, Scope scope) {
      Objects.requireNonNull(signalType, "signalType");
      Objects.requireNonNull(body, "body");
      Objects.requireNonNull(scope, "scope");

      this.signalType = signalType;
      this.body = body;
      this.scope = scope;
    }

    @Override
    public boolean test(Object signal) {
      return getSignalType().isInstance(signal);
    }

    @Override
    public Restart.Option apply(Condition c) {
      return getBody().apply(c);
    }

    public Class<?> getSignalType() {
      return signalType;
    }

    public Function<Condition, Restart.Option> getBody() {
      return body;
    }

    public Scope getScope() {
      return scope;
    }

    @Override
    public String toString() {
      return "Handler.Impl{" +
        "signalType=" + signalType +
        ", body=" + body +
        ", scope=" + scope +
        '}';
    }
  }

  /**
   * To be returned when a handler, for whatever reason, can't handle a particular condition. Other handlers,
   * bound later in the stack, will have the chance to handle the condition.
   */
  Restart.Option SKIP = new Restart.Option() {
    @Override
    public String toString() {
      return "Handler.SKIP";
    }
  };
}
