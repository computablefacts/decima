package com.computablefacts.decima.problog;

import static com.computablefacts.decima.problog.AbstractTerm.newConst;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Assert;
import org.junit.Test;

public class ConstTest {

  @Test
  public void testHashcodeAndEquals() {
    EqualsVerifier.forClass(Const.class).withIgnoredFields("value_").withNonnullFields("id_").verify();
  }

  @Test
  public void testId() {

    Const a = newConst(1);
    Assert.assertEquals("c1", a.id());

    Const b = newConst("1");
    Assert.assertEquals("c1", b.id());
  }

  @Test
  public void testToString() {

    Const a = newConst(1);
    Assert.assertEquals("1", a.toString());

    Const b = newConst("1");
    Assert.assertEquals("1", b.toString());
  }
}
