package org.sbrubbles.conditio.fixtures.warning;

import org.sbrubbles.conditio.Signal;

public class Warning implements Signal {
  private int number;

  public Warning(int number) {
    this.number = number;
  }

  public int getNumber() {
    return number;
  }
}
