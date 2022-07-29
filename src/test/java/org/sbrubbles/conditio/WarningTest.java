package org.sbrubbles.conditio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sbrubbles.conditio.fixtures.warning.WarningFixture;

import java.io.PrintStream;
import java.util.ArrayList;
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
  public void warningInterception() {
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

  @Test
  public void noHandler() {
    List<String> warnings = new ArrayList<>();

    fixture.noHandler(new PrintStream(System.out, true) {
      @Override
      public void println(String x) {
        warnings.add(x);
      }
    });

    assertEquals(Arrays.asList("Warning: -1", "Warning: -2"), warnings);
  }
}
