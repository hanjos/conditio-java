package org.sbrubbles.conditio.fixtures.warning;

import org.sbrubbles.conditio.Scope;
import org.sbrubbles.conditio.Scopes;
import org.sbrubbles.conditio.fixtures.AbstractFixture;
import org.sbrubbles.conditio.restarts.Restarts;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class WarningFixture extends AbstractFixture {
  public void c(int i) {
    try (Scope scope = Scopes.create()) {
      scope.signal(new IntWarning(i), Restarts.resume());
    }
  }

  public void b(int n) {
    try (Scope scope = Scopes.create()) {
      scope.handle(IntWarning.class,
        traceHandler("b", (c, t, ops) -> {
          if (c.getNumber() % 2 == 0) {
            return ops.restart(Restarts.resume()); // ignore pair warnings
          } else {
            return ops.skip();
          }
        }));

      for (int i = 0; i < n; i++) {
        c(i);
      }
    }
  }

  public List<String> a(int n) {
    try (Scope scope = Scopes.create()) {
      List<String> warnings = new ArrayList<>();

      scope.handle(IntWarning.class,
        traceHandler("a", (c, t, ops) -> {
          warnings.add(c.getMessage());
          return ops.restart(Restarts.resume());
        }));

      b(n);

      return warnings;
    }
  }

  public void noHandler(PrintStream out) {
    try (Scope scope = Scopes.create()) {
      scope.signal(new IntWarning(-1, out));
      scope.signal(new IntWarning(-2, out));
    }
  }
}
