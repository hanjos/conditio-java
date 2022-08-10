package org.sbrubbles.conditio.fixtures.warning;

import org.sbrubbles.conditio.Condition;
import org.sbrubbles.conditio.HandlerNotFoundException;
import org.sbrubbles.conditio.Scope;
import org.sbrubbles.conditio.policies.HandlerNotFoundPolicy;

import java.io.PrintStream;

public class WarningPolicy implements HandlerNotFoundPolicy<Void> {
  private final PrintStream output;

  public WarningPolicy() {
    this(System.out);
  }

  public WarningPolicy(PrintStream output) {
    this.output = output;
  }

  @Override
  public Void onHandlerNotFound(Condition condition, Scope scope) throws HandlerNotFoundException {
    this.output.println("[WARN] " + condition);
    return null;
  }
}
