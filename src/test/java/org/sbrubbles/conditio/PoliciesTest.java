package org.sbrubbles.conditio;

import org.junit.jupiter.api.Test;
import org.sbrubbles.conditio.policies.Policies;
import org.sbrubbles.conditio.policies.ReturnTypePolicy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class PoliciesTest {
  @Test
  public void defaultPolicyForReturnTypeIsToReturnNull() {
    Policies<?> policies = new Policies<>();
    assertNull(policies.getExpectedType());
  }

  @Test
  public void settingExpectedReturnType() {
    Policies<String> pWithString = new Policies<>(ReturnTypePolicy.expects(String.class));
    assertEquals(String.class, pWithString.getExpectedType());
  }
}
