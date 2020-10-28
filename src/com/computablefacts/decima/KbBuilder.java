package com.computablefacts.decima;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.computablefacts.decima.problog.AbstractTerm;
import com.computablefacts.decima.problog.Clause;
import com.computablefacts.decima.problog.Const;
import com.computablefacts.decima.problog.InMemoryKnowledgeBase;
import com.computablefacts.decima.problog.Literal;
import com.computablefacts.decima.problog.RandomString;
import com.computablefacts.nona.helpers.CommandLine;
import com.computablefacts.nona.helpers.Files;
import com.github.wnameless.json.flattener.JsonFlattener;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.errorprone.annotations.CheckReturnValue;

@CheckReturnValue
final public class KbBuilder extends CommandLine {

  private static final char SEPARATOR = '¤';

  public static void main(String[] args) {

    Preconditions.checkNotNull(args, "args should not be null");

    File input = getFileCommand(args, "input", null);
    String output = getStringCommand(args, "output", null);
    boolean showLogs = getBooleanCommand(args, "show_logs", false);

    Stopwatch stopwatch = Stopwatch.createStarted();
    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();

    // Fill KB from ND-JSON file (see http://ndjson.org for details)
    RandomString rnd = new RandomString(8);
    long nbFacts = Files.lineStream(input, StandardCharsets.UTF_8)
        .filter(pair -> !Strings.isNullOrEmpty(pair.getValue()))
        .map(pair -> new JsonFlattener(pair.getValue()).withSeparator(SEPARATOR).flattenAsMap())
        .peek(json -> {
          String uid = rnd.nextString();
          json.forEach((k, v) -> {

            List<AbstractTerm> terms = new ArrayList<>();
            terms.add(new Const(uid));
            Splitter.on(SEPARATOR).trimResults().split(k)
                .forEach(term -> terms.add(new Const(term)));
            terms.add(new Const(v == null ? "null" : v.toString()));

            kb.azzert(new Clause(new Literal("json_path", terms)));
          });
        }).count();

    if (output == null) {
      System.out.println(kb.toString());
    } else {
      Files.create(new File(output), kb.toString());
    }

    stopwatch.stop();

    if (showLogs) {
      System.out.println("number of facts : " + kb.facts().size());
      System.out.println("elapsed time : " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
    }
  }
}
