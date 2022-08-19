package org.sbrubbles.conditio.fixtures.warning;

import org.sbrubbles.conditio.Handler;
import org.sbrubbles.conditio.HandlerNotFoundException;
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
  public Void onHandlerNotFound(Handler.Context<?> context) throws HandlerNotFoundException {
    this.output.println("[WARN] " + context.getCondition());
    return null;
  }
}
