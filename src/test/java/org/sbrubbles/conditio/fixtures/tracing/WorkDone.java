package org.sbrubbles.conditio.fixtures.tracing;

import org.sbrubbles.conditio.Condition;

public class WorkDone implements Condition {
  private final int amount;

  public WorkDone(int amount) {
    this.amount = amount;
  }

  public int getAmount() {
    return amount;
  }
}
