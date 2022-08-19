package org.sbrubbles.conditio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sbrubbles.conditio.fixtures.BasicCondition;
import org.sbrubbles.conditio.policies.HandlerNotFoundPolicy;
import org.sbrubbles.conditio.policies.Policies;
import org.sbrubbles.conditio.policies.ReturnTypePolicy;

import static org.junit.jupiter.api.Assertions.*;

public class PoliciesTest {
  private Policies<?> policies;

  @BeforeEach
  public void setUp() {
    policies = new Policies<>();
  }

  @Test
  public void defaultPolicyForHandlerNotFoundIsToErrorOut() {
    try (Scope scope = Scopes.create()) {
      assertThrows(HandlerNotFoundException.class, () -> policies.onHandlerNotFound(new BasicCondition(""), scope));
    }
  }

  @Test
  public void settingHandlerNotFoundPolicy() {
    try (Scope scope = Scopes.create()) {
      policies.set((HandlerNotFoundPolicy) null);

      // we should error out
      assertThrows(HandlerNotFoundException.class, () -> policies.onHandlerNotFound(new BasicCondition(""), scope));

      policies.set(HandlerNotFoundPolicy.IGNORE);

      // nothing should happen
      policies.onHandlerNotFound(new BasicCondition(""), scope);

      policies.set(HandlerNotFoundPolicy.ERROR);

      // we should error out
      assertThrows(HandlerNotFoundException.class, () -> policies.onHandlerNotFound(new BasicCondition(""), scope));
    }
  }

  @Test
  public void defaultPolicyForReturnTypeIsToReturnNull() {
    assertNull(policies.getExpectedType());
  }

  @Test
  public void settingExpectedReturnType() {
    Policies<String> p = new Policies<>();

    p.set(ReturnTypePolicy.expects(String.class));

    assertEquals(String.class, p.getExpectedType());

    p.set((ReturnTypePolicy<String>) null);

    assertNull(p.getExpectedType());
  }
}
