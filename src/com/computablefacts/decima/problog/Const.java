package com.computablefacts.decima.problog;

import java.nio.charset.StandardCharsets;

import com.google.common.base.Preconditions;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.errorprone.annotations.CheckReturnValue;

/**
 * Constants as simple objects.
 */
@CheckReturnValue
final public class Const extends AbstractTerm {

  private final Object value_;
  private final String id_;

  public Const(Object value) {

    value_ = Preconditions.checkNotNull(value, "value should not be null");

    String newValue = value_.toString();

    if (newValue.length() <= 32 /* murmur3_128 hash length */) {
      id_ = null;
    } else {
      Hasher hasher = Hashing.murmur3_128().newHasher();
      hasher.putString(newValue, StandardCharsets.UTF_8);
      id_ = hasher.hash().toString();
    }
  }

  @Override
  public String toString() {
    return String.valueOf(value_);
  }

  @Override
  public String id() {
    return id_ == null ? value_.toString() : id_;
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
