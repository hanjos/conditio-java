package org.sbrubbles.conditio.fixtures.skipping;

import org.sbrubbles.conditio.Condition;
import org.sbrubbles.conditio.Scope;
import org.sbrubbles.conditio.fixtures.AbstractFixture;

public class SkippingFixture extends AbstractFixture {
  public String c(Condition condition) {
    try (Scope scope = Scope.create()) {
      return (String) scope.signal(condition);
    }
  }

  public String b(Condition condition) {
    try (Scope scope = Scope.create()) {
      scope.handle(SkipHandler.class,
        traceHandler("b", (c, ops) -> ops.skip()));

      return c(condition);
    }
  }

  public String a(Condition condition) {
    try (Scope scope = Scope.create()) {
      scope.handle(SkipHandler.class,
        traceHandler("a", (c, ops) -> ops.use(EXPECTED_RESULT)));

      return b(condition);
    }
  }

  public static final String EXPECTED_RESULT = "<result>";
}
