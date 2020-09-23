package com.computablefacts.decima;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

@net.jcip.annotations.NotThreadSafe
public class SolverTest {

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
  public void testSolveSimpleQuizz() throws IOException {

    List<String> facts = Lists.newArrayList("boy(bill).", "mother(alice, bill).");
    List<String> rules = Lists.newArrayList("child(X,Y) :- mother(Y,X).",
        "child(X,Y) :- father(Y,X).", "son(X,Y) :- child(X,Y),boy(X).");
    List<String> queries = Lists.newArrayList("son(X, alice)?");

    Path factz = Files.createTempFile("facts-", ".txt");
    java.nio.file.Files.write(factz, facts, StandardCharsets.UTF_8, StandardOpenOption.APPEND);

    Path rulez = Files.createTempFile("rules-", ".txt");
    java.nio.file.Files.write(rulez, rules, StandardCharsets.UTF_8, StandardOpenOption.APPEND);

    Path queriez = Files.createTempFile("queries-", ".txt");
    java.nio.file.Files.write(queriez, queries, StandardCharsets.UTF_8, StandardOpenOption.APPEND);

    Solver.main(new String[] {"-facts", factz.toString(), "-rules", rulez.toString(), "-queries",
        queriez.toString()});

    Assert.assertTrue(outContent_.toString().startsWith("son(\"bill\", \"alice\") -> 1.0000"));
  }
}
