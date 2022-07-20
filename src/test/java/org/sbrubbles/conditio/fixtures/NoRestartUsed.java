package org.sbrubbles.conditio.fixtures;

import org.sbrubbles.conditio.Condition;

public class NoRestartUsed extends Condition {
  private final Object value;

  public NoRestartUsed(Object value) {
    this.value = value;
  }

  public Object getValue() {
    return value;
  }
}
