package org.sbrubbles.conditio;

import org.junit.jupiter.api.Test;
import org.sbrubbles.conditio.fixtures.BasicCondition;
import org.sbrubbles.conditio.policies.Policies;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

public class ScopeTest {
  @Test
  public void rootIsItsOwnParent() {
    try (Scope scope = Scopes.create()) {
      assertNotNull(scope.getParent());

      assertSame(scope.getParent(), scope.getParent().getParent());
    }
  }

  @Test
  public void everyInvocationChainIsDifferentButHasTheSameRoot() {
    Scope root, first;

    try (Scope s1 = Scopes.create()) {
        root = s1.getParent();
        first = s1;
    }

    try (Scope s2 = Scopes.create()) {
      assertSame(root, s2.getParent());

      assertNotSame(first, s2);
    }
  }

  @Test
  public void createReflectsTheTryStack() {
    Scope root;
    try(Scope s = Scopes.create()) {
      root = s.getParent();
    }

    try (Scope a = Scopes.create()) {
      assertSame(root, a.getParent());

      try (Scope b = Scopes.create()) {
        assertSame(a, b.getParent());

        try (Scope c = Scopes.create()) {
          assertSame(b, c.getParent());
        }
      }
    }
  }

  @Test
  public void createRejectsNullRestarts() {
    assertThrows(NullPointerException.class, () -> Scopes.create((Restart<?>[]) null));
    assertThrows(NullPointerException.class, () -> Scopes.create((Restart<?>) null));
    assertThrows(NullPointerException.class, () -> Scopes.create(Restarts.resume(), null));
  }

  @Test
  public void createWithRestartsStoresThemOnlyDuringTheScopesLifetime() {
    try(Scope a = Scopes.create()) {
      assertIterableEquals(Collections.emptyList(), a.getAllRestarts());

      try(Scope b = Scopes.create(Restarts.resume())) {
        assertIterableEquals(Collections.emptyList(), a.getAllRestarts());
        assertIterableEquals(Collections.singletonList(Restarts.resume()), b.getAllRestarts());
      }

      assertIterableEquals(Collections.emptyList(), a.getAllRestarts());
    }
  }

  @Test
  public void theIteratorFollowsTheProperProtocol() {
    try (Scope a = Scopes.create()) {
      Iterator<Handler> iterator = a.getAllHandlers().iterator();

      assertFalse(iterator.hasNext());
      assertThrows(NoSuchElementException.class, iterator::next);
    }
  }

  @Test
  public void signalThrowsIfHandlerReturnsNull() {
    try (Scope a = Scopes.create()) {
      a.handle(BasicCondition.class, (s, ops) -> null);

      assertThrows(NullPointerException.class, () -> a.signal(new BasicCondition("oops"), new Policies<>()));
    }
  }
}
