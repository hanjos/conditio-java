package org.sbrubbles.conditio.fixtures;

import org.sbrubbles.conditio.Condition;
import org.sbrubbles.conditio.Scope;

public class BasicCondition extends Condition {
  private final Object value;

  public BasicCondition(Object value, Scope scope) {
    super(scope);

    this.value = value;
  }

  public Object getValue() {
    return value;
  }
}
