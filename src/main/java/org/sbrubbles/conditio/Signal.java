package org.sbrubbles.conditio;

import java.util.Objects;
import java.util.Optional;

/**
 * Holds data about a specific {@code signal} invocation, including the condition.
 *
 * @param <C> the condition type this signal holds.
 */
public class Signal<C extends Condition> {
  private final C condition;
  private final Optional<Class> returnType;
  private final Scope scope;

  /**
   * Creates a new instance.
   *
   * @param condition  the condition signalled.
   * @param returnType the expected type of the result of {@code signal}, or {@code Optional.empty()} if no return is
   *                   expected.
   * @param scope      the scope where the condition was signalled.
   * @throws NullPointerException if one or more arguments are null.
   */
  public Signal(C condition, Optional<Class> returnType, Scope scope) {
    this.condition = Objects.requireNonNull(condition, "condition");
    this.returnType = Objects.requireNonNull(returnType, "returnType");
    this.scope = Objects.requireNonNull(scope, "scope");
  }

  /**
   * The condition signalled.
   *
   * @return the condition signalled.
   */
  public C getCondition() {
    return condition;
  }

  /**
   * The expected return type, wrapped in an {@link Optional}.
   *
   * @return a class with the expected return type, or {@code None} if no return type is expected.
   */
  public Optional<Class> getReturnType() {
    return returnType;
  }

  /**
   * The scope where the condition was emitted.
   *
   * @return the scope where the condition was emitted.
   */
  public Scope getScope() {
    return scope;
  }

  @Override
  public String toString() {
    return super.toString() + "{" +
        "condition=" + condition +
        ", returnType=" + returnType +
        ", scope=" + scope +
        '}';
  }
}
