package com.computablefacts.decima.problog;

import com.computablefacts.nona.types.BoxedType;
import com.google.errorprone.annotations.CheckReturnValue;

/**
 * Constants as simple objects.
 */
@CheckReturnValue
final public class Const extends AbstractTerm {

  private final BoxedType<?> value_;

  Const(String id, Object value) {
    super(id);
    value_ = value instanceof BoxedType ? (BoxedType<?>) value : BoxedType.create(value);
  }

  @Override
  public String toString() {
    return value_.asString();
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
    return value_.value();
  }
}
