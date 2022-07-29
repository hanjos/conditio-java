package org.sbrubbles.conditio.fixtures.warning;

import org.sbrubbles.conditio.Scope;
import org.sbrubbles.conditio.Scopes;
import org.sbrubbles.conditio.fixtures.AbstractFixture;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class WarningFixture extends AbstractFixture {
  public void c(int i) {
    try (Scope scope = Scopes.create()) {
      scope.signal(new Warning(i, System.out));
    }
  }

  public void b(int n) {
    try (Scope scope = Scopes.create()) {
      scope.handle(Warning.class,
        traceHandler("b", (c, ops) -> {
          if (c.getNumber() % 2 == 0) {
            return ops.resume(); // ignore pair warnings
          } else {
            return ops.skip();
          }
        }));

      for (int i = 0; i < n; i++) {
        c(i);
      }
    }
  }

  public List<Integer> a(int n) {
    try (Scope scope = Scopes.create()) {
      List<Integer> warnings = new ArrayList<>();

      scope.handle(Warning.class,
        traceHandler("a", (c, ops) -> {
          warnings.add(c.getNumber());
          return ops.resume();
        }));

      b(n);

      return warnings;
    }
  }

  public void noHandler(PrintStream out) {
    try (Scope scope = Scopes.create()) {
      scope.signal(new Warning(-1, out));
      scope.signal(new Warning(-2, out));
    }
  }
}
