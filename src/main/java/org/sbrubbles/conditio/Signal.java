package org.sbrubbles.conditio;

import org.sbrubbles.conditio.policies.Policies;

import java.util.Objects;

/**
 * Holds data about a specific {@code signal} invocation, including the condition.
 *
 * @param <C> the condition type this signal holds.
 */
public class Signal<C extends Condition> {
  private final C condition;
  private final Policies<?> policies;
  private final Scope scope;

  /**
   * Creates a new instance.
   *
   * @param condition the condition signalled.
   * @param policies  the policies in effect.
   * @param scope     the scope where the condition was signalled.
   * @throws NullPointerException if one or more arguments are null.
   */
  public Signal(C condition, Policies<?> policies, Scope scope) {
    this.condition = Objects.requireNonNull(condition, "condition");
    this.policies = Objects.requireNonNull(policies, "policies");
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
   * The policies in effect for this signal invocation.
   *
   * @return the policies in effect for this signal invocation.
   */
  public Policies<?> getPolicies() { return policies; }

  /**
   * The scope where the condition was emitted.
   *
   * @return the scope where the condition was emitted.
   */
  public Scope getScope() {
    return scope;
  }
}
