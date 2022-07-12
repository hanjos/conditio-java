package org.sbrubbles.conditio;

public class HandlerNotFoundException extends RuntimeException {
  private Object signal;

  public HandlerNotFoundException(Object signal) {
    super("No handler found for signal " + signal);

    this.signal = signal;
  }

  public Object getSignal() {
    return signal;
  }
}
