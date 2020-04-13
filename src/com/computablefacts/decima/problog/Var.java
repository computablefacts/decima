package com.computablefacts.decima.problog;

import java.util.concurrent.atomic.AtomicInteger;

import com.google.errorprone.annotations.CheckReturnValue;

/**
 * Variables as simple objects.
 */
@CheckReturnValue
final public class Var extends AbstractTerm {

  private final static AtomicInteger ID = new AtomicInteger(0);

  private final String id_;
  private final boolean isWildcard_;

  public Var() {
    this(false);
  }

  public Var(boolean isWildcard) {
    id_ = "v" + Integer.toString(ID.getAndIncrement(), 10);
    isWildcard_ = isWildcard;
  }

  @Override
  public String toString() {
    return isWildcard_ ? "_" : id_.toUpperCase();
  }

  @Override
  public String id() {
    return id_;
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
