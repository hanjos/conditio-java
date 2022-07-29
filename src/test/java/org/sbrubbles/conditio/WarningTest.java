package org.sbrubbles.conditio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sbrubbles.conditio.fixtures.warning.WarningFixture;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

public class WarningTest {
  private WarningFixture fixture;

  @BeforeEach
  public void setUp() {
    fixture = new WarningFixture();
  }

  @Test
  public void test() {
    List<Integer> actual = fixture.a(10);

    // even numbers were muzzled by b
    assertEquals(Arrays.asList(1, 3, 5, 7, 9), actual);
    assertLinesMatch(
      Arrays.asList("b: Warning", "b: Warning", "a: Warning",
        "b: Warning", "b: Warning", "a: Warning",
        "b: Warning", "b: Warning", "a: Warning",
        "b: Warning", "b: Warning", "a: Warning",
        "b: Warning", "b: Warning", "a: Warning"),
      fixture.getHandlerTrace());
  }
}
