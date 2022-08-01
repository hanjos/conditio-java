package org.sbrubbles.conditio.restarts;

/**
 * Thrown by {@link Abort} to interrupt processing, and unwind the stack until captured.
 *
 * @see Abort
 */
public class AbortException extends RuntimeException {
  public AbortException() { }
}
