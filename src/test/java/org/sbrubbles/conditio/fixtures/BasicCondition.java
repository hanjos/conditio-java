package org.sbrubbles.conditio.fixtures;

import org.sbrubbles.conditio.Condition;

public class BasicCondition implements Condition {
  private final Object value;

  public BasicCondition(Object value) {
    this.value = value;
  }

  public Object getValue() {
    return value;
  }
}
