package org.sbrubbles.conditio.fixtures.warning;

import org.sbrubbles.conditio.conditions.Warning;

import java.io.PrintStream;

public class IntWarning extends Warning {
  private final int number;

  public IntWarning(int number) {
    this(number, System.out);
  }

  public IntWarning(int number, PrintStream output) {
    super(Integer.toString(number), output);

    this.number = number;
  }

  public int getNumber() {
    return number;
  }
}
