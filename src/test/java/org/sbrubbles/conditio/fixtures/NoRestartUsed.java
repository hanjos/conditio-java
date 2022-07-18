package org.sbrubbles.conditio.fixtures;

import org.sbrubbles.conditio.Condition;
import org.sbrubbles.conditio.Scope;

public class NoRestartUsed extends Condition {
  private final Object value;

  public NoRestartUsed(Scope scope, Object value) {
    super(scope);

    this.value = value;
  }

  public Object getValue() {
    return value;
  }
}
