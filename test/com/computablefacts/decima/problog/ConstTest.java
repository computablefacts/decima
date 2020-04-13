package com.computablefacts.decima.problog;

import org.junit.Assert;
import org.junit.Test;

public class ConstTest {

  @Test
  public void testConstEquals() {

    Const a = new Const("a");
    Const b = new Const(1);

    Assert.assertEquals(a, a);
    Assert.assertEquals(b, b);

    Assert.assertNotEquals(a, b);
  }

  @Test
  public void testConstHashcode() {

    Const a = new Const("a");
    Const b = new Const(1);

    Assert.assertEquals(a.hashCode(), a.hashCode());
    Assert.assertEquals(b.hashCode(), b.hashCode());

    Assert.assertNotEquals(a.hashCode(), b.hashCode());
  }
}
