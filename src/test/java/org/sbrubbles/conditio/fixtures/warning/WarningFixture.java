package org.sbrubbles.conditio.fixtures.warning;

import org.sbrubbles.conditio.Scope;
import org.sbrubbles.conditio.Scopes;
import org.sbrubbles.conditio.fixtures.AbstractFixture;
import org.sbrubbles.conditio.restarts.Restarts;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class WarningFixture extends AbstractFixture {
  public void low(int i) {
    try (Scope scope = Scopes.create()) {
      scope.signal(new IntWarning(i), new WarningPolicy(), Restarts.resume());
    }
  }

  public void mid(int n) {
    try (Scope scope = Scopes.create()) {
      scope.handle(IntWarning.class,
        traceHandler("b", (c, ops) -> {
          if (c.getNumber() % 2 == 0) {
            return ops.restart(Restarts.resume()); // ignore pair warnings
          } else {
            return ops.skip();
          }
        }));

      for (int i = 0; i < n; i++) {
        low(i);
      }
    }
  }

  public List<String> high(int n) {
    try (Scope scope = Scopes.create()) {
      List<String> warnings = new ArrayList<>();

      scope.handle(IntWarning.class,
        traceHandler("a", (c, ops) -> {
          warnings.add(c.getMessage());
          return ops.restart(Restarts.resume());
        }));

      mid(n);

      return warnings;
    }
  }

  public void noHandler(PrintStream out) {
    try (Scope scope = Scopes.create()) {
      scope.signal(new IntWarning(-1), new WarningPolicy(out));
      scope.signal(new IntWarning(-2), new WarningPolicy(out));
    }
  }
}
