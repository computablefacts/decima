package com.computablefacts.decima.problog;

import static com.computablefacts.decima.problog.AbstractTerm.newVar;

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
}
