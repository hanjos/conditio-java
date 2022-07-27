package org.sbrubbles.conditio.fixtures.logging;

import org.sbrubbles.conditio.Condition;

public class NoRestartUsed implements Condition {
  private final Object value;

  public NoRestartUsed(Object value) {
    this.value = value;
  }

  public Object getValue() {
    return value;
  }
}
