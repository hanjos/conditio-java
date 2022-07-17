package org.sbrubbles.conditio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sbrubbles.conditio.fixtures.BasicCondition;
import org.sbrubbles.conditio.fixtures.UseValue;

import static org.junit.jupiter.api.Assertions.*;

public class HandlerImplTest {
  private Handler h;

  @BeforeEach
  public void setUp() {
    try (Scope a = Scope.create()) {
      h = new HandlerImpl(BasicCondition.class, this::body, a);
    }
  }

  @Test
  public void nullParametersAreNotAllowed() {
    try(Scope scope = Scope.create()) {
      assertThrows(NullPointerException.class, () -> new HandlerImpl(null, this::body, scope), "missing conditionType");
      assertThrows(NullPointerException.class, () -> new HandlerImpl(BasicCondition.class, null, scope), "missing body");
      assertThrows(NullPointerException.class, () -> new HandlerImpl(BasicCondition.class, this::body, null), "missing scope");
      assertThrows(NullPointerException.class, () -> new HandlerImpl(null, null, scope), "missing both");
    }
  }

  @Test
  public void test() {
    try (Scope scope = Scope.create()) {
      assertTrue(h.test(new BasicCondition(scope, "string")));
      assertFalse(h.test(null));
    }
  }

  @Test
  public void apply() {
    try (Scope scope = Scope.create()) {
      Condition c = new BasicCondition(scope, "OMGWTFBBQ");
      assertEquals(
        new UseValue("OK: OMGWTFBBQ"),
        h.apply(c));

      Condition f = new BasicCondition(scope, "FAIL");
      assertEquals(
        new UseValue("FAIL!"),
        h.apply(f));
    }
  }

  private Restart.Option body(BasicCondition c) {
    if (!"FAIL".equals(c.getValue())) {
      return new UseValue("OK: " + c.getValue());
    } else {
      return new UseValue("FAIL!");
    }
  }
}
