package org.sbrubbles.conditio;

/**
 * Manages the stack of nested {@linkplain Scope scopes}. Works as a {@code Scope} factory,
 * {@linkplain #create(Restart[]) pushing} and popping scopes as needed.
 * <p>
 * This class is not intended to be instanced or subclassed.
 *
 * @see Scope
 */
public final class Scopes {
  private static Scope current;

  private Scopes() { /* empty */ }

  /**
   * Creates and returns a new instance, nested in the (now former) current scope, with the given restarts available.
   *
   * @param restarts The restarts available inside the newly created scope. They all must be non-{@code null}.
   * @return a new instance of {@code Scope}.
   * @throws NullPointerException if either {@code restarts} or one of the given restarts is {@code null}.
   */
  public static Scope create(Restart<?>... restarts) {
    current = new ScopeImpl(current, restarts);

    return current;
  }

  /**
   * "Pops" the current scope from the stack, leaving its {@linkplain Scope#getParent() parent} as the new
   * current scope.
   * <p>
   * This is intended to be called by a scope when it {@linkplain Scope#close() closes}, to correct the nesting. Avoid
   * manual management; {@code try}-with-resources should do all the work :)
   */
  static void retire() {
    if (current == null) {
      return;
    }

    current = current.getParent();
  }
}
