package org.sbrubbles.conditio.fixtures.resignalling;

import org.sbrubbles.conditio.Scope;
import org.sbrubbles.conditio.fixtures.AbstractFixture;

public class ResignallingFixture extends AbstractFixture {
  public String cDoesWork() {
    try (Scope scope = Scope.create()) {
      return (String) scope.signal(new PleaseSignalSomethingElse());
    }
  }

  public String bCallsC() {
    try (Scope scope = Scope.create()) {
      scope.handle(SomethingElse.class, traceHandler("bCallsC",
        (c, ops) -> ops.use(FIXED_RESULT)));

      return cDoesWork();
    }
  }

  public String aCallsB() {
    try (Scope scope = Scope.create()) {
      scope.handle(PleaseSignalSomethingElse.class, traceHandler("aCallsB",
        (c, ops) -> {
          try (Scope s = Scope.create()) {
            return ops.use(s.signal(new SomethingElse()));
          }
        }));

      return bCallsC();
    }
  }

  public static final String FIXED_RESULT = "<result>";

  public ResignallingFixture() { }
}
