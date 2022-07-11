package org.sbrubbles.conditio;

import org.junit.jupiter.api.Test;

import java.util.function.Function;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

public class HandlerTest {
  private final Predicate<?> checker = String.class::isInstance;
  private final Function<Condition, Object> body = c -> {
    if (!"FAIL".equals(c.getSignal())) {
      return "OK: " + c.getSignal();
    } else {
      return "FAIL!";
    }
  };

  private final Handler h = new Handler.Impl(checker, body);

  @Test
  public void nullParametersAreNotAllowed() {
    assertThrows(NullPointerException.class, () -> new Handler.Impl(null, body), "missing checker");
    assertThrows(NullPointerException.class, () -> new Handler.Impl(checker, null), "missing body");
    assertThrows(NullPointerException.class, () -> new Handler.Impl(null, null), "missing both");
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
        "OK: OMGWTFBBQ",
        h.handle(c));

      Condition f = new Condition("FAIL", scope);
      assertEquals(
        "FAIL!",
        h.handle(f));
    }
  }
}
