package com.computablefacts.decima;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.computablefacts.decima.json.Fact;
import com.computablefacts.decima.json.Metadata;
import com.computablefacts.decima.json.Provenance;
import com.computablefacts.decima.problog.AbstractKnowledgeBase;
import com.computablefacts.decima.problog.AbstractTerm;
import com.computablefacts.decima.problog.Clause;
import com.computablefacts.decima.problog.Estimator;
import com.computablefacts.decima.problog.InMemoryKnowledgeBase;
import com.computablefacts.decima.problog.Literal;
import com.computablefacts.decima.problog.Parser;
import com.computablefacts.nona.helpers.Codecs;
import com.computablefacts.nona.helpers.CommandLine;
import com.computablefacts.nona.helpers.Files;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CheckReturnValue;

@CheckReturnValue
final public class Solver extends CommandLine {

  public static void main(String[] args) {

    Preconditions.checkNotNull(args, "args should not be null");

    File rules = getFileCommand(args, "rules", null);
    File facts = getFileCommand(args, "facts", null);
    File queries = getFileCommand(args, "queries", null);
    String output = getStringCommand(args, "output", null);
    String type = getStringCommand(args, "type", "problob");
    String extractedWith = getStringCommand(args, "extracted_with", "decima");
    String extractedBy = getStringCommand(args, "extracted_by", "decima");
    String root = getStringCommand(args, "root", null);
    String dataset = getStringCommand(args, "dataset", null);
    boolean showLogs = getBooleanCommand(args, "show_logs", false);

    Stopwatch stopwatch = Stopwatch.createStarted();
    Map<Literal, BigDecimal> answers = apply(rules, facts, queries);

    if ("problob".equals(type)) {

      String str = answers.entrySet().stream()
          .map(e -> e.getValue().toPlainString() + "::" + e.getKey().toString() + ".")
          .collect(Collectors.joining("\n"));

      if (output == null) {
        System.out.println(str);
      } else {
        Files.create(new File(output), str);
      }
    } else {

      // TODO : legacy code. Remove ASAP.
      String sourceType = "STORAGE/ROOT/DATASET/DOC_ID";
      String sourceStore = "ACCUMULO/" + root + "/" + dataset + "/000|0000-00-00T00:00:00.000Z";

      List<Fact> jsons = answers.entrySet().stream().map(e -> {

        String factType = e.getKey().predicate().name();
        double confidenceScore = e.getValue().doubleValue();
        Fact fact = new Fact(factType, confidenceScore, null, null, null, true);

        for (AbstractTerm term : e.getKey().terms()) {

          Preconditions.checkState(term.isConst(), "Term should be Const : %s", term);

          fact.value(term.toString());
        }

        fact.metadata(Lists.newArrayList(new Metadata("Comment", "extracted_with", extractedWith),
            new Metadata("Comment", "extracted_by", extractedBy),
            new Metadata("Comment", "extraction_date", Instant.now().toString())));
        fact.provenance(new Provenance(sourceType, sourceStore, null, null, null));

        return fact;
      }).collect(Collectors.toList());

      String str = jsons.stream().map(Codecs::asString).collect(Collectors.joining("\n"));

      if (output == null) {
        System.out.println(str);
      } else {
        Files.create(new File(output), str);
      }
    }

    stopwatch.stop();

    if (showLogs) {
      System.out.println("number of answers : " + answers.size());
      System.out.println("elapsed time : " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
    }
  }

  private static Map<Literal, BigDecimal> apply(File rules, File facts, File queries) {

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

    return apply(questions, clauses);
  }

  private static Map<Literal, BigDecimal> apply(Set<Literal> questions, Set<Clause> clauses) {

    Preconditions.checkNotNull(questions, "questions should not be null");
    Preconditions.checkNotNull(clauses, "clauses should not be null");

    Map<Literal, BigDecimal> answers = new HashMap<>();
    AbstractKnowledgeBase kb = new InMemoryKnowledgeBase();
    clauses.forEach(kb::azzert);

    com.computablefacts.decima.problog.Solver solver =
        new com.computablefacts.decima.problog.Solver(kb);

    for (Literal question : questions) {

      Estimator estimator = new Estimator(solver.proofs(question));
      Map<Clause, BigDecimal> probabilities = estimator.probabilities();

      probabilities.forEach((head, probability) -> answers.put(head.head(), probability));
    }
    return answers;
  }
}
