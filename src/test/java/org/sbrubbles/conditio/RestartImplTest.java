package org.sbrubbles.conditio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sbrubbles.conditio.restarts.UseValue;

import static org.junit.jupiter.api.Assertions.*;

public class RestartImplTest {
  private Restart rA;
  private Restart rB;

  @BeforeEach
  public void setUp() {
    try (Scope scope = Scope.create()) {
      rA = new RestartImpl(A.class, this::body, scope);
      rB = new RestartImpl(B.class, this::body, scope);
    }
  }

  @Test
  public void nullParametersAreNotAllowed() {
    try (Scope scope = Scope.create()) {
      assertThrows(NullPointerException.class,
        () -> new RestartImpl(null, this::body, scope), "missing optionType");
      assertThrows(NullPointerException.class,
        () -> new RestartImpl(A.class, null, scope), "missing body");
      assertThrows(NullPointerException.class,
        () -> new RestartImpl(A.class, this::body, null), "missing scope");
      assertThrows(NullPointerException.class,
        () -> new RestartImpl(null, null, scope), "missing both");
    }
  }

  @Test
  public void testForA() {
    assertTrue(rA.test(new A("string")));
    assertTrue(rA.test(new B("string")));

    assertFalse(rA.test(null));
    assertFalse(rA.test(new UseValue("nope")));
  }

  @Test
  public void testForB() {
    assertTrue(rB.test(new B("string")));

    assertFalse(rB.test(new A("string")));
    assertFalse(rB.test(null));
    assertFalse(rB.test(new UseValue("nope")));
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
      () -> { rB.apply(new A("OMGWTFBBQ")); });

    assertThrows(
      ClassCastException.class,
      () -> { rB.apply(new UseValue("OMGWTFBBQ")); });

    assertEquals(
      "OK: OMGWTFBBQ",
      rB.apply(new B("OMGWTFBBQ")));

    assertEquals(
      "FAIL!",
      rB.apply(new B("FAIL")));
  }

  private Object body(A a) {
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
