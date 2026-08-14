package org.sbrubbles.conditio;

import org.junit.jupiter.api.Test;
import org.sbrubbles.conditio.fixtures.BasicCondition;
import org.sbrubbles.conditio.fixtures.SonOfBasicCondition;
import org.sbrubbles.conditio.policies.Policies;
import org.sbrubbles.conditio.restarts.UseValue;

import java.util.Collections;

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
  public void shouldFailOnClosedScopes() {
    final Scope closed = getClosedScope();

    assertThrows(UnsupportedOperationException.class, closed::isRoot);
    assertThrows(UnsupportedOperationException.class, closed::getParent);
    assertThrows(UnsupportedOperationException.class, closed::getAllHandlers);
    assertThrows(UnsupportedOperationException.class, closed::getAllRestarts);
    assertThrows(UnsupportedOperationException.class, closed::getParent);
    assertThrows(UnsupportedOperationException.class, () -> closed.call(() -> ""));
    assertThrows(UnsupportedOperationException.class, () -> closed.raise(new BasicCondition(""), String.class));
    assertThrows(UnsupportedOperationException.class, () -> closed.notify(new BasicCondition("")));
    assertThrows(UnsupportedOperationException.class, () -> closed.signal(new BasicCondition(""), new Policies<>()));
    assertThrows(UnsupportedOperationException.class, () -> closed.handle(null));
    assertThrows(UnsupportedOperationException.class, () -> closed.handle(BasicCondition.class, (s, ops) -> ops.skip()));
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
    try (Scope s = Scopes.create()) {
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
    try (Scope a = Scopes.create()) {
      assertIterableEquals(Collections.emptyList(), a.getAllRestarts());

      try (Scope b = Scopes.create(Restarts.resume())) {
        assertIterableEquals(Collections.emptyList(), a.getAllRestarts());
        assertIterableEquals(Collections.singletonList(Restarts.resume()), b.getAllRestarts());
      }

      assertIterableEquals(Collections.emptyList(), a.getAllRestarts());
    }
  }

  @Test
  public void signalThrowsIfHandlerReturnsNull() {
    try (Scope a = Scopes.create()) {
      a.handle(BasicCondition.class, (s, ops) -> null);

      assertThrows(NullPointerException.class, () -> a.signal(new BasicCondition("oops"), new Policies<>()));
    }
  }

  @Test
  public void notifyHandlesHandlerNotFoundByResuming() {
    try (Scope a = Scopes.create()) {
      // signal throws...
      assertThrows(HandlerNotFoundException.class, () -> a.signal(new BasicCondition(""), new Policies<>()));

      // notify doesn't
      a.notify(new BasicCondition(""));
    }
  }

  @Test
  public void notifyDoesntSwallowUnrelatedHandlerNotFounds() {
    Condition c = new SonOfBasicCondition("");

    try (Scope a = Scopes.create()) {
      a.handle(SonOfBasicCondition.class, (s, ops) -> {
        // a different, unhandled condition
        Scope scope = s.getScope();
        return ops.restart(new UseValue<>(scope.raise(new BasicCondition(""), String.class)));
      });

      assertThrows(HandlerNotFoundException.class, () -> a.notify(c));
    }
  }

  private Scope getClosedScope() {
    try (Scope a = Scopes.create()) {
      return a;
    }
  }
}
