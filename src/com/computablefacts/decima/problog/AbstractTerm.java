package com.computablefacts.decima.problog;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Preconditions;
import com.google.common.collect.MapMaker;
import com.google.common.hash.Hashing;
import com.google.errorprone.annotations.CheckReturnValue;

/**
 * A term is either a variable or a constant.
 */
@CheckReturnValue
public abstract class AbstractTerm {

  /**
   * Internalize objects based on an identifier.
   *
   * To make comparisons between terms of the same type efficient, each term is internalized so
   * there is at most one of them associated with an identifier. An identifier is always a string.
   *
   * For example, after internalization, there is one constant for each string used to name a
   * constant.
   *
   * Idea extracted from https://github.com/catwell/datalog.lua/blob/master/datalog/datalog.lua
   */
  private final static ConcurrentMap<String, Object> idToObj_ = new MapMaker().weakKeys().makeMap();
  private final static AtomicInteger idGenerator_ = new AtomicInteger(0);

  private final String id_;

  protected AbstractTerm(String id) {
    id_ = Preconditions.checkNotNull(id, "id should not be null");
  }

  public static Const newConst(Object value) {

    String id;
    String newValue = String.valueOf(value);

    if (newValue.length() <= 32 /* murmur3_128 hash length */) {
      id = newValue;
    } else {
      id = Hashing.murmur3_128().newHasher().putString(newValue, StandardCharsets.UTF_8).hash()
          .toString();
    }

    idToObj_.putIfAbsent(id, value);
    return new Const(id);
  }

  public static Var newVar() {
    return newVar(false);
  }

  public static Var newVar(boolean isWildcard) {
    return new Var(Integer.toString(idGenerator_.getAndIncrement(), 10), isWildcard);
  }

  @Override
  final public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof AbstractTerm)) {
      return false;
    }
    AbstractTerm term = (AbstractTerm) obj;
    return id().equals(term.id());
  }

  @Override
  final public int hashCode() {
    return id().hashCode();
  }

  /**
   * Term tag.
   *
   * @return an identifier that maps all variables to the character "v".
   */
  final public String tag() {
    return isConst() ? id() : "v";
  }

  /**
   * Term unique identifier.
   *
   * @return a unique identifier for the current term. The identifier will start with a 'v'
   *         character if the current term is a variable (or wildcard) and a 'c' character
   *         otherwise.
   */
  final public String id() {
    return (isConst() ? "c" : "v") + id_;
  }

  /**
   * Returns the object (if any) associated with the current term identifier.
   *
   * @return an {@code Object}.
   */
  final protected Object objectOrNull() {
    return isConst() ? idToObj_.get(id_) : null;
  }

  /**
   * Try to bind the current term to an environment constant.
   *
   * @param env environment.
   * @return returns a constant or an unbound variable.
   */
  AbstractTerm chase(Map<Var, AbstractTerm> env) {

    Preconditions.checkNotNull(env, "env should not be null");

    if (isConst()) {
      return this;
    }

    if (env.containsKey(this)) {
      return env.get(this).chase(env);
    }
    return this;
  }

  /**
   * Try to substitute the current term to an environment constant.
   *
   * @param env environment.
   * @return returns a constant or an unbound variable.
   */
  AbstractTerm subst(Map<Var, AbstractTerm> env) {

    Preconditions.checkNotNull(env, "env should not be null");

    if (isConst()) {
      return this;
    }

    if (env.containsKey(this)) {
      return env.get(this);
    }
    return this;
  }

  /**
   * Unify two terms.
   * 
   * @param term term.
   * @param env environment.
   * @return the result is either an environment or null. Null is returned when the two terms cannot
   *         be unified.
   */
  Map<Var, AbstractTerm> unify(AbstractTerm term, Map<Var, AbstractTerm> env) {

    Preconditions.checkNotNull(term, "term should not be null");
    Preconditions.checkNotNull(env, "env should not be null");

    if (isConst()) {
      return term.unifyConst((Const) this, env);
    }
    return term.unifyVar((Var) this, env);
  }

  /**
   * Unify a term with a constant.
   *
   * @param constant constant.
   * @param env environment.
   * @return the result is either an environment or null. Null is returned when the two terms cannot
   *         be unified.
   */
  Map<Var, AbstractTerm> unifyConst(Const constant, Map<Var, AbstractTerm> env) {

    Preconditions.checkNotNull(constant, "constant should not be null");
    Preconditions.checkNotNull(env, "env should not be null");

    if (isConst()) {
      return null;
    }

    env.put((Var) this, constant);
    return env;
  }

  /**
   * Unify a term with a variable.
   * 
   * @param variable variable.
   * @param env environment.
   * @return the result is either an environment or null. Null is returned when the two terms cannot
   *         be unified.
   */
  Map<Var, AbstractTerm> unifyVar(Var variable, Map<Var, AbstractTerm> env) {

    Preconditions.checkNotNull(variable, "variable should not be null");
    Preconditions.checkNotNull(env, "env should not be null");

    if (isConst()) {
      return variable.unifyConst((Const) this, env);
    }

    env.put(variable, this);
    return env;
  }

  /**
   * Check if the current term is a constant.
   *
   * @return true iif the current term is a constant.
   */
  public abstract boolean isConst();

  /**
   * Check if the current term is a wildcard.
   *
   * @return true iif the current term is a wildcard.
   */
  public abstract boolean isWildcard();
}
