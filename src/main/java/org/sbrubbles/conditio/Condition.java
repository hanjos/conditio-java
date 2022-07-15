package org.sbrubbles.conditio;

/**
 * Represents an exceptional situation, which needs to be {@linkplain Scope#signal(Condition) signalled},
 * {@linkplain Handler handled} and {@linkplain Restart dealt with}, all of which may happen at different points in
 * the stack.
 * <p>
 * The constructor obtains and stores the current {@linkplain Scope scope}, so any condition should be created inside a
 * try-with-resources.
 */
public abstract class Condition {
  private final Scope scope;

  public Condition() {
    this.scope = Scope.current();
  }

  public Scope getScope() {
    return scope;
  }
}
