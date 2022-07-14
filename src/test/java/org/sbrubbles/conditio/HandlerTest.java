package org.sbrubbles.conditio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sbrubbles.conditio.restarts.UseValue;

import static org.junit.jupiter.api.Assertions.*;

public class HandlerTest {
  private Handler h;

  @BeforeEach
  public void setUp() {
    try(Scope a = Scope.create()) {
      h = new Handler.Impl(String.class, this::body, a);
    }
  }

  @Test
  public void nullParametersAreNotAllowed() {
    try(Scope scope = Scope.create()) {
      assertThrows(NullPointerException.class, () -> new Handler.Impl(null, this::body, scope), "missing checker");
      assertThrows(NullPointerException.class, () -> new Handler.Impl(String.class, null, scope), "missing body");
      assertThrows(NullPointerException.class, () -> new Handler.Impl(null, null, scope), "missing both");
    }
  }

  @Test
  public void accepts() {
    assertTrue(h.accepts("string"));
    assertTrue(h.accepts(""));

    assertFalse(h.accepts(null));
    assertFalse(h.accepts(new Object()));
    assertFalse(h.accepts(1));
    assertFalse(h.accepts('t'));
  }

  @Test
  public void handle() {
    try (Scope scope = Scope.create()) {
      Condition c = new Condition("OMGWTFBBQ", scope);
      assertEquals(
        new UseValue("OK: OMGWTFBBQ"),
        h.handle(c));

      Condition f = new Condition("FAIL", scope);
      assertEquals(
        new UseValue("FAIL!"),
        h.handle(f));
    }
  }

  private Restart.Option body(Condition c) {
    if (!"FAIL".equals(c.getSignal())) {
      return new UseValue( "OK: " + c.getSignal());
    } else {
      return new UseValue("FAIL!");
    }
  }
}
