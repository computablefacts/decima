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

import com.computablefacts.nona.helpers.WildcardMatcher;
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
  public void testSolveSimpleQuizzOutputsProblog() throws IOException {

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

    Assert.assertTrue(outContent_.toString().startsWith("1.0000::son(\"bill\", \"alice\")."));
  }

  @Test
  public void testSolveSimpleQuizzOutputsJson() throws IOException {

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
        queriez.toString(), "-type", "json", "-root", "my_root", "-dataset", "my_dataset"});

    Assert.assertTrue(WildcardMatcher.match(outContent_.toString(),
        "*{\"external_id\":\"*\",\"metadata\":[{\"type\":\"Comment\",\"key\":\"extracted_with\",\"value\":\"decima\"},{\"type\":\"Comment\",\"key\":\"extracted_by\",\"value\":\"decima\"},{\"type\":\"Comment\",\"key\":\"extraction_date\",\"value\":\"????-??-??T??:??:??*Z\"}],\"provenances\":[{\"source_store\":\"ACCUMULO/my_root/my_dataset/000|0000-00-00T00:00:00.000Z\",\"source_type\":\"STORAGE/ROOT/DATASET/DOC_ID\"}],\"type\":\"son\",\"confidence_score\":1.0,\"start_date\":\"????-??-??T??:??:??*Z\"}*"));
  }
}
