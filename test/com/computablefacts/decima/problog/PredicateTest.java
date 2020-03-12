package com.computablefacts.decima.problog;

import org.junit.Assert;
import org.junit.Test;

public class PredicateTest {

  @Test
  public void testPredicate() {

    Predicate predicate = new Predicate("edge", 2);

    Assert.assertEquals("edge", predicate.baseName());
    Assert.assertEquals("edge", predicate.name());
    Assert.assertEquals(2, predicate.arity());
    Assert.assertFalse(predicate.isNegated());
    Assert.assertFalse(predicate.isPrimitive());
  }

  @Test
  public void testNegatedPredicate() {

    Predicate predicate = new Predicate("~edge", 2);

    Assert.assertEquals("edge", predicate.baseName());
    Assert.assertEquals("~edge", predicate.name());
    Assert.assertEquals(2, predicate.arity());
    Assert.assertTrue(predicate.isNegated());
    Assert.assertFalse(predicate.isPrimitive());
  }

  @Test
  public void testBuiltinPredicate() {

    Predicate predicate = new Predicate("fn_isOk", 1);

    Assert.assertEquals("fn_isOk", predicate.baseName());
    Assert.assertEquals("fn_isOk", predicate.name());
    Assert.assertEquals(1, predicate.arity());
    Assert.assertFalse(predicate.isNegated());
    Assert.assertTrue(predicate.isPrimitive());
  }

  @Test(expected = IllegalStateException.class)
  public void testNegatedBuiltinPredicate() {
    Predicate predicate = new Predicate("~fn_isOk", 1);
  }
}
