package org.sbrubbles.conditio;

import org.junit.jupiter.api.Test;
import org.sbrubbles.conditio.fixtures.aborting.AbortingFixture;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

public class AbortTest {
  @Test
  public void testRun() {
    AbortingFixture fixture = new AbortingFixture();

    assertEquals(AbortingFixture.HANDLE, fixture.handle());
    assertLinesMatch(
      Arrays.asList("signal: AbortException", "passThrough: AbortException", "handle: AbortException"),
      fixture.getTrace());
  }
}
