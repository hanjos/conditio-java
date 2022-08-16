package org.sbrubbles.conditio.fixtures.aborting;

import org.sbrubbles.conditio.AbortException;
import org.sbrubbles.conditio.Scope;
import org.sbrubbles.conditio.Scopes;
import org.sbrubbles.conditio.fixtures.BasicCondition;
import org.sbrubbles.conditio.handlers.Handlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AbortingFixture {
  public static final String HANDLE = "HANDLE";
  public static final String SIGNAL = "SIGNAL";

  public String signal() {
    try (Scope scope = Scopes.create()) {
      scope.raise(new BasicCondition(""));

      return SIGNAL;
    } catch (Exception e) {
      trace.add("signal: " + e.getClass().getSimpleName());

      throw e;
    }
  }

  public String passThrough() {
    try {
      return signal();
    } catch (Exception e) {
      trace.add("passThrough: " + e.getClass().getSimpleName());

      throw e;
    }
  }

  public String handle() {
    try (Scope scope = Scopes.create()) {
      scope.handle(BasicCondition.class, Handlers.abort());

      return passThrough();
    } catch (AbortException e) {
      trace.add("handle: " + e.getClass().getSimpleName());
    }

    return HANDLE;
  }

  private final List<String> trace;

  public AbortingFixture() {
    this.trace = new ArrayList<>();
  }

  public List<String> getTrace() {
    return Collections.unmodifiableList(trace);
  }
}
