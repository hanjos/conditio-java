package org.sbrubbles.conditio;

public class HandlerNotFoundException extends RuntimeException {
  private Condition condition;

  public HandlerNotFoundException(Condition condition) {
    super("No handler found for condition " + condition);

    this.condition = condition;
  }

  public Condition getCondition() {
    return condition;
  }
}
