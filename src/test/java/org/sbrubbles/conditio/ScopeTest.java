package org.sbrubbles.conditio;

import org.junit.jupiter.api.Test;
import org.sbrubbles.conditio.fixtures.BasicCondition;
import org.sbrubbles.conditio.fixtures.logging.UseValue;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ScopeTest {
  @Test
  public void rootHasNullParent() {
    try (Scope scope = Scopes.create()) {
      assertNull(scope.getParent());
    }
  }

  @Test
  public void everyInvocationChainHasItsOwnRoot() {
    Scope firstTry;

    try (Scope scope = Scopes.create()) {
      assertNull(scope.getParent());

      firstTry = scope;
    }

    try (Scope scope = Scopes.create()) {
      assertNull(scope.getParent());
      assertNotEquals(firstTry, scope);
    }
  }

  @Test
  public void createReflectsTheTryStack() {
    try (Scope a = Scopes.create()) {
      assertNull(a.getParent());

      try (Scope b = Scopes.create()) {
        assertEquals(b.getParent(), a);

        try (Scope c = Scopes.create()) {
          assertEquals(c.getParent(), b);
        }
      }
    }
  }

  @Test
  public void getHandlersIsUnmodifiable() {
    try (Scope a = Scopes.create()) {
      final List<Handler> hs = a.getHandlers();
      final Handler h = new HandlerImpl(BasicCondition.class, (c, ops) -> ops.use("test"));

      assertThrows(UnsupportedOperationException.class, () -> hs.add(h));
      assertThrows(UnsupportedOperationException.class, () -> hs.remove(h));
    }
  }

  @Test
  public void getRestartsIsUnmodifiable() {
    try (Scope a = Scopes.create()) {
      final List<Restart> rs = a.getRestarts();
      final Restart r = new RestartImpl(UseValue.class, u -> "test");

      assertThrows(UnsupportedOperationException.class, () -> rs.add(r));
      assertThrows(UnsupportedOperationException.class, () -> rs.remove(r));
    }
  }
}
