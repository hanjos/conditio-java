package org.sbrubbles.conditio;

/**
 * Represents an unusual situation, which the running code doesn't know how to deal with. Conditions are meant to
 * be {@linkplain Scope#signal(Condition) signalled}.
 * <p>
 * This is the superclass of all conditions.
 *
 * @see Scope#signal(Condition)
 */
public class Condition {
  private final Condition cause;

  /**
   * Creates a new instance.
   */
  public Condition() {
    this(null);
  }

  /**
   * Creates a new instance with the specified cause.
   *
   * @param cause a condition which caused the signalling of this one. May be {@code null}, to indicate that the cause
   *              is nonexistent or unknown.
   */
  public Condition(Condition cause) {
    this.cause = cause;
  }

  /**
   * Returns the condition which caused the signalling of this one, or {@code null} if it's nonexistent or unknown.
   *
   * @return the condition which caused the signalling of this one. May be {@code null}, to indicate that the cause
   * is nonexistent or unknown.
   */
  public Condition getCause() {
    return cause;
  }
}
