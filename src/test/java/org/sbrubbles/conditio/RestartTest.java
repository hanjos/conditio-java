package org.sbrubbles.conditio;

import org.junit.jupiter.api.Test;
import org.sbrubbles.conditio.fixtures.RetryWith;
import org.sbrubbles.conditio.fixtures.UseValue;

import static org.junit.jupiter.api.Assertions.*;

public class RestartTest {
  private final Restart r = new Restart.Impl(UseValue.class, this::body);

  @Test
  public void nullParametersAreNotAllowed() {
    assertThrows(NullPointerException.class,
      () -> new Restart.Impl(null, this::body), "missing checker");
    assertThrows(NullPointerException.class,
      () -> new Restart.Impl(UseValue.class, null), "missing body");
    assertThrows(NullPointerException.class,
      () -> new Restart.Impl(null, null), "missing both");
  }

  @Test
  public void matches() {
    assertTrue(r.matches(new UseValue("string")));

    assertFalse(r.matches(null));
    assertFalse(r.matches(new RetryWith("nope")));
  }

  @Test
  public void run() {
    assertEquals(
      "OK: OMGWTFBBQ",
      r.run(new UseValue("OMGWTFBBQ")));

    assertEquals(
      "FAIL!",
      r.run(new UseValue("FAIL")));
  }

  private Object body(UseValue u) {
    if (!"FAIL".equals(u.getValue())) {
      return "OK: " + u.getValue();
    } else {
      return "FAIL!";
    }
  }
}
