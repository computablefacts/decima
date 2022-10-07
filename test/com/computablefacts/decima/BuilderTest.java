package com.computablefacts.decima;

import com.computablefacts.asterix.WildcardMatcher;
import com.computablefacts.decima.problog.Parser;
import com.computablefacts.decima.yaml.TestUtils;
import com.google.common.collect.Lists;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@net.jcip.annotations.NotThreadSafe
public class BuilderTest {

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

    String json = TestUtils.load("/data/tests/simple-json.txt");
    Path input = Files.createTempFile("rules-", ".json");
    Files.write(input, Lists.newArrayList(json));

    Builder.main(new String[]{"-input", input.toString()});

    String result = outContent_.toString().replace("\r", "");

    Assert.assertTrue(WildcardMatcher.match(result, String.format("*json(\"\", \"????????\", \"%s\").\n*", Parser.wrap(
        "{\"Actors\":[{\"name\":\"Tom Cruise\",\"age\":56,\"Born At\":\"Syracuse, NY\",\"Birthdate\":\"July 3, 1962\",\"photo\":\"https://jsonformatter.org/img/tom-cruise.jpg\",\"wife\":null,\"weight\":67.5,\"hasChildren\":true,\"hasGreyHair\":false,\"children\":[\"Suri\",\"Isabella Jane\",\"Connor\"]},{\"name\":\"Robert Downey Jr.\",\"age\":53,\"Born At\":\"New York City, NY\",\"Birthdate\":\"April 4, 1965\",\"photo\":\"https://jsonformatter.org/img/Robert-Downey-Jr.jpg\",\"wife\":\"Susan Downey\",\"weight\":77.1,\"hasChildren\":true,\"hasGreyHair\":false,\"children\":[\"Indio Falconer\",\"Avri Roel\",\"Exton Elias\"]}]}"))));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"\", \"????????\", \"Actors\", \"0\", \"children\", \"2\", \"Connor\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"\", \"????????\", \"Actors\", \"1\", \"children\", \"1\", \"Avri Roel\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"\", \"????????\", \"Actors\", \"0\", \"Born At\", \"b64_(U3lyYWN1c2UsIE5Z)\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"\", \"????????\", \"Actors\", \"1\", \"Birthdate\", \"b64_(QXByaWwgNCwgMTk2NQ==)\").\n*"));
    Assert.assertTrue(
        WildcardMatcher.match(result, "*json_path(\"\", \"????????\", \"Actors\", \"1\", \"weight\", \"77.1\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"\", \"????????\", \"Actors\", \"0\", \"photo\", \"b64_(aHR0cHM6Ly9qc29uZm9ybWF0dGVyLm9yZy9pbWcvdG9tLWNydWlzZS5qcGc=)\").\n*"));
    Assert.assertTrue(
        WildcardMatcher.match(result, "*json_path(\"\", \"????????\", \"Actors\", \"0\", \"weight\", \"67.5\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"\", \"????????\", \"Actors\", \"0\", \"Birthdate\", \"b64_(SnVseSAzLCAxOTYy)\").\n*"));
    Assert.assertTrue(
        WildcardMatcher.match(result, "*json_path(\"\", \"????????\", \"Actors\", \"0\", \"wife\", \"null\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"\", \"????????\", \"Actors\", \"1\", \"Born At\", \"b64_(TmV3IFlvcmsgQ2l0eSwgTlk=)\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"\", \"????????\", \"Actors\", \"1\", \"photo\", \"b64_(aHR0cHM6Ly9qc29uZm9ybWF0dGVyLm9yZy9pbWcvUm9iZXJ0LURvd25leS1Kci5qcGc=)\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"\", \"????????\", \"Actors\", \"1\", \"children\", \"2\", \"Exton Elias\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"\", \"????????\", \"Actors\", \"1\", \"hasGreyHair\", \"false\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"\", \"????????\", \"Actors\", \"0\", \"children\", \"1\", \"Isabella Jane\").\n*"));
    Assert.assertTrue(
        WildcardMatcher.match(result, "*json_path(\"\", \"????????\", \"Actors\", \"1\", \"age\", \"53\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"\", \"????????\", \"Actors\", \"1\", \"wife\", \"Susan Downey\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"\", \"????????\", \"Actors\", \"0\", \"hasChildren\", \"true\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"\", \"????????\", \"Actors\", \"1\", \"children\", \"0\", \"Indio Falconer\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"\", \"????????\", \"Actors\", \"0\", \"children\", \"0\", \"Suri\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"\", \"????????\", \"Actors\", \"1\", \"hasChildren\", \"true\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"\", \"????????\", \"Actors\", \"0\", \"hasGreyHair\", \"false\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"\", \"????????\", \"Actors\", \"1\", \"name\", \"Robert Downey Jr.\").\n*"));
    Assert.assertTrue(
        WildcardMatcher.match(result, "*json_path(\"\", \"????????\", \"Actors\", \"0\", \"age\", \"56\").\n*"));
    Assert.assertTrue(
        WildcardMatcher.match(result, "*json_path(\"\", \"????????\", \"Actors\", \"0\", \"name\", \"Tom Cruise\").*"));
  }
}
