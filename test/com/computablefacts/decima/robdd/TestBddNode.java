package com.computablefacts.decima.robdd;

import com.google.errorprone.annotations.Var;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Assert;
import org.junit.Test;

public class TestBddNode {

  @Test
  public void testHashcodeAndEquals() {
    BddManager manager = new BddManager(1);
    EqualsVerifier.forClass(BddNode.class).suppress(Warning.NONFINAL_FIELDS)
        .withPrefabValues(BddNode.class, manager.Zero, manager.One).withIgnoredFields("value_", "index_", "refCount_")
        .verify();
  }

  @Test
  public void testTupleValueForOne() {
    BddManager manager = new BddManager(1);
    Assert.assertEquals(-1, manager.One.key().t.intValue());
    Assert.assertEquals(0, manager.One.key().u.intValue());
  }

  @Test
  public void testTupleValueForZero() {
    BddManager manager = new BddManager(1);
    Assert.assertEquals(-1, manager.Zero.key().t.intValue());
    Assert.assertEquals(-1, manager.Zero.key().u.intValue());
  }

  @Test
  public void testToString() {

    BddManager manager = new BddManager(1);
    BddNode c = manager.create(0, manager.Zero, manager.One);

    @Var String str = manager.One.toString();

    Assert.assertTrue(str.contains("Identifier=1"));
    Assert.assertTrue(str.contains("Value=true"));
    Assert.assertTrue(str.contains("Low=null"));
    Assert.assertTrue(str.contains("High=null"));

    str = manager.Zero.toString();

    Assert.assertTrue(str.contains("Identifier=0"));
    Assert.assertTrue(str.contains("Value=false"));
    Assert.assertTrue(str.contains("Low=null"));
    Assert.assertTrue(str.contains("High=null"));

    str = c.toString();

    Assert.assertTrue(str.contains("Value="));
    Assert.assertTrue(str.contains("Low=1"));
    Assert.assertTrue(str.contains("High=0"));
  }
}
