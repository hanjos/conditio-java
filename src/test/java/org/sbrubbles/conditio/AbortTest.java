package org.sbrubbles.conditio;

import org.junit.jupiter.api.Test;
import org.sbrubbles.conditio.restarts.Abort;
import org.sbrubbles.conditio.restarts.AbortException;

import static org.junit.jupiter.api.Assertions.*;

public class AbortTest {
  @Test
  public void abortEqualsAnyOtherAbort() {
    Abort expected = new Abort();
    Abort actual = new Abort();

    assertEquals(expected, actual);
    assertEquals(expected.hashCode(), actual.hashCode());
  }

  @Test
  public void abortTestOnlyTakesAborts() {
    Abort a = new Abort();

    assertTrue(a.test(a));
    assertTrue(a.test(new Abort()));

    assertFalse(a.test(null));
    assertFalse(a.test(new Restart.Option() { }));
  }

  @Test
  public void abortApplyAlwaysAborts() {
    Abort a = new Abort();

    assertThrows(AbortException.class, () -> a.apply(a));
    assertThrows(AbortException.class, () -> a.apply(new Abort()));
    assertThrows(AbortException.class, () -> a.apply(null));
    assertThrows(AbortException.class, () -> a.apply(new Restart.Option() { }));
  }
}
