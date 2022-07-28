package org.sbrubbles.conditio;

/**
 * Indicates the ability to set restarts.
 *
 * @see Scope
 */
public interface WithRestarts {
  /**
   * Sets the given restarts.
   *
   * @param restarts some restarts to set.
   */
  void set(Restart... restarts);
}
