package org.sbrubbles.conditio.fixtures;

public class UseValue {
  Object value;

  public UseValue(Object value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return "UseValue(" + value + ")";
  }
}
