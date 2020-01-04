package com.computablefacts.decima.robdd;

import org.junit.Assert;
import org.junit.Test;

public class TestPair {

  @Test
  public void testEquals() {

    Pair<String, String> pair1 = new Pair<>("left", "right");
    Pair<String, String> pair2 = new Pair<>("left", "right");

    Assert.assertEquals(pair1, pair2);
    Assert.assertEquals(pair1.hashCode(), pair2.hashCode());
  }

  @Test
  public void testNotEquals() {

    Pair<String, String> pair1 = new Pair<>("left", "right");
    Pair<String, String> pair2 = new Pair<>("l", "r");

    Assert.assertNotEquals(pair1, pair2);
    Assert.assertNotEquals(pair1.hashCode(), pair2.hashCode());
  }

  @Test
  public void testEqualsNull() {

    Pair<String, String> pair = new Pair<>("left", "right");

    Assert.assertFalse(pair.equals(null));
  }

  @Test
  public void testEqualsTuple() {

    Pair<String, String> pair = new Pair<>("left", "right");
    Tuple<String, String, String> tuple = new Tuple<>("left", "right", null);

    Assert.assertNotEquals(pair, tuple);
  }
}
