package org.sbrubbles.conditio.fixtures.warning;

import org.sbrubbles.conditio.Scope;
import org.sbrubbles.conditio.Scopes;
import org.sbrubbles.conditio.fixtures.AbstractFixture;
import org.sbrubbles.conditio.policies.Policies;
import org.sbrubbles.conditio.policies.ReturnTypePolicy;
import org.sbrubbles.conditio.restarts.Restarts;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unchecked")
public class WarningFixture extends AbstractFixture {
  public void low(int i) {
    try (Scope scope = Scopes.create()) {
      scope.signal(new IntWarning(i), new Policies<>(new WarningPolicy(), ReturnTypePolicy.ignore()), Restarts.resume());
    }
  }

  public void mid(int n) {
    try (Scope scope = Scopes.create()) {
      scope.handle(IntWarning.class,
        traceHandler("b", (ctx, ops) -> {
          if (ctx.getCondition().getNumber() % 2 == 0) {
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
        traceHandler("a", (ctx, ops) -> {
          warnings.add(ctx.getCondition().getMessage());
          return ops.restart(Restarts.resume());
        }));

      mid(n);

      return warnings;
    }
  }

  public void noHandler(PrintStream out) {
    try (Scope scope = Scopes.create()) {
      scope.signal(new IntWarning(-1), new Policies<>(new WarningPolicy(out), ReturnTypePolicy.ignore()));
      scope.signal(new IntWarning(-2), new Policies<>(new WarningPolicy(out), ReturnTypePolicy.ignore()));
    }
  }
}
