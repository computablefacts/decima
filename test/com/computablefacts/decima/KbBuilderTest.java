package com.computablefacts.decima;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.computablefacts.decima.yaml.TestUtils;
import com.computablefacts.nona.helpers.WildcardMatcher;
import com.google.common.collect.Lists;

@net.jcip.annotations.NotThreadSafe
public class KbBuilderTest {

  private final ByteArrayOutputStream outContent_ = new ByteArrayOutputStream();
  private final ByteArrayOutputStream errContent_ = new ByteArrayOutputStream();
  private final PrintStream originalOut_ = System.out;
  private final PrintStream originalErr_ = System.err;

  @Before
  public void setUpStreams() {
    System.setOut(new PrintStream(outContent_));
    System.setErr(new PrintStream(errContent_));
  }

  @After
  public void restoreStreams() {
    System.setOut(originalOut_);
    System.setErr(originalErr_);
  }

  @Test
  public void testBuildKb() throws IOException {

    String json = TestUtils.load("/data/tests/simple-json.json");
    Path input = Files.createTempFile("rules-", ".json");
    Files.write(input, Lists.newArrayList(json));

    KbBuilder.main(new String[] {"-input", input.toString()});

    String result = outContent_.toString().replace("\r", "");

    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"????????\", \"Actors[0]\", \"children[2]\", \"Connor\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"????????\", \"Actors[1]\", \"children[1]\", \"Avri Roel\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"????????\", \"Actors[0]\", \"Born At\", \"Syracuse\\u002c NY\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"????????\", \"Actors[1]\", \"Birthdate\", \"April 4\\u002c 1965\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"????????\", \"Actors[1]\", \"weight\", \"77.1\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"????????\", \"Actors[0]\", \"photo\", \"https://jsonformatter.org/img/tom-cruise.jpg\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"????????\", \"Actors[0]\", \"weight\", \"67.5\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"????????\", \"Actors[0]\", \"Birthdate\", \"July 3\\u002c 1962\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"????????\", \"Actors[0]\", \"wife\", \"null\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"????????\", \"Actors[1]\", \"Born At\", \"New York City\\u002c NY\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"????????\", \"Actors[1]\", \"photo\", \"https://jsonformatter.org/img/Robert-Downey-Jr.jpg\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"????????\", \"Actors[1]\", \"children[2]\", \"Exton Elias\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"????????\", \"Actors[1]\", \"hasGreyHair\", \"false\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"????????\", \"Actors[0]\", \"children[1]\", \"Isabella Jane\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"????????\", \"Actors[1]\", \"age\", \"53\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"????????\", \"Actors[1]\", \"wife\", \"Susan Downey\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"????????\", \"Actors[0]\", \"hasChildren\", \"true\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"????????\", \"Actors[1]\", \"children[0]\", \"Indio Falconer\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"????????\", \"Actors[0]\", \"children[0]\", \"Suri\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"????????\", \"Actors[1]\", \"hasChildren\", \"true\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"????????\", \"Actors[0]\", \"hasGreyHair\", \"false\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"????????\", \"Actors[1]\", \"name\", \"Robert Downey Jr.\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"????????\", \"Actors[0]\", \"age\", \"56\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"????????\", \"Actors[0]\", \"name\", \"Tom Cruise\").*"));
  }
}
