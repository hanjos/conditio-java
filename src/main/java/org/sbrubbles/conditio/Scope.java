package org.sbrubbles.conditio;

public class Scope implements AutoCloseable {
  // the current scope in execution
  private static Scope current = null;

  // this scope's daddy
  private final Scope parent;

  // private to ensure creation only via #create(), so that current is updated accordingly
  private Scope(Scope parent) {
    this.parent = parent;
  }

  // === scope management ===

  /**
   * Creates and returns a new {@link Scope} instance, keeping track of the current {@code scope} in... well, scope :)
   *
   * @return a new instance of {@link Scope}.
   */
  public static Scope create() {
    current = new Scope(current);

    return current;
  }

  /**
   * The {@link Scope} instance wrapping this one. May be {@code null} if this is the topmost {@code Scope}.
   *
   * @return the {@link Scope} instance wrapping this one, or {@code null} if this is a root scope.
   */
  public Scope getParent() {
    return parent;
  }

  /**
   * If this is the topmost scope in its execution.
   *
   * @return {@code true} if this is the topmost scope.
   */
  public boolean isRoot() {
    return getParent() == null;
  }

  /**
   * Updates the current scope when execution leaves the {@code try} block.
   */
  @Override
  public void close() {
    current = getParent();
  }
}
