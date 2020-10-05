package com.computablefacts.decima.problog;

import java.util.Objects;

import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;

/**
 * A predicate symbol has a name and an arity. Negated predicates are prefixed with "~". Primitives
 * are prefixed with "fn_".
 */
@CheckReturnValue
final public class Predicate {

  private final String id_;
  private final String name_;
  private final int arity_;
  private final boolean isNegated_;
  private final boolean isPrimitive_;

  /**
   * Constructor.
   *
   * @param name predicate name.
   * @param arity predicate arity.
   */
  public Predicate(String name, int arity) {

    Preconditions.checkNotNull(name, "name should not be null");
    Preconditions.checkArgument(arity >= 0, "arity should be >= 0");

    String newName = name.startsWith("~") ? name.substring(1) : name;

    id_ = name + "/" + Integer.toString(arity, 10);
    name_ = name;
    arity_ = arity;
    isNegated_ = !name.equals(newName);
    isPrimitive_ = newName.startsWith("fn_");

    Preconditions.checkState(!(isNegated_ && isPrimitive_), "primitives cannot be negated");
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof Predicate)) {
      return false;
    }
    Predicate predicate = (Predicate) o;
    return Objects.equals(id_, predicate.id_);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id_);
  }

  @Override
  public String toString() {
    return name_;
  }

  /**
   * Predicate unique identifier.
   *
   * @return an unique identifier for the current predicate.
   */
  public String id() {
    return id_;
  }

  /**
   * Predicate name.
   *
   * @return predicate name. If the predicate is negated, the name starts with "~".
   */
  public String name() {
    return name_;
  }

  /**
   * Predicate base name.
   *
   * @return predicate base name. If the predicate is negated, the "~" is removed.
   */
  public String baseName() {
    return name_.startsWith("~") ? name_.substring(1) : name_;
  }

  /**
   * Predicate arity.
   *
   * @return predicate arity. An integer &gt;= 0.
   */
  public int arity() {
    return arity_;
  }

  /**
   * Check if the current predicate is negated.
   *
   * @return true iif the current predicate is negated.
   */
  public boolean isNegated() {
    return isNegated_;
  }

  /**
   * Check if the current predicate is a primitive.
   *
   * @return true iif the current predicate is a primitive.
   */
  public boolean isPrimitive() {
    return isPrimitive_;
  }
}
