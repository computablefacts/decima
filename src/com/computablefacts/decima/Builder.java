package com.computablefacts.decima;

import static com.computablefacts.decima.problog.AbstractTerm.newConst;

import com.computablefacts.asterix.IO;
import com.computablefacts.asterix.RandomString;
import com.computablefacts.asterix.View;
import com.computablefacts.asterix.codecs.JsonCodec;
import com.computablefacts.asterix.console.ConsoleApp;
import com.computablefacts.decima.problog.AbstractTerm;
import com.computablefacts.decima.problog.Clause;
import com.computablefacts.decima.problog.InMemoryKnowledgeBase;
import com.computablefacts.decima.problog.Literal;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.errorprone.annotations.CheckReturnValue;
import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@CheckReturnValue
final public class Builder extends ConsoleApp {

  private static final char SEPARATOR = '¤';

  public static Clause json(String namespace, String uuid, String json) {

    Preconditions.checkNotNull(namespace, "namespace should not be null");
    Preconditions.checkNotNull(uuid, "uuid should not be null");
    Preconditions.checkNotNull(json, "json should not be null");

    return new Clause(new Literal("json", Lists.newArrayList(newConst(namespace), newConst(uuid), newConst(json))));
  }

  public static Set<Clause> jsonPaths(String namespace, String uuid, String json) {

    Preconditions.checkNotNull(namespace, "namespace should not be null");
    Preconditions.checkNotNull(uuid, "uuid should not be null");
    Preconditions.checkNotNull(json, "json should not be null");

    Set<Clause> clauses = new HashSet<>();

    JsonCodec.flatten(json, SEPARATOR).forEach((k, v) -> {

      List<AbstractTerm> terms = new ArrayList<>();
      terms.add(newConst(namespace));
      terms.add(newConst(uuid));
      Splitter.on(SEPARATOR).trimResults().split(k).forEach(term -> {

        int indexBegin = term.lastIndexOf('[');
        int indexEnd = term.lastIndexOf(']');

        if (indexBegin < 0 || indexEnd < 0 || indexBegin >= indexEnd || indexEnd != term.length() - 1) {
          terms.add(newConst(term));
        } else { // Here, we are dealing with an array
          terms.add(newConst(term.substring(0, indexBegin)));
          terms.add(newConst(Integer.parseInt(term.substring(indexBegin + 1, indexEnd), 10)));
        }
      });
      terms.add(newConst(v == null ? "null" : v.toString()));

      clauses.add(new Clause(new Literal("json_path", terms)));
    });
    return clauses;
  }

  public static void main(String[] args) {

    Preconditions.checkNotNull(args, "args should not be null");

    File input = getFileCommand(args, "input", null);
    String output = getStringCommand(args, "output", null);
    boolean showLogs = getBooleanCommand(args, "show_logs", false);

    Stopwatch stopwatch = Stopwatch.createStarted();
    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();

    // Fill KB from ND-JSON file (see http://ndjson.org for details)
    RandomString rnd = new RandomString(8);
    long nbFacts = View.of(input).index().filter(pair -> !Strings.isNullOrEmpty(pair.getValue()))
        .map(pair -> new AbstractMap.SimpleEntry<>(rnd.nextString(), pair.getValue()))
        .peek(pair -> kb.azzert(json("", pair.getKey(), pair.getValue())))
        .peek(pair -> kb.azzert(jsonPaths("", pair.getKey(), pair.getValue()))).reduce(0, (carry, x) -> carry + 1);

    if (output == null) {
      System.out.println(kb);
    } else {
      boolean isOk = IO.writeText(new File(output), kb.toString(), false);
    }

    stopwatch.stop();

    if (showLogs) {
      System.out.println("number of facts : " + kb.nbFacts());
      System.out.println("elapsed time : " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
    }
  }
}
