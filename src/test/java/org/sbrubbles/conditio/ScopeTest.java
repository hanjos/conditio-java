package org.sbrubbles.conditio;

import org.junit.jupiter.api.Test;

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

        try(Scope c = Scope.create()) {
          assertFalse(c.isRoot());
          assertEquals(c.getParent(), b);
        }
      }
    }
  }
}
