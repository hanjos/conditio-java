package org.sbrubbles.conditio;

import java.util.Objects;

/**
 * Conditions represent exceptional situations, which the running code doesn't know how to deal with. They are meant to
 * be {@linkplain Scope#signal(Condition) signalled}, an operation which will search for and run recovery code in the
 * {@linkplain Scope call stack} to provide a result. Therefore, conditions should only be created inside a scope.
 * <p>
 * This class provides some common behaviors for conditions, and is intended to be subclassed.
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
