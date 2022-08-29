package org.sbrubbles.conditio;

import org.junit.jupiter.api.Test;
import org.sbrubbles.conditio.fixtures.BasicCondition;
import org.sbrubbles.conditio.policies.HandlerNotFoundPolicy;
import org.sbrubbles.conditio.policies.Policies;
import org.sbrubbles.conditio.policies.ReturnTypePolicy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PoliciesTest {
  @Test
  public void defaultPolicyForHandlerNotFoundIsToErrorOut() {
    Policies<?> policies = new Policies<>();

    try (Scope scope = Scopes.create()) {
      Condition c = new BasicCondition("");
      Context<Condition> ctx = new Context<>(c, policies, scope);

      assertThrows(HandlerNotFoundException.class, () -> policies.onHandlerNotFound(ctx));
    }
  }

  @Test
  public void settingHandlerNotFoundPolicy() {
    try (Scope scope = Scopes.create()) {
      Condition c = new BasicCondition("");
      List<String> trail = new ArrayList<>();

      Policies<?> pWithIgnore = new Policies<>(
        trace(trail, "ignore", HandlerNotFoundPolicy.ignore()),
        ReturnTypePolicy.ignore());
      pWithIgnore.onHandlerNotFound(new Context<>(c, pWithIgnore, scope));

      Policies<?> pWithError = new Policies<>(
        trace(trail, "error", HandlerNotFoundPolicy.error()),
        ReturnTypePolicy.ignore());
      assertThrows(HandlerNotFoundException.class, () -> pWithError.onHandlerNotFound(new Context<>(c, pWithError, scope)));

      assertLinesMatch(Arrays.asList("ignore", "error"), trail);
    }
  }

  @Test
  public void defaultPolicyForReturnTypeIsToReturnNull() {
    Policies<?> policies = new Policies<>();
    assertNull(policies.getExpectedType());
  }

  @Test
  public void settingExpectedReturnType() {
    Policies<String> pWithString = new Policies<>(HandlerNotFoundPolicy.error(), ReturnTypePolicy.expects(String.class));
    assertEquals(String.class, pWithString.getExpectedType());
  }

  static <T> HandlerNotFoundPolicy<T> trace(List<String> trail, String message, HandlerNotFoundPolicy<T> policy) {
    return ctx -> {
      trail.add(message);

      return policy.onHandlerNotFound(ctx);
    };
  }
}
