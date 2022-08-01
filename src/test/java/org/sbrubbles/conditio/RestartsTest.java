package org.sbrubbles.conditio;

import org.junit.jupiter.api.Test;
import org.sbrubbles.conditio.restarts.Resume;

import static org.junit.jupiter.api.Assertions.*;

public class RestartsTest {
  @Test
  public void resumeEqualsAnyOtherResume() {
    Resume expected = new Resume();
    Resume actual = new Resume();

    assertEquals(expected, actual);
    assertEquals(expected.hashCode(), actual.hashCode());
  }

  @Test
  public void resumeTestOnlyTakesResumes() {
    Resume r = new Resume();

    assertTrue(r.test(r));
    assertTrue(r.test(new Resume()));

    assertFalse(r.test(null));
    assertFalse(r.test(new Restart.Option() { }));
  }

  @Test
  public void resumeApplyOnlyTakesResumes() {
    Resume r = new Resume();

    assertNull(r.apply(r));
    assertNull(r.apply(new Resume()));

    assertThrows(ClassCastException.class, () -> r.apply(null));
    assertThrows(ClassCastException.class, () -> r.apply(new Restart.Option() { }));
  }
}
