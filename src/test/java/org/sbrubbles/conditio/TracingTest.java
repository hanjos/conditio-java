package org.sbrubbles.conditio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sbrubbles.conditio.fixtures.tracing.TracingFixture;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TracingTest {
  private TracingFixture fixture;
  private static final int MAX_WORK = 10;

  @BeforeEach
  public void setUp() {
    fixture = new TracingFixture(MAX_WORK);
  }

  @Test
  public void resuming() {
    fixture.resume();

    assertEquals(MAX_WORK, fixture.getWorkDone());
    assertEquals(Collections.nCopies(MAX_WORK, "run: WorkDone"), fixture.getHandlerTrace());
    assertEquals(Collections.nCopies(MAX_WORK, 1), fixture.getWorkTrace());
  }
}
