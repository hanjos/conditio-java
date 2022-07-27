package org.sbrubbles.conditio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sbrubbles.conditio.fixtures.resignalling.PleaseSignalSomethingElse;
import org.sbrubbles.conditio.fixtures.resignalling.ResignallingFixture;
import org.sbrubbles.conditio.fixtures.resignalling.SomethingElse;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

public class ResignallingTest {
  private ResignallingFixture fixture;

  @BeforeEach
  public void setUp() {
    fixture = new ResignallingFixture();
  }

  @Test
  public void resignalling() {
    String actual = fixture.aCallsB();

    assertEquals(ResignallingFixture.FIXED_RESULT, actual);
    assertLinesMatch(
      Arrays.asList(
        "aCallsB: " + PleaseSignalSomethingElse.class.getSimpleName(),
        "bCallsC: " + SomethingElse.class.getSimpleName()),
      fixture.getHandlerTrace());
    assertEquals(Collections.emptyList(), fixture.getRestartTrace());
  }
}
