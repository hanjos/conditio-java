package org.sbrubbles.conditio.fixtures.warning;

import org.sbrubbles.conditio.conditions.Warning;

import java.io.PrintStream;

public class IntWarning implements Warning {
  private final PrintStream output;
  private final int number;

  public IntWarning(int number) {
    this(number, System.out);
  }

  public IntWarning(int number, PrintStream output) {
    this.number = number;
    this.output = output;
  }

  @Override
  public String getMessage() {
    return Integer.toString(number);
  }

  @Override
  public PrintStream getOutput() {
    return output;
  }

  public int getNumber() {
    return number;
  }
}
