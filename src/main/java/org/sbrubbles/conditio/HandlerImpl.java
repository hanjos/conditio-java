package org.sbrubbles.conditio;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 * A simple implementation of {@link Handler}, which delegates its functionality to its attributes.
 */
class HandlerImpl<T> implements Handler<T> {
  private final Class<? extends Condition> conditionType;
  private final BiFunction<? extends Condition, Scope, T> body;

  /**
   * Creates a new instance, ensuring statically that the given parameters are type-compatible.
   *
   * @param conditionType the type of {@link Condition} this handler expects.
   * @param body          a function which receives a condition and returns the end result.
   * @throws NullPointerException if any of the arguments are {@code null}.
   */
  <C extends Condition, S extends C> HandlerImpl(Class<S> conditionType, BiFunction<C, Scope, T> body) {
    Objects.requireNonNull(conditionType, "conditionType");
    Objects.requireNonNull(body, "body");

    this.conditionType = conditionType;
    this.body = body;
  }

  @Override
  public boolean test(Condition condition) {
    return getConditionType().isInstance(condition);
  }

  @SuppressWarnings("unchecked")
  @Override
  public T apply(Condition c, Scope s) {
    return (T) ((BiFunction) getBody()).apply(c, s);
  }

  public Class<? extends Condition> getConditionType() {
    return conditionType;
  }

  public BiFunction<? extends Condition, Scope, ?> getBody() {
    return body;
  }
}
