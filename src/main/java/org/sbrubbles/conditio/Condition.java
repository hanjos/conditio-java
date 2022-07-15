package org.sbrubbles.conditio;

import java.util.Objects;

/**
 * Represents an exceptional situation, which needs to be {@linkplain Scope#signal(Condition) signalled},
 * {@linkplain Handler handled} and {@linkplain Restart dealt with}, all of which may happen at different points in
 * the stack.
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
