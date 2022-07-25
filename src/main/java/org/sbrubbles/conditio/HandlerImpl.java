package org.sbrubbles.conditio;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 * A simple implementation of {@link Handler}, which delegates its functionality to its attributes.
 */
class HandlerImpl implements Handler {
  private final Class<? extends Condition> conditionType;
  private final BiFunction<? extends Condition, Scope, ?> body;

  /**
   * Creates a new instance, ensuring statically that the given parameters are type-compatible.
   *
   * @param conditionType the type of {@link Condition} this handler expects.
   * @param body          a function which receives a condition and returns the end result.
   * @throws NullPointerException if any of the arguments are {@code null}.
   */
  <T extends Condition, S extends T> HandlerImpl(Class<S> conditionType, BiFunction<T, Scope, ?> body) {
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
  public Object apply(Condition c, Scope s) {
    return ((BiFunction) getBody()).apply(c, s);
  }

  public Class<? extends Condition> getConditionType() {
    return conditionType;
  }

  public BiFunction<? extends Condition, Scope, ?> getBody() {
    return body;
  }
}
