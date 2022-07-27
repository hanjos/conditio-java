package org.sbrubbles.conditio.fixtures.skipping;

import org.sbrubbles.conditio.Condition;

import java.util.Objects;

public class SkipHandler implements Condition {
  private final String value;

  public SkipHandler(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "SkipHandler(" + value + ")";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SkipHandler that = (SkipHandler) o;
    return Objects.equals(getValue(), that.getValue());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getValue());
  }
}
