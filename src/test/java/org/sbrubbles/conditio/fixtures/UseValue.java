package org.sbrubbles.conditio.fixtures;

import org.sbrubbles.conditio.Restart;

import java.util.Objects;

public class UseValue implements Restart.Option {
  Object value;

  public UseValue(Object value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UseValue useValue = (UseValue) o;
    return Objects.equals(value, useValue.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return "UseValue(" + value + ")";
  }
}
