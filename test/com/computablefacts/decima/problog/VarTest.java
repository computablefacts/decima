package com.computablefacts.decima.problog;

import org.junit.Assert;
import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class VarTest {

  @Test
  public void testHashcodeAndEquals() {
    EqualsVerifier.forClass(Var.class).withNonnullFields("id_").withIgnoredFields("isWildcard_")
        .verify();
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
