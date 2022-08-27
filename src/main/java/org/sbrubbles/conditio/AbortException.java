package org.sbrubbles.conditio;

/**
 * Thrown to indicate the desire to interrupt
 * {@link Scope#signal(Condition, org.sbrubbles.conditio.policies.Policies, Restart[]) signal}
 * processing and unwind the stack until captured.
 *
 * @see Handler.Operations#abort()
 */
public class AbortException extends RuntimeException {
  /**
   * Creates a new instance.
   */
  public AbortException() { }
}
