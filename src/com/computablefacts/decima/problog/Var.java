package com.computablefacts.decima.problog;

import java.util.concurrent.atomic.AtomicInteger;

import com.google.errorprone.annotations.CheckReturnValue;

/**
 * Variables as simple objects.
 */
@CheckReturnValue
final public class Var extends AbstractTerm {

  private final static AtomicInteger ID = new AtomicInteger(0);

  private final boolean isWildcard_;
  private final int id_;

  public Var() {
    this(false);
  }

  public Var(boolean isWildcard) {
    id_ = ID.getAndIncrement();
    isWildcard_ = isWildcard;
  }

  @Override
  public String toString() {
    return isWildcard_ ? "_" : "V" + id_;
  }

  @Override
  public String id() {
    return "v" + id_;
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
