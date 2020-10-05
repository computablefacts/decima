package com.computablefacts.decima.yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Lists;

public class RulesTest {

  @Test
  public void testLoadRules() throws IOException {

    String yaml = "name: Friends and Smokers\n"
        + "description: \"The program below encodes a variant of the \\\"Friends & Smokers\\\" problem.\"\n"
        + "rules:\n" + "  - name: stress\n" + "    parameters: X\n" + "    confidence: 0.3\n"
        + "    body:\n" + "      - person(X)\n" + "  - name: influences\n"
        + "    parameters: X, Y\n" + "    confidence: 0.2\n" + "    body:\n"
        + "      - person(X), person(Y)\n" + "  - name: smokes\n" + "    parameters: X\n"
        + "    body:\n" + "      - stress(X)\n"
        + "      - friend(X, Y), influences(Y, X), smokes(Y)\n" + "  - name: asthma\n"
        + "    parameters: X\n" + "    confidence: 0.4\n" + "    body:\n" + "      - smokes(X)\n";

    Path file = Files.createTempFile("rules-", ".yml");
    Files.write(file, Lists.newArrayList(yaml));
    Rules rules = Rules.load(file.toFile());

    Assert.assertEquals("Friends and Smokers", rules.name_);
    Assert.assertEquals("The program below encodes a variant of the \"Friends & Smokers\" problem.",
        rules.description_);
    Assert.assertEquals(4, rules.rules_.length);
    Assert.assertEquals("0.3::stress(X) :- person(X).\n"
        + "0.2::influences(X, Y) :- person(X), person(Y).\n" + "1.0::smokes(X) :- stress(X).\n"
        + "1.0::smokes(X) :- friend(X, Y), influences(Y, X), smokes(Y).\n"
        + "0.4::asthma(X) :- smokes(X).\n", rules.toString());
  }

  @Test
  public void testLoadAndTestRules() throws IOException {

    String yaml = "name: Friends and Smokers\n"
        + "description: \"The program below encodes a variant of the \\\"Friends & Smokers\\\" problem.\"\n"
        + "rules:\n" + "  - name: stress\n" + "    parameters: X\n" + "    confidence: 0.3\n"
        + "    body:\n" + "      - person(X)\n" + "  - name: influences\n"
        + "    parameters: X, Y\n" + "    confidence: 0.2\n" + "    body:\n"
        + "      - person(X), person(Y)\n" + "  - name: smokes\n" + "    parameters: X\n"
        + "    body:\n" + "      - stress(X)\n"
        + "      - friend(X, Y), influences(Y, X), smokes(Y)\n" + "    tests:\n"
        + "      - kb: \"person(éléana).\\nperson(jean).\\nperson(pierre).\\nperson(alexis).\\nfriend(jean, pierre).\\nfriend(jean, éléana).\\nfriend(jean, alexis).\\nfriend(éléana, pierre).\"\n"
        + "        query: \"smokes(éléana)?\"\n" + "        output: \"0.342::smokes(éléana).\"\n"
        + "      - kb: \"person(éléana).\\nperson(jean).\\nperson(pierre).\\nperson(alexis).\\nfriend(jean, pierre).\\nfriend(jean, éléana).\\nfriend(jean, alexis).\\nfriend(éléana, pierre).\"\n"
        + "        query: \"smokes(jean)?\"\n" + "        output: \"0.42557::smokes(jean).\"\n"
        + "  - name: asthma\n" + "    parameters: X\n" + "    confidence: 0.4\n" + "    body:\n"
        + "      - smokes(X)\n";

    Path file = Files.createTempFile("rules-", ".yml");
    Files.write(file, Lists.newArrayList(yaml));
    Rules rules = Rules.load(file.toFile(), true);

    Assert.assertNotNull(rules);
  }

  @Test
  public void testLoadAndTestRulesInvalid() throws IOException {

    // The only difference with testLoadAndTestRules() is the precision expected by the test
    // smokes(jean)?
    String yaml = "name: Friends and Smokers\n"
        + "description: \"The program below encodes a variant of the \\\"Friends & Smokers\\\" problem.\"\n"
        + "rules:\n" + "  - name: stress\n" + "    parameters: X\n" + "    confidence: 0.3\n"
        + "    body:\n" + "      - person(X)\n" + "  - name: influences\n"
        + "    parameters: X, Y\n" + "    confidence: 0.2\n" + "    body:\n"
        + "      - person(X), person(Y)\n" + "  - name: smokes\n" + "    parameters: X\n"
        + "    body:\n" + "      - stress(X)\n"
        + "      - friend(X, Y), influences(Y, X), smokes(Y)\n" + "    tests:\n"
        + "      - kb: \"person(éléana).\\nperson(jean).\\nperson(pierre).\\nperson(alexis).\\nfriend(jean, pierre).\\nfriend(jean, éléana).\\nfriend(jean, alexis).\\nfriend(éléana, pierre).\"\n"
        + "        query: \"smokes(éléana)?\"\n" + "        output: \"0.342::smokes(éléana).\"\n"
        + "      - kb: \"person(éléana).\\nperson(jean).\\nperson(pierre).\\nperson(alexis).\\nfriend(jean, pierre).\\nfriend(jean, éléana).\\nfriend(jean, alexis).\\nfriend(éléana, pierre).\"\n"
        + "        query: \"smokes(jean)?\"\n" + "        output: \"0.42556811::smokes(jean).\"\n"
        + "  - name: asthma\n" + "    parameters: X\n" + "    confidence: 0.4\n" + "    body:\n"
        + "      - smokes(X)\n";

    Path file = Files.createTempFile("rules-", ".yml");
    Files.write(file, Lists.newArrayList(yaml));
    Rules rules = Rules.load(file.toFile(), true);

    Assert.assertNull(rules);
  }
}
