package com.computablefacts.decima.problog;

import com.google.errorprone.annotations.CheckReturnValue;

/**
 * Constants as simple objects.
 */
@CheckReturnValue
final public class Const extends AbstractTerm {

  private final String value_;

  Const(String id, String value) {
    super(id);
    value_ = value;
  }

  @Override
  public String toString() {
    return value_;
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
