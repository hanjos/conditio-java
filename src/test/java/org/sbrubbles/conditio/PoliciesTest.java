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
      Handler.Context<Condition> ctx = new HandlerContextImpl<>(c, policies, scope);

      assertThrows(HandlerNotFoundException.class, () -> policies.onHandlerNotFound(ctx));
    }
  }

  @Test
  public void settingHandlerNotFoundPolicy() {
    try (Scope scope = Scopes.create()) {
      Condition c = new BasicCondition("");
      List<String> trail = new ArrayList<>();

      Policies<?> pWithNull = new Policies<>(null, null);
      assertThrows(HandlerNotFoundException.class, () -> pWithNull.onHandlerNotFound(new HandlerContextImpl<>(c, pWithNull, scope)));

      Policies<?> pWithIgnore = new Policies<>(
        trace(trail, "ignore", HandlerNotFoundPolicy.ignore()),
        null);
      pWithIgnore.onHandlerNotFound(new HandlerContextImpl<>(c, pWithIgnore, scope));

      Policies<?> pWithError = new Policies<>(
        trace(trail, "error", HandlerNotFoundPolicy.error()),
        null);
      assertThrows(HandlerNotFoundException.class, () -> pWithError.onHandlerNotFound(new HandlerContextImpl<>(c, pWithError, scope)));

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
    Policies<String> pWithString = new Policies<>(null, ReturnTypePolicy.expects(String.class));
    assertEquals(String.class, pWithString.getExpectedType());

    Policies<String> pWithNull = new Policies<>(null, null);
    assertNull(pWithNull.getExpectedType());
  }

  static <T> HandlerNotFoundPolicy<T> trace(List<String> trail, String message, HandlerNotFoundPolicy<T> policy) {
    return ctx -> {
      trail.add(message);

      return policy.onHandlerNotFound(ctx);
    };
  }
}
