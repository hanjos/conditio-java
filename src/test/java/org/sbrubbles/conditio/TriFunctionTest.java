package org.sbrubbles.conditio;

import org.junit.jupiter.api.Test;
import org.sbrubbles.conditio.util.TriFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TriFunctionTest {
  private TriFunction<Integer, String, Character, Boolean> fixture;

  @Test
  public void andThen() {
    fixture = (i, s, c) -> s.charAt(i) == c;

    assertEquals("true", fixture.andThen(String::valueOf).apply(0, "abc", 'a'));
    assertEquals("false", fixture.andThen(String::valueOf).apply(0, "abc", 'b'));
  }

  @Test
  public void andThenDoesntTakeNull() {
    fixture = (a, b, c) -> b.charAt(a) != c;

    assertThrows(NullPointerException.class, () -> fixture.andThen(null));
  }
}
