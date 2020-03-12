package com.computablefacts.decima.problog;

import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;

/**
 * Constants as simple objects.
 */
@CheckReturnValue
final public class Const extends Term {

  private final String id_;
  private final Object value_;

  public Const(Object value) {
    value_ = Preconditions.checkNotNull(value, "value should not be null");
    id_ = addSize(value_.toString());
  }

  @Override
  public String toString() {
    return String.valueOf(value_);
  }

  @Override
  public String id() {
    return id_;
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

  private String addSize(String str) {

    Preconditions.checkNotNull(str, "str should not be null");

    return Integer.toString(str.length(), 10) + ":" + str;
  }
}
