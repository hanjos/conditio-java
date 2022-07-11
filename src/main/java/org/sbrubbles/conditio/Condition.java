package org.sbrubbles.conditio;

/**
 * Represents an exceptional situation, which needs to be signalled, {@link Handler handled} and
 * {@link Restart dealt with}, all of which may happen at different points in the stack.
 */
public class Condition {
  private final Object signal;
  private final Scope scope;

  public Condition(Object signal, Scope scope) {
    this.signal = signal;
    this.scope = scope;
  }

  public Object getSignal() {
    return signal;
  }

  public Scope getScope() {
    return scope;
  }
}
