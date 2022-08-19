package org.sbrubbles.conditio;

import org.sbrubbles.conditio.policies.HandlerNotFoundPolicy;

/**
 * Thrown to indicate the desire to interrupt {@link Scope#signal(Condition, HandlerNotFoundPolicy, Restart[]) signal}
 * processing and unwind the stack until captured.
 *
 * @see Handler.Context#abort()
 */
public class AbortException extends RuntimeException {
  /**
   * Creates a new instance.
   */
  public AbortException() { }
}
