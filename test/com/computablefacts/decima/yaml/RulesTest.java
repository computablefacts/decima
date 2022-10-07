package com.computablefacts.decima.yaml;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Assert;
import org.junit.Test;

public class RulesTest {

  @Test
  public void testLoadRules() throws IOException {

    String yaml = TestUtils.load("/data/tests/valid-yaml.yml");
    Path file = Files.createTempFile("rules-", ".yml");
    Files.write(file, Lists.newArrayList(yaml));
    Rules rules = Rules.load(file.toFile());

    Assert.assertEquals("Friends and Smokers", rules.name_);
    Assert.assertEquals("The program below encodes a variant of the \"Friends & Smokers\" problem.",
        rules.description_);
    Assert.assertEquals(4, rules.rules_.length);
    Assert.assertEquals("0.3::stress(X) :- person(X).\n" + "0.2::influences(X, Y) :- person(X), person(Y).\n"
        + "1.0::smokes(X) :- stress(X).\n" + "1.0::smokes(X) :- friend(X, Y), influences(Y, X), smokes(Y).\n"
        + "0.4::asthma(X) :- smokes(X).\n", rules.toString());
  }

  @Test
  public void testLoadAndTestRules() throws IOException {

    String yaml = TestUtils.load("/data/tests/valid-yaml.yml");
    Path file = Files.createTempFile("rules-", ".yml");
    Files.write(file, Lists.newArrayList(yaml));
    Rules rules = Rules.load(file.toFile(), true);

    Assert.assertNotNull(rules);
  }

  @Test
  public void testLoadAndTestRulesInvalid() throws IOException {

    String yaml = TestUtils.load("/data/tests/invalid-yaml.yml");
    Path file = Files.createTempFile("rules-", ".yml");
    Files.write(file, Lists.newArrayList(yaml));
    Rules rules = Rules.load(file.toFile(), true);

    Assert.assertNull(rules);
  }
}
