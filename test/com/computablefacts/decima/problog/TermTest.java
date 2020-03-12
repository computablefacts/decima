package com.computablefacts.decima.problog;

import org.junit.Assert;
import org.junit.Test;

public class TermTest {

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

  @Test
  public void testVarEquals() {

    Var a = new Var();
    Var b = new Var();

    Assert.assertEquals(a, a);
    Assert.assertEquals(b, b);

    Assert.assertNotEquals(a, b);
  }

  @Test
  public void testVarHashcode() {

    Var a = new Var();
    Var b = new Var();

    Assert.assertEquals(a.hashCode(), a.hashCode());
    Assert.assertEquals(b.hashCode(), b.hashCode());

    Assert.assertNotEquals(a.hashCode(), b.hashCode());
  }

  @Test
  public void testWildcardEquals() {

    Var a = new Var(true);
    Var b = new Var(true);

    Assert.assertEquals(a, a);
    Assert.assertEquals(b, b);

    Assert.assertNotEquals(a, b);
  }

  @Test
  public void testWildcardHashcode() {

    Var a = new Var(true);
    Var b = new Var(true);

    Assert.assertEquals(a.hashCode(), a.hashCode());
    Assert.assertEquals(b.hashCode(), b.hashCode());

    Assert.assertNotEquals(a.hashCode(), b.hashCode());
  }
}
