package org.sbrubbles.conditio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sbrubbles.conditio.fixtures.BasicCondition;
import org.sbrubbles.conditio.restarts.UseValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class RestartTest {
  private Restart<String> rA;
  private Restart<String> rB;

  @BeforeEach
  public void setUp() {
    rA = Restarts.on(A.class, this::body);
    rB = Restarts.on(B.class, this::body);
  }

  @Test
  public void nullParametersAreNotAllowed() {
    assertThrows(NullPointerException.class,
        () -> Restarts.on(null, this::body), "missing optionType");
    assertThrows(NullPointerException.class,
        () -> Restarts.on(A.class, null), "missing body");
    assertThrows(NullPointerException.class,
        () -> Restarts.on(null, null), "missing both");
  }

  @Test
  public void testForA() {
    assertTrue(rA.test(new A("string")));
    assertTrue(rA.test(new B("string")));

    assertFalse(rA.test(null));
    assertFalse(rA.test(new UseValue<>("nope")));
  }

  @Test
  public void testForB() {
    assertTrue(rB.test(new B("string")));

    assertFalse(rB.test(new A("string")));
    assertFalse(rB.test(null));
    assertFalse(rB.test(new UseValue<>("nope")));
  }

  @Test
  public void apply() {
    assertEquals(
        "OK: OMGWTFBBQ",
        rA.apply(new A("OMGWTFBBQ")));

    assertEquals(
        "OK: OMGWTFBBQ",
        rA.apply(new B("OMGWTFBBQ")));

    assertEquals(
        "FAIL!",
        rA.apply(new A("FAIL")));

    assertEquals(
        "FAIL!",
        rA.apply(new B("FAIL")));
  }

  @Test
  public void applyForB() {
    assertThrows(
        ClassCastException.class,
        () -> rB.apply(new A("OMGWTFBBQ")));

    assertThrows(
        ClassCastException.class,
        () -> rB.apply(new UseValue<>("OMGWTFBBQ")));

    assertEquals(
        "OK: OMGWTFBBQ",
        rB.apply(new B("OMGWTFBBQ")));

    assertEquals(
        "FAIL!",
        rB.apply(new B("FAIL")));
  }

  @Test
  public void restartNotFoundExceptionIsThrownByDefault() {
    try (Scope a = Scopes.create()) {
      a.handle(BasicCondition.class, (s, ops) -> ops.restart(new UseValue<>(1)));

      a.signal(new BasicCondition(""), Optional.empty());
    } catch (RestartNotFoundException r) {
      assertEquals(new UseValue<>(1), r.getOption());

      return;
    }

    fail("Should've thrown a RestartNotFoundException earlier!");
  }

  @Test
  public void restartNotFoundIsSignalledWhenThereIsNoRestart() {
    List<String> trail = new ArrayList<>();

    try (Scope a = Scopes.create()) {
      a.handle(BasicCondition.class, (s, ops) -> {
        trail.add("BasicCondition");

        return ops.restart(new UseValue<>(1));
      });
      a.handle(RestartNotFound.class, (s, ops) -> {
        trail.add("RestartNotFound");

        return ops.abort();
      });

      a.signal(new BasicCondition(""), Optional.empty());
    } catch (AbortException a) {
      assertLinesMatch(Arrays.asList("BasicCondition", "RestartNotFound"), trail);

      return;
    }

    fail("Should've thrown an AbortException earlier!");
  }

  private String body(A a) {
    if (!"FAIL".equals(a.getValue())) {
      return "OK: " + a.getValue();
    } else {
      return "FAIL!";
    }
  }

  static class A implements Restart.Option {
    private final Object value;

    public A(Object value) {
      this.value = value;
    }

    public Object getValue() {
      return value;
    }
  }

  static class B extends A {
    public B(Object value) {
      super(value);
    }
  }
}
