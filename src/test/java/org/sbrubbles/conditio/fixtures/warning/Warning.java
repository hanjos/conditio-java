package org.sbrubbles.conditio.fixtures.warning;

import org.sbrubbles.conditio.Scope;
import org.sbrubbles.conditio.Signal;

import java.io.PrintStream;

public class Warning implements Signal {
  private final PrintStream out;
  private final int number;

  public Warning(int number, PrintStream out) {
    this.number = number;
    this.out = out;
  }

  public int getNumber() {
    return number;
  }

  public PrintStream getOut() {
    return out;
  }

  @Override
  public Void onHandlerNotFound(Scope scope) {
    getOut().println("Warning: " + getNumber());

    return null;
  }
}
