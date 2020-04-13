package com.computablefacts.decima.problog;

import java.util.Map;
import java.util.Objects;

import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;

/**
 * A term is either a variable or a constant.
 */
@CheckReturnValue
public abstract class AbstractTerm {

  protected AbstractTerm() {}

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }
    if (!(o instanceof AbstractTerm)) {
      return false;
    }
    AbstractTerm term = (AbstractTerm) o;
    return Objects.equals(id(), term.id());
  }

  @Override
  public int hashCode() {
    return Objects.hash(id());
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
   * Term tag.
   *
   * @return an identifier that maps all variables to the character "v".
   */
  public String tag() {
    return isConst() ? id() : "v";
  }

  /**
   * Term unique identifier.
   *
   * @return an unique identifier for the current term.
   */
  public abstract String id();

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
