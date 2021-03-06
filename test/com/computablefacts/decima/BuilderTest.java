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

    Builder.main(new String[] {"-input", input.toString()});

    String result = outContent_.toString().replace("\r", "");

    Assert.assertTrue(WildcardMatcher.match(result,
        "*json(\"\", \"????????\", \"{\\u0022Actors\\u0022\\u003a[{\\u0022name\\u0022\\u003a\\u0022Tom Cruise\\u0022\\u002c\\u0022age\\u0022\\u003a56\\u002c\\u0022Born At\\u0022\\u003a\\u0022Syracuse\\u002c NY\\u0022\\u002c\\u0022Birthdate\\u0022\\u003a\\u0022July 3\\u002c 1962\\u0022\\u002c\\u0022photo\\u0022\\u003a\\u0022https\\u003a//jsonformatter.org/img/tom-cruise.jpg\\u0022\\u002c\\u0022wife\\u0022\\u003anull\\u002c\\u0022weight\\u0022\\u003a67.5\\u002c\\u0022hasChildren\\u0022\\u003atrue\\u002c\\u0022hasGreyHair\\u0022\\u003afalse\\u002c\\u0022children\\u0022\\u003a[\\u0022Suri\\u0022\\u002c\\u0022Isabella Jane\\u0022\\u002c\\u0022Connor\\u0022]}\\u002c{\\u0022name\\u0022\\u003a\\u0022Robert Downey Jr.\\u0022\\u002c\\u0022age\\u0022\\u003a53\\u002c\\u0022Born At\\u0022\\u003a\\u0022New York City\\u002c NY\\u0022\\u002c\\u0022Birthdate\\u0022\\u003a\\u0022April 4\\u002c 1965\\u0022\\u002c\\u0022photo\\u0022\\u003a\\u0022https\\u003a//jsonformatter.org/img/Robert-Downey-Jr.jpg\\u0022\\u002c\\u0022wife\\u0022\\u003a\\u0022Susan Downey\\u0022\\u002c\\u0022weight\\u0022\\u003a77.1\\u002c\\u0022hasChildren\\u0022\\u003atrue\\u002c\\u0022hasGreyHair\\u0022\\u003afalse\\u002c\\u0022children\\u0022\\u003a[\\u0022Indio Falconer\\u0022\\u002c\\u0022Avri Roel\\u0022\\u002c\\u0022Exton Elias\\u0022]}]}\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"\", \"????????\", \"Actors\", \"0\", \"children\", \"2\", \"Connor\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"\", \"????????\", \"Actors\", \"1\", \"children\", \"1\", \"Avri Roel\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"\", \"????????\", \"Actors\", \"0\", \"Born At\", \"Syracuse\\u002c NY\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"\", \"????????\", \"Actors\", \"1\", \"Birthdate\", \"April 4\\u002c 1965\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"\", \"????????\", \"Actors\", \"1\", \"weight\", \"77.1\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"\", \"????????\", \"Actors\", \"0\", \"photo\", \"https\\u003a//jsonformatter.org/img/tom-cruise.jpg\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"\", \"????????\", \"Actors\", \"0\", \"weight\", \"67.5\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"\", \"????????\", \"Actors\", \"0\", \"Birthdate\", \"July 3\\u002c 1962\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"\", \"????????\", \"Actors\", \"0\", \"wife\", \"null\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"\", \"????????\", \"Actors\", \"1\", \"Born At\", \"New York City\\u002c NY\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"\", \"????????\", \"Actors\", \"1\", \"photo\", \"https\\u003a//jsonformatter.org/img/Robert-Downey-Jr.jpg\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"\", \"????????\", \"Actors\", \"1\", \"children\", \"2\", \"Exton Elias\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"\", \"????????\", \"Actors\", \"1\", \"hasGreyHair\", \"false\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"\", \"????????\", \"Actors\", \"0\", \"children\", \"1\", \"Isabella Jane\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"\", \"????????\", \"Actors\", \"1\", \"age\", \"53\").\n*"));
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
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"\", \"????????\", \"Actors\", \"0\", \"age\", \"56\").\n*"));
    Assert.assertTrue(WildcardMatcher.match(result,
        "*json_path(\"\", \"????????\", \"Actors\", \"0\", \"name\", \"Tom Cruise\").*"));
  }
}
