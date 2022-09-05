package org.sbrubbles.conditio.fixtures.warning;

import org.sbrubbles.conditio.Condition;

public class IntWarning implements Condition {
  private final int number;

  public IntWarning(int number) {
    this.number = number;
  }

  public int getNumber() {
    return number;
  }

  public String getMessage() {
    return toString();
  }

  @Override
  public String toString() {
    return Integer.toString(getNumber());
  }
}
