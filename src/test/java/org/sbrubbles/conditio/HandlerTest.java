package org.sbrubbles.conditio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sbrubbles.conditio.fixtures.BasicCondition;
import org.sbrubbles.conditio.restarts.UseValue;

import static org.junit.jupiter.api.Assertions.*;

public class HandlerTest {
  private Handler h;

  @BeforeEach
  public void setUp() {
    try(Scope a = Scope.create()) {
      h = new Handler.Impl(BasicCondition.class, this::body, a);
    }
  }

  @Test
  public void nullParametersAreNotAllowed() {
    try(Scope scope = Scope.create()) {
      assertThrows(NullPointerException.class, () -> new Handler.Impl(null, this::body, scope), "missing signalType");
      assertThrows(NullPointerException.class, () -> new Handler.Impl(BasicCondition.class, null, scope), "missing body");
      assertThrows(NullPointerException.class, () -> new Handler.Impl(BasicCondition.class, this::body, null), "missing scope");
      assertThrows(NullPointerException.class, () -> new Handler.Impl(null, null, scope), "missing both");
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
      assertEquals(
        new UseValue("OK: OMGWTFBBQ"),
        h.apply(c));

      Condition f = new BasicCondition("FAIL");
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
