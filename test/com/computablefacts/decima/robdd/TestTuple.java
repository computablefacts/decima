package com.computablefacts.decima.robdd;

import org.junit.Assert;
import org.junit.Test;

public class TestTuple {

  @Test
  public void testEquals() {

    Tuple<String, String, String> tuple1 = new Tuple<>("left", "middle", "right");
    Tuple<String, String, String> tuple2 = new Tuple<>("left", "middle", "right");

    Assert.assertEquals(tuple1, tuple2);
    Assert.assertEquals(tuple1.hashCode(), tuple2.hashCode());
  }

  @Test
  public void testNotEquals() {

    Tuple<String, String, String> tuple1 = new Tuple<>("left", "middle", "right");
    Tuple<String, String, String> tuple2 = new Tuple<>("left", "m", "right");

    Assert.assertNotEquals(tuple1, tuple2);
    Assert.assertNotEquals(tuple1.hashCode(), tuple2.hashCode());
  }

  @Test
  public void testEqualsNull() {

    Tuple<String, String, String> tuple = new Tuple<>("left", "middle", "right");

    Assert.assertFalse(tuple.equals(null));
  }

  @Test
  public void testEqualsPair() {

    Tuple<String, String, String> tuple = new Tuple<>("left", "right", null);
    Pair<String, String> pair = new Pair<>("left", "right");

    Assert.assertNotEquals(tuple, pair);
  }
}
