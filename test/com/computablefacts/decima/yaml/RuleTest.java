package com.computablefacts.decima.yaml;

import com.google.common.collect.Lists;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Assert;
import org.junit.Test;

public class RuleTest {

  @Test
  public void testHashcodeAndEquals() {
    EqualsVerifier.forClass(Rule.class).withIgnoredFields("tests_").suppress(Warning.NONFINAL_FIELDS).verify();
  }

  @Test
  public void testToStringBodyWithOneLiteral() {

    Rule rule = new Rule("stress", "", 0.3, "X", Lists.newArrayList("person(X)").toArray(new String[1]));

    Assert.assertEquals("0.3::stress(X) :- person(X).\n", rule.toString());
  }

  @Test
  public void testToStringBodyWithMoreThanOneLiteral() {

    Rule rule = new Rule("smokes", "", 1.0, "X",
        Lists.newArrayList("stress(X)", "friend(X, Y), influences(Y, X), smokes(Y)").toArray(new String[2]));

    Assert.assertEquals(
        "1.0::smokes(X) :- stress(X).\n" + "1.0::smokes(X) :- friend(X, Y), influences(Y, X), smokes(Y).\n",
        rule.toString());
  }
}
