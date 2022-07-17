package org.sbrubbles.conditio;

import java.util.Objects;

/**
 * Represents an unusual situation, which the running code doesn't know how to deal with. Conditions are meant to
 * be {@linkplain Scope#signal(Condition) signalled}; therefore, they should only be created inside a
 * {@linkplain Scope scope}.
 * <p>
 * Since signalling doesn't unwind the stack (unless that was the recovery strategy selected), a condition is more
 * general than an exception, and enables strategies and protocols for things other than error handling.
 * <p>
 * This is the superclass of all conditions.
 *
 * @see Scope
 */
public abstract class Condition {
  private final Scope scope;

  /**
   * Creates a new instance.
   *
   * @param scope the {@linkplain Scope scope} where this condition was created.
   * @throws NullPointerException if the given scope is {@code null}.
   */
  public Condition(Scope scope) {
    Objects.requireNonNull(scope, "scope");

    this.scope = scope;
  }

  /**
   * The scope where this instance was created.
   *
   * @return the scope where this instance was created.
   */
  public Scope getScope() {
    return scope;
  }
}
