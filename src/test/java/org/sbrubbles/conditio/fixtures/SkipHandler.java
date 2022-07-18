package org.sbrubbles.conditio.fixtures;

import org.sbrubbles.conditio.Condition;
import org.sbrubbles.conditio.Scope;

public class SkipHandler extends Condition {
  private final Entry value;

  public SkipHandler(Scope scope, Entry value) {
    super(scope);
    this.value = value;
  }

  public Entry getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "OneOff(" + value + ")";
  }
}
