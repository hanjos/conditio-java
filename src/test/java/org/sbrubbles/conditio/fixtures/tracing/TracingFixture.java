package org.sbrubbles.conditio.fixtures.tracing;

import org.sbrubbles.conditio.Scope;

import java.util.ArrayList;
import java.util.List;

public class TracingFixture {
  public void doWork(int workDone) {
    try (Scope scope = Scope.create()) {
      traceWork(workDone);

      scope.signal(new WorkDone(workDone));
    }
  }

  public void run() {
    try (Scope scope = Scope.create()) {
      scope.handle(WorkDone.class, (c, ops) -> {
        traceHandler("run: " + c.getClass().getSimpleName());

        increaseWorkDone(c.getAmount());

        return ops.resume();
      });

      while (getWorkDone() < getMaxWork()) {
        doWork(1);
      }
    }
  }

  private int workDone;
  private final int maxWork;

  private final List<String> handlerTrace;
  private final List<Integer> workTrace;

  public TracingFixture(int maxWork) {
    this.workDone = 0;
    this.maxWork = maxWork;

    this.handlerTrace = new ArrayList<>();
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

  public List<String> getHandlerTrace() {
    return handlerTrace;
  }

  public List<Integer> getWorkTrace() {
    return workTrace;
  }

  private void traceHandler(String handler) {
    handlerTrace.add(handler);
  }

  private void traceWork(int workDone) {
    workTrace.add(workDone);
  }
}
