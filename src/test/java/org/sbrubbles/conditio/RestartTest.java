package org.sbrubbles.conditio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sbrubbles.conditio.restarts.RetryWith;
import org.sbrubbles.conditio.restarts.UseValue;

import static org.junit.jupiter.api.Assertions.*;

public class RestartTest {
  private Restart r;

  @BeforeEach
  public void setUp() {
    try (Scope a = Scope.create()) {
      r = new Restart.Impl(UseValue.class, this::body, a);
    }
  }

  @Test
  public void nullParametersAreNotAllowed() {
    try (Scope a = Scope.create()) {
      assertThrows(NullPointerException.class,
        () -> new Restart.Impl(null, this::body, a), "missing optionType");
      assertThrows(NullPointerException.class,
        () -> new Restart.Impl(UseValue.class, null, a), "missing body");
      assertThrows(NullPointerException.class,
        () -> new Restart.Impl(UseValue.class, this::body, null), "missing scope");
      assertThrows(NullPointerException.class,
        () -> new Restart.Impl(null, null, a), "missing both");
    }
  }

  @Test
  public void matches() {
    assertTrue(r.test(new UseValue("string")));

    assertFalse(r.test(null));
    assertFalse(r.test(new RetryWith("nope")));
  }

  @Test
  public void run() {
    assertEquals(
      "OK: OMGWTFBBQ",
      r.apply(new UseValue("OMGWTFBBQ")));

    assertEquals(
      "FAIL!",
      r.apply(new UseValue("FAIL")));
  }

  private Object body(UseValue u) {
    if (!"FAIL".equals(u.getValue())) {
      return "OK: " + u.getValue();
    } else {
      return "FAIL!";
    }
  }
}
