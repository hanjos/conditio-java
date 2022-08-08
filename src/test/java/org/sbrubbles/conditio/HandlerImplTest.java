package org.sbrubbles.conditio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sbrubbles.conditio.fixtures.BasicCondition;

import static org.junit.jupiter.api.Assertions.*;

public class HandlerImplTest {
  private Handler<String> h;

  @BeforeEach
  public void setUp() {
    h = new HandlerImpl<>(BasicCondition.class, this::body);
  }

  @Test
  public void nullParametersAreNotAllowed() {
    assertThrows(NullPointerException.class, () -> new HandlerImpl<>(null, this::body), "missing conditionType");
    assertThrows(NullPointerException.class, () -> new HandlerImpl<>(BasicCondition.class, null), "missing body");
    assertThrows(NullPointerException.class, () -> new HandlerImpl<>(null, null), "missing both");
  }

  @Test
  public void test() {
    assertTrue(h.test(new BasicCondition("string")));
    assertFalse(h.test(null));
  }

  @Test
  public void apply() {
    try (Scope scope = Scopes.create()) {
      Handler.Operations<String> ops = new HandlerOperationsImpl<>(scope, String.class);

      Condition<String> c = new BasicCondition("OMGWTFBBQ");
      assertEquals("OK: OMGWTFBBQ", h.apply(c, ops).get());

      Condition<String> f = new BasicCondition("FAIL");
      assertEquals("FAIL!", h.apply(f, ops).get());
    }
  }

  private Handler.Decision<String> body(BasicCondition c, Handler.Operations<String> ops) {
    if (!"FAIL".equals(c.getValue())) {
      return ops.use("OK: " + c.getValue());
    } else {
      return ops.use("FAIL!");
    }
  }
}
