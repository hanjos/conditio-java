package org.sbrubbles.conditio.fixtures;

import org.sbrubbles.conditio.Condition;

public class SkipHandler implements Condition {
  private final Entry value;

  public SkipHandler(Entry value) {
    this.value = value;
  }

  public Entry getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "SkipHandler(" + value + ")";
  }
}
