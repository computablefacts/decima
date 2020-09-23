package com.computablefacts.decima;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.CheckReturnValue;

import com.computablefacts.decima.problog.AbstractKnowledgeBase;
import com.computablefacts.decima.problog.Clause;
import com.computablefacts.decima.problog.Estimator;
import com.computablefacts.decima.problog.InMemoryKnowledgeBase;
import com.computablefacts.decima.problog.Literal;
import com.computablefacts.decima.problog.Parser;
import com.computablefacts.nona.helpers.CommandLine;
import com.computablefacts.nona.helpers.Files;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;

@CheckReturnValue
final public class Solver extends CommandLine {

  public static void main(String[] args) {

    Preconditions.checkNotNull(args, "args should not be null");

    File rules = getFileCommand(args, "rules", null);
    File facts = getFileCommand(args, "facts", null);
    File queries = getFileCommand(args, "queries", null);

    Stopwatch stopwatch = Stopwatch.createStarted();

    apply(rules, facts, queries);

    stopwatch.stop();

    System.out.println("elapsed time : " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
  }

  private static void apply(File rules, File facts, File queries) {

    Preconditions.checkNotNull(rules, "rules should not be null");
    Preconditions.checkNotNull(facts, "facts should not be null");
    Preconditions.checkNotNull(queries, "queries should not be null");

    Preconditions.checkArgument(rules.exists(), "Missing rules : %s", rules);
    Preconditions.checkArgument(facts.exists(), "Missing facts : %s", facts);
    Preconditions.checkArgument(queries.exists(), "Missing queries : %s", queries);

    Set<Clause> clauses = Sets.union(
        Files.lineStream(rules, StandardCharsets.UTF_8)
            .map(line -> Parser.parseClause(line.getValue())).collect(Collectors.toSet()),
        Files.lineStream(facts, StandardCharsets.UTF_8)
            .map(line -> Parser.parseClause(line.getValue())).collect(Collectors.toSet()));

    Set<Literal> questions = Files.lineStream(queries, StandardCharsets.UTF_8)
        .map(line -> Parser.parseQuery(line.getValue())).collect(Collectors.toSet());

    apply(questions, clauses);
  }

  private static void apply(Set<Literal> questions, Set<Clause> clauses) {

    Preconditions.checkNotNull(questions, "questions should not be null");
    Preconditions.checkNotNull(clauses, "clauses should not be null");

    AbstractKnowledgeBase kb = new InMemoryKnowledgeBase();
    clauses.forEach(kb::azzert);

    com.computablefacts.decima.problog.Solver solver =
        new com.computablefacts.decima.problog.Solver(kb);

    for (Literal question : questions) {

      Estimator estimator = new Estimator(solver.proofs(question));
      Map<Clause, BigDecimal> probabilities = estimator.probabilities();

      probabilities.forEach((head, probability) -> System.out
          .println(head.head().toString() + " -> " + probability.toPlainString()));
    }
  }
}
