package org.sbrubbles.conditio.fixtures.aborting;

import org.sbrubbles.conditio.Scope;
import org.sbrubbles.conditio.Scopes;
import org.sbrubbles.conditio.fixtures.BasicCondition;
import org.sbrubbles.conditio.restarts.Abort;
import org.sbrubbles.conditio.restarts.AbortException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AbortingFixture {
  public static final String FAIL = "FAIL";
  public static final String PASS = "PASS";

  public String signal() {
    try (Scope scope = Scopes.create()) {
      scope.signal(new BasicCondition(""), Abort.INSTANCE);

      return PASS;
    } catch (Exception e) {
      trace.add("signal: " + e.getClass().getSimpleName());

      throw e;
    }
  }

  public String handle() {
    try (Scope scope = Scopes.create()) {
      scope.handle(BasicCondition.class, (c, ops) -> ops.restart(Abort.INSTANCE));

      return signal();
    } catch (AbortException e) {
      trace.add("handle: " + e.getClass().getSimpleName());
    }

    return FAIL;
  }

  private final List<String> trace;

  public AbortingFixture() {
    this.trace = new ArrayList<>();
  }

  public List<String> getTrace() {
    return Collections.unmodifiableList(trace);
  }
}
