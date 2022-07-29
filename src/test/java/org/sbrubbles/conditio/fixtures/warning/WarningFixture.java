package org.sbrubbles.conditio.fixtures.warning;

import org.sbrubbles.conditio.Scope;
import org.sbrubbles.conditio.Stack;
import org.sbrubbles.conditio.fixtures.AbstractFixture;

import java.util.ArrayList;
import java.util.List;

public class WarningFixture extends AbstractFixture {
  public static final String RESULT = "OK";

  public String c(int i) {
    try (Scope scope = Stack.create()) {
      scope.signal(new Warning(i));

      return RESULT;
    }
  }

  public void b(int n) {
    try (Scope scope = Stack.create()) {
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
    try (Scope scope = Stack.create()) {
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
}
