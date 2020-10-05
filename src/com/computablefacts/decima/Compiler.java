package com.computablefacts.decima;

import java.io.File;
import java.util.concurrent.TimeUnit;

import javax.annotation.CheckReturnValue;

import com.computablefacts.decima.yaml.Rules;
import com.computablefacts.nona.helpers.CommandLine;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;

@CheckReturnValue
final public class Compiler extends CommandLine {

  public static void main(String[] args) {

    Preconditions.checkNotNull(args, "args should not be null");

    File file = getFileCommand(args, "file", null);

    Stopwatch stopwatch = Stopwatch.createStarted();

    Rules rules = Rules.load(file, true);

    System.out.println(
        "================================================================================");
    System.out.println(rules.toString());
    System.out.println(
        "================================================================================");

    stopwatch.stop();

    System.out.println("elapsed time : " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
  }
}
