package org.sbrubbles.conditio;

/**
 * Manages a stack of nested {@linkplain Scope scopes}. This class is not a resource itself; it just manages the
 * nesting by controlling its scopes' lifetime.
 *
 * @see Scope
 */
public final class Stack {
  private static Scope current;

  private Stack() { /**/ }

  /**
   * Creates and returns a new instance, nested in the (now former) current scope.
   *
   * @return a new instance of {@code Scope}.
   */
  public static Scope create() {
    current = new ScopeImpl(current);

    return current;
  }

  /**
   * To be called when a scope {@linkplain Scope#close() closes}, to correct the nesting.
   */
  static void close() {
    if (current == null) {
      return;
    }

    current = current.getParent();
  }
}
