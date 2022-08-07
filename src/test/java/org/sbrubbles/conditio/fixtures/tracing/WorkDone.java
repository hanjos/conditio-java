package org.sbrubbles.conditio.fixtures.tracing;

import org.sbrubbles.conditio.conditions.Notice;

public class WorkDone extends Notice {
  private final int amount;

  public WorkDone(int amount) {
    this.amount = amount;
  }

  public int getAmount() {
    return amount;
  }
}
