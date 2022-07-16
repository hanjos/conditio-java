package org.sbrubbles.conditio;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Provides a strategy to deal with {@linkplain Condition conditions}.
 * <p>
 * Similarly to a handler, a restart can do two things: check if it accepts a given restart option (with
 * {@link #test(Object)}), and analyze said option, producing the result to be returned by
 * {@link Scope#signal(Condition) Scope.signal}.
 * <p>
 * Since a restart works both as a {@linkplain Predicate predicate} and as a {@linkplain Function function}, this
 * interface extends both.
 *
 * @see Condition
 * @see Restart.Option
 * @see Scope
 */
public interface Restart extends Predicate<Restart.Option>, Function<Restart.Option, Object> {
  /**
   * A simple implementation of {@link Restart}, which delegates its functionality to its attributes.
   */
  class Impl implements Restart {
    private final Class<? extends Option> optionType;
    private final Function<? extends Option, ?> body;
    private final Scope scope;

    /**
     * Creates a new instance, ensuring statically that the given parameters are type-compatible.
     *
     * @param optionType the type of {@link Option} this restart expects.
     * @param body       a function which receives a restart option and returns the result of
     *                   {@link Scope#signal(Condition) signal}.
     * @param scope      the {@link Scope} instance where this restart was created.
     * @throws NullPointerException if any of the arguments are {@code null}.
     */
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
   * A marker interface, implemented by all valid restart options.
   */
  interface Option { /**/ }
}
