package com.computablefacts.decima.problog;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

import com.computablefacts.nona.Generated;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;

/**
 * Extracted from https://stackoverflow.com/a/41156
 */
@CheckReturnValue
final public class RandomString {

  public static final String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  public static final String lower = upper.toLowerCase(Locale.ROOT);
  public static final String digits = "0123456789";
  public static final String alphanum = upper + lower + digits;

  private final Random random;
  private final char[] symbols;
  private final char[] buf;

  /**
   * Create an alphanumeric string generator.
   */
  @Generated
  public RandomString(int length, Random random) {
    this(length, random, alphanum);
  }

  /**
   * Create session identifiers.
   */
  @Generated
  public RandomString() {
    this(21);
  }

  /**
   * Create an alphanumeric strings from a secure generator.
   */
  @Generated
  public RandomString(int length) {
    this(length, new SecureRandom());
  }

  public RandomString(int length, Random random, String symbols) {

    Preconditions.checkArgument(length >= 1, "length must be >= 1");
    Preconditions.checkArgument(symbols.length() >= 2, "the number of symbols must be >= 2");

    this.random = Objects.requireNonNull(random);
    this.symbols = symbols.toCharArray();
    this.buf = new char[length];
  }

  /**
   * Generate a random string. Two consecutive calls never return the same value.
   */
  public String nextString() {
    for (int idx = 0; idx < buf.length; ++idx) {
      buf[idx] = symbols[random.nextInt(symbols.length)];
    }
    return new String(buf);
  }
}
