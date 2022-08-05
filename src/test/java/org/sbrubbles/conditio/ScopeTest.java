package org.sbrubbles.conditio;

import org.junit.jupiter.api.Test;
import org.sbrubbles.conditio.fixtures.BasicCondition;

import java.util.Iterator;
import java.util.NoSuchElementException;

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
  public void theIteratorFollowsTheProperProtocol() {
    try (Scope a = Scopes.create()) {
      Iterator<Handler> iterator = a.getAllHandlers().iterator();

      assertFalse(iterator.hasNext());
      assertThrows(NoSuchElementException.class, () -> iterator.next());
    }
  }

  @Test
  public void signalThrowsIfHandlerReturnsNull() {
    try (Scope a = Scopes.create()) {
      a.handle(BasicCondition.class, (c, ops) -> null);

      assertThrows(NullPointerException.class, () -> a.signal(new BasicCondition("oops")));
    }
  }
}
