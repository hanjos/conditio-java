package org.sbrubbles.conditio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RestartTest {
  private final Restart r = new Restart.Impl(String.class::isInstance, this::body);

  @Test
  public void nullParametersAreNotAllowed() {
    assertThrows(NullPointerException.class,
      () -> new Restart.Impl(null, this::body), "missing checker");
    assertThrows(NullPointerException.class,
      () -> new Restart.Impl(String.class::isInstance, null), "missing body");
    assertThrows(NullPointerException.class,
      () -> new Restart.Impl(null, null), "missing both");
  }

  @Test
  public void matches() {
    assertTrue(r.matches("string"));
    assertTrue(r.matches(""));

    assertFalse(r.matches(null));
    assertFalse(r.matches(new Object()));
    assertFalse(r.matches(1));
    assertFalse(r.matches('t'));
  }

  @Test
  public void run() {
    assertEquals(
      "OK: OMGWTFBBQ",
      r.run("OMGWTFBBQ"));

    assertEquals(
      "FAIL!",
      r.run("FAIL"));
  }

  private Object body(String s) {
    if (!"FAIL".equals(s)) {
      return "OK: " + s;
    } else {
      return "FAIL!";
    }
  }
}
