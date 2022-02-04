package com.computablefacts.decima.problog;

import com.google.errorprone.annotations.CheckReturnValue;

/**
 * Variables as simple objects.
 */
@CheckReturnValue
final public class Var extends AbstractTerm {

  private final boolean isWildcard_;

  Var(String id, boolean isWildcard) {
    super(id);
    isWildcard_ = isWildcard;
  }

  @Override
  public String toString() {
    return isWildcard_ ? "_" : id().toUpperCase();
  }

  @Override
  public boolean isConst() {
    return false;
  }

  @Override
  public boolean isWildcard() {
    return isWildcard_;
  }
}
