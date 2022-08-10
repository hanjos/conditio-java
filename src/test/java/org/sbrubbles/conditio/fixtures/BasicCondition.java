package org.sbrubbles.conditio.fixtures;

import org.sbrubbles.conditio.Condition;

import java.util.Objects;

public class BasicCondition extends Condition {
  private final String value;

  public BasicCondition(String value) {
    this.value = value;
  }

  public String getValue() {
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
