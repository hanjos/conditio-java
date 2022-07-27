package org.sbrubbles.conditio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sbrubbles.conditio.fixtures.skipping.SkipHandler;
import org.sbrubbles.conditio.fixtures.skipping.SkippingFixture;
import org.sbrubbles.conditio.fixtures.skipping.SonOfSkipHandler;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class SkippingTest {
  private SkippingFixture fixture;

  @BeforeEach
  public void setUp() {
    fixture = new SkippingFixture();
  }

  @ParameterizedTest
  @MethodSource("skipHandlingProvider")
  public void skipHandling(final Class<? extends Condition> conditionType, final Function<String, Condition> conditionBuilder) throws Exception {
    final String INPUT = "OMG";

    String actual = fixture.a(conditionBuilder.apply(INPUT));

    assertEquals(SkippingFixture.EXPECTED_RESULT, actual);
    assertLinesMatch(
      Arrays.asList(
        "b: " + conditionType.getSimpleName(),
        "a: " + conditionType.getSimpleName()),
      fixture.getHandlerTrace());
    assertEquals(Collections.emptyList(), fixture.getRestartTrace());
  }

  static Stream<Arguments> skipHandlingProvider() {
    return Stream.of(
      arguments(SkipHandler.class, (Function<String, Condition>) SkipHandler::new),
      arguments(SonOfSkipHandler.class, (Function<String, Condition>) SonOfSkipHandler::new)
    );
  }
}
