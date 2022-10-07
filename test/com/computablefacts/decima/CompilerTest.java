package com.computablefacts.decima;

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
public class CompilerTest {

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
  public void testCompile() throws IOException {

    String yaml = TestUtils.load("/data/tests/valid-yaml.yml");
    Path file = Files.createTempFile("rules-", ".yml");
    Files.write(file, Lists.newArrayList(yaml));
    Compiler.main(new String[]{"-input", file.toString()});

    Assert.assertTrue(outContent_.toString().replace("\r", "").startsWith(
        "0.3::stress(X) :- person(X).\n" + "0.2::influences(X, Y) :- person(X), person(Y).\n"
            + "1.0::smokes(X) :- stress(X).\n" + "1.0::smokes(X) :- friend(X, Y), influences(Y, X), smokes(Y).\n"
            + "0.4::asthma(X) :- smokes(X).\n"));
  }
}
