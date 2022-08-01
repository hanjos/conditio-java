package org.sbrubbles.conditio.fixtures;

import org.sbrubbles.conditio.conditions.Notice;

import java.util.Objects;

public class BasicNotice extends Notice {
  private final String value;

  public BasicNotice(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BasicNotice that = (BasicNotice) o;
    return Objects.equals(getValue(), that.getValue());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getValue());
  }
}
