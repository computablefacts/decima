package com.computablefacts.decima.problog;

import com.google.errorprone.annotations.CheckReturnValue;

/**
 * Constants as simple objects.
 */
@CheckReturnValue
final public class Const extends AbstractTerm {

  Const(String id) {
    super(id);
  }

  @Override
  public String toString() {
    return String.valueOf(objectOrNull());
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
    return objectOrNull();
  }
}
