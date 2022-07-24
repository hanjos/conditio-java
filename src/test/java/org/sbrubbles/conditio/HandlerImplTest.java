package org.sbrubbles.conditio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sbrubbles.conditio.fixtures.BasicCondition;

import static org.junit.jupiter.api.Assertions.*;

public class HandlerImplTest {
  private Handler h;

  @BeforeEach
  public void setUp() {
    try (Scope a = Scope.create()) {
      h = new HandlerImpl<>(BasicCondition.class, this::body);
    }
  }

  @Test
  public void nullParametersAreNotAllowed() {
    try(Scope scope = Scope.create()) {
      assertThrows(NullPointerException.class, () -> new HandlerImpl<>(null, this::body), "missing conditionType");
      assertThrows(NullPointerException.class, () -> new HandlerImpl<>(BasicCondition.class, null), "missing body");
      assertThrows(NullPointerException.class, () -> new HandlerImpl<>(null, null), "missing both");
    }
  }

  @Test
  public void test() {
    try (Scope scope = Scope.create()) {
      assertTrue(h.test(new BasicCondition("string")));
      assertFalse(h.test(null));
    }
  }

  @Test
  public void apply() {
    try (Scope scope = Scope.create()) {
      Condition c = new BasicCondition("OMGWTFBBQ");
      assertEquals("OK: OMGWTFBBQ", h.apply(c, scope));

      Condition f = new BasicCondition("FAIL");
      assertEquals("FAIL!", h.apply(f, scope));
    }
  }

  private String body(BasicCondition c, Scope s) {
    if (!"FAIL".equals(c.getValue())) {
      return "OK: " + c.getValue();
    } else {
      return "FAIL!";
    }
  }
}
