package org.sbrubbles.conditio.fixtures;

import org.sbrubbles.conditio.conditions.Signal;

import java.util.Objects;

public class BasicSignal extends Signal {
  private final String value;

  public BasicSignal(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BasicSignal that = (BasicSignal) o;
    return Objects.equals(getValue(), that.getValue());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getValue());
  }
}
