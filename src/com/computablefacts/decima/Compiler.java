package com.computablefacts.decima;

import java.io.File;
import java.util.concurrent.TimeUnit;

import com.computablefacts.decima.yaml.Rules;
import com.computablefacts.nona.helpers.CommandLine;
import com.computablefacts.nona.helpers.Files;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.errorprone.annotations.CheckReturnValue;

@CheckReturnValue
final public class Compiler extends CommandLine {

  public static void main(String[] args) {

    Preconditions.checkNotNull(args, "args should not be null");

    File input = getFileCommand(args, "input", null);
    String output = getStringCommand(args, "output", null);
    boolean showLogs = getBooleanCommand(args, "show_logs", false);

    Stopwatch stopwatch = Stopwatch.createStarted();
    Rules rules = Rules.load(input, true);

    if (output == null) {
      System.out.println(rules.toString());
    } else {
      Files.create(new File(output), rules.toString());
    }

    stopwatch.stop();

    if (showLogs) {
      System.out.println("number of rules : " + rules.nbRules());
      System.out.println("elapsed time : " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
    }
  }
}
