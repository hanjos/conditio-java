package org.sbrubbles.conditio;

public class HandlerNotFound implements Condition {
  private final Signal<?> signal;

  public HandlerNotFound(Signal<?> signal) {
    this.signal = signal;
  }

  public Signal<?> getSignal() {
    return signal;
  }

  @Override
  public String toString() {
    return "No handler found for " + signal;
  }
}
