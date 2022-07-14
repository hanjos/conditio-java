package org.sbrubbles.conditio;

import org.junit.jupiter.api.Test;
import org.sbrubbles.conditio.restarts.RetryWith;
import org.sbrubbles.conditio.restarts.UseValue;

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
