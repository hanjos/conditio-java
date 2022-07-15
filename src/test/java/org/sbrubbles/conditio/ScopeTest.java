package org.sbrubbles.conditio;

import org.junit.jupiter.api.Test;
import org.sbrubbles.conditio.fixtures.BasicCondition;
import org.sbrubbles.conditio.restarts.UseValue;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ScopeTest {
  @Test
  public void rootHasNullParent() {
    try(Scope scope = Scope.create()) {
      assertTrue(scope.isRoot());
      assertNull(scope.getParent());
    }
  }

  @Test
  public void everyInvocationChainHasItsOwnRoot() {
    Scope firstTry;

    try(Scope scope = Scope.create()) {
      assertTrue(scope.isRoot());

      firstTry = scope;
    }

    try(Scope scope = Scope.create()) {
      assertTrue(scope.isRoot());
      assertNotEquals(firstTry, scope);
    }
  }

  @Test
  public void createReflectsTheTryStack() {
    try(Scope a = Scope.create()) {
      assertTrue(a.isRoot());

      try(Scope b = Scope.create()) {
        assertFalse(b.isRoot());
        assertEquals(b.getParent(), a);

        try (Scope c = Scope.create()) {
          assertFalse(c.isRoot());
          assertEquals(c.getParent(), b);
        }
      }
    }
  }

  @Test
  public void getHandlersIsUnmodifiable() {
    try (Scope a = Scope.create()) {
      final List<Handler> hs = a.getHandlers();
      final Handler h = new Handler.Impl(BasicCondition.class, c -> new UseValue("test"), a);

      assertThrows(UnsupportedOperationException.class, () -> hs.add(h));
      assertThrows(UnsupportedOperationException.class, () -> hs.remove(h));
    }
  }

  @Test
  public void getRestartsIsUnmodifiable() {
    try (Scope a = Scope.create()) {
      final List<Restart> rs = a.getRestarts();
      final Restart r = new Restart.Impl(UseValue.class, u -> "test", a);

      assertThrows(UnsupportedOperationException.class, () -> rs.add(r));
      assertThrows(UnsupportedOperationException.class, () -> rs.remove(r));
    }
  }
}
