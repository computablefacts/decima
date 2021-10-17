package com.computablefacts.decima.problog;

import java.nio.charset.StandardCharsets;

import com.google.common.base.Preconditions;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.errorprone.annotations.CheckReturnValue;

/**
 * Constants as simple objects.
 */
@CheckReturnValue
final public class Const extends AbstractTerm {

  private static final HashFunction MURMUR3_128 = Hashing.murmur3_128();

  private final Object value_;

  public Const(Object value) {
    value_ = Preconditions.checkNotNull(value, "value should not be null");
  }

  private static String hash(Object value) {

    Hasher hasher = MURMUR3_128.newHasher();

    if (value != null) {

      String newValue = value.toString();

      if (newValue.length() <= 32) {
        return newValue;
      }
      hasher.putString(newValue, StandardCharsets.UTF_8);
    }
    return hasher.hash().toString();
  }

  @Override
  public String toString() {
    return String.valueOf(value_);
  }

  @Override
  public String id() {
    return hash(value_);
  }

  @Override
  public boolean isConst() {
    return true;
  }

  @Override
  public boolean isWildcard() {
    return false;
  }

  public Object value() {
    return value_;
  }
}
