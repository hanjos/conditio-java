package org.sbrubbles.conditio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sbrubbles.conditio.fixtures.BasicCondition;

import static org.junit.jupiter.api.Assertions.*;

public class HandlerImplTest {
  private Handler h;

  @BeforeEach
  public void setUp() {
    h = new HandlerImpl(BasicCondition.class, this::body);
  }

  @Test
  public void nullParametersAreNotAllowed() {
    assertThrows(NullPointerException.class, () -> new HandlerImpl(null, this::body), "missing conditionType");
    assertThrows(NullPointerException.class, () -> new HandlerImpl(BasicCondition.class, null), "missing body");
    assertThrows(NullPointerException.class, () -> new HandlerImpl(null, null), "missing both");
  }

  @Test
  public void test() {
    assertTrue(h.test(new BasicCondition("string")));
    assertFalse(h.test(null));
  }

  @Test
  public void apply() {
    try (Scope scope = Scopes.create()) {
      Condition c = new BasicCondition("OMGWTFBBQ");
      final Handler.Context<Condition> ctxC = new HandlerContextImpl<>(c, scope);

      assertEquals("OK: OMGWTFBBQ", h.apply(ctxC).get());

      Condition f = new BasicCondition("FAIL");
      final Handler.Context<Condition> ctxF = new HandlerContextImpl<>(f, scope);

      assertEquals("FAIL!", h.apply(ctxF).get());
    }
  }

  @Test
  public void getters() {
    try (Scope a = Scopes.create()) {
      Condition c = new BasicCondition("OMGWTFBBQ");
      final Handler.Context<Condition> ctx = new HandlerContextImpl<>(c, a);

      assertEquals(c, ctx.getCondition());
      assertEquals(a, ctx.getScope());
    }
  }

  private Handler.Decision body(Handler.Context<BasicCondition> ctx) {
    BasicCondition c = ctx.getCondition();

    if (!"FAIL".equals(c.getValue())) {
      return new Handler.Decision("OK: " + c.getValue());
    } else {
      return new Handler.Decision("FAIL!");
    }
  }
}
