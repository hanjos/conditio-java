package org.sbrubbles.conditio.fixtures.logging;

import org.sbrubbles.conditio.Condition;

import java.util.Objects;

public class BasicCondition implements Condition {
  private final Object value;

  public BasicCondition(Object value) {
    this.value = value;
  }

  public Object getValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BasicCondition that = (BasicCondition) o;
    return Objects.equals(getValue(), that.getValue());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getValue());
  }
}
