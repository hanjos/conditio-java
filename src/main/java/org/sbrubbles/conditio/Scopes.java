package org.sbrubbles.conditio;

/**
 * Manages the stack of nested {@linkplain Scope scopes}. Works as a {@code Scope} factory.
 * <p>
 * This class is not intended to be instanced or subclassed.
 *
 * @see Scope
 */
public final class Scopes {
  private static Scope current;

  private Scopes() { /**/ }

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
