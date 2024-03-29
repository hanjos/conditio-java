package org.sbrubbles.conditio.fixtures.tracing;

import org.sbrubbles.conditio.Restarts;
import org.sbrubbles.conditio.Scope;
import org.sbrubbles.conditio.Scopes;
import org.sbrubbles.conditio.fixtures.AbstractFixture;

import java.util.ArrayList;
import java.util.List;

public class TracingFixture extends AbstractFixture {
  public void doWork(int workDone) {
    try (Scope scope = Scopes.create()) {
      traceWork(workDone);

      scope.notify(new WorkDone(workDone));
    }
  }

  public void resume() {
    try (Scope scope = Scopes.create()) {
      scope.handle(WorkDone.class, traceHandler("run", (s, ops) -> {
        increaseWorkDone(s.getCondition().getAmount());

        return ops.restart(Restarts.resume());
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
