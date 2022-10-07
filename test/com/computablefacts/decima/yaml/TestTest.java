package com.computablefacts.decima.yaml;

import com.computablefacts.decima.problog.Clause;
import com.computablefacts.decima.problog.Parser;
import com.google.common.collect.Lists;
import java.util.Set;
import org.junit.Assert;

public class TestTest {

  @org.junit.Test
  public void testIsValidWithOutput() {

    Set<Clause> rules = Parser.parseClauses(
        new Rule("child", "", 1.0, "X", Lists.newArrayList("boy(X)", "girl(X)").toArray(new String[2])).toString());

    Test test1 = new com.computablefacts.decima.yaml.Test("girl(alice).\nboy(alex).\n", "child(alice)?",
        "child(alice).");
    Test test2 = new com.computablefacts.decima.yaml.Test("girl(alice).\nboy(alex).\n", "child(alex)?", "child(alex).");

    Assert.assertTrue(test1.matchOutput(rules));
    Assert.assertTrue(test2.matchOutput(rules));
  }

  @org.junit.Test
  public void testIsValidWithoutOutput() {

    Set<Clause> rules = Parser.parseClauses(
        new Rule("child", "", 1.0, "X", Lists.newArrayList("boy(X)", "girl(X)").toArray(new String[2])).toString());

    Test test1 = new com.computablefacts.decima.yaml.Test("girl(alice).\nboy(alex).\n", "child(tom)?");
    Test test2 = new com.computablefacts.decima.yaml.Test("girl(alice).\nboy(alex).\n", "child(jerry)?");

    Assert.assertTrue(test1.matchOutput(rules));
    Assert.assertTrue(test2.matchOutput(rules));
  }
}
