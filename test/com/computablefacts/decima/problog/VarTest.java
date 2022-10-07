package com.computablefacts.decima.problog;

import static com.computablefacts.decima.problog.AbstractTerm.newVar;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Assert;
import org.junit.Test;

public class VarTest {

  @Test
  public void testHashcodeAndEquals() {
    EqualsVerifier.forClass(Var.class).withNonnullFields("id_").withIgnoredFields("isWildcard_").verify();
  }

  @Test
  public void testWildcardEquals() {

    Var a = newVar(true);
    Var b = newVar(true);

    Assert.assertEquals(a, a);
    Assert.assertEquals(b, b);

    Assert.assertNotEquals(a, b);
  }

  @Test
  public void testWildcardHashcode() {

    Var a = newVar(true);
    Var b = newVar(true);

    Assert.assertEquals(a.hashCode(), a.hashCode());
    Assert.assertEquals(b.hashCode(), b.hashCode());

    Assert.assertNotEquals(a.hashCode(), b.hashCode());
  }

  @Test
  public void testId() {

    Var a = newVar(true);
    Assert.assertTrue(a.id().startsWith("v"));

    Var b = newVar();
    Assert.assertTrue(b.id().startsWith("v"));
  }

  @Test
  public void testToString() {

    Var a = newVar(true);
    Assert.assertEquals("_", a.toString());

    Var b = newVar();
    Assert.assertTrue(b.toString().startsWith("V"));
  }
}
