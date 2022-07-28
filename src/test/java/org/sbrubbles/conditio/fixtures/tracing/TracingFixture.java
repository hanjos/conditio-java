package org.sbrubbles.conditio.fixtures.tracing;

import org.sbrubbles.conditio.Scope;
import org.sbrubbles.conditio.Stack;
import org.sbrubbles.conditio.fixtures.AbstractFixture;

import java.util.ArrayList;
import java.util.List;

public class TracingFixture extends AbstractFixture {
  public void doWork(int workDone) {
    try (Scope scope = Stack.create()) {
      traceWork(workDone);

      scope.signal(new WorkDone(workDone));
    }
  }

  public void resume() {
    try (Scope scope = Stack.create()) {
      scope.handle(WorkDone.class, traceHandler("run", (c, ops) -> {
        increaseWorkDone(c.getAmount());

        return ops.resume();
      }));

      while (getWorkDone() < getMaxWork()) {
        doWork(1);
      }
    }
  }

  private int workDone;
  private final int maxWork;

  private final List<Integer> workTrace;

  public TracingFixture(int maxWork) {
    this.workDone = 0;
    this.maxWork = maxWork;

    this.workTrace = new ArrayList<>();
  }

  public int getWorkDone() {
    return workDone;
  }

  public void increaseWorkDone(int dw) {
    this.workDone += dw;
  }

  public int getMaxWork() {
    return maxWork;
  }

  public List<Integer> getWorkTrace() {
    return workTrace;
  }

  private void traceWork(int workDone) {
    workTrace.add(workDone);
  }
}
