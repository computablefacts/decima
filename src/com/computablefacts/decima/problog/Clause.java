package com.computablefacts.decima.problog;

import com.computablefacts.asterix.Generated;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A clause has a head literal, and a sequence of literals that form its body. If there are no literals in its body, the
 * clause is called a fact. If there is at least one literal in its body, it is called a rule.
 * <p>
 * A clause asserts that its head is true if every literal in its body is true.
 */
@CheckReturnValue
final public class Clause {

  private final Literal head_;
  private final List<Literal> body_;

  private Boolean isGrounded_ = null;
  private Boolean isFact_ = null;

  /**
   * Initialize a fact.
   *
   * @param head literal.
   */
  @Generated
  public Clause(Literal head) {
    this(head, new ArrayList<>());
  }

  /**
   * Initialize a rule.
   *
   * @param head literal.
   * @param body body literals.
   */
  public Clause(Literal head, Literal... body) {
    this(head, Lists.newArrayList(body));
  }

  /**
   * Initialize a fact or a rule.
   *
   * @param head literal.
   * @param body list of literals.
   */
  public Clause(Literal head, List<Literal> body) {

    Preconditions.checkNotNull(head, "head should not be null");
    Preconditions.checkNotNull(body, "body should not be null");
    Preconditions.checkState(body.stream().noneMatch(Objects::isNull), "body literals should not be null");

    head_ = head;
    body_ = new ArrayList<>(body);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof Clause)) {
      return false;
    }

    Clause clause = (Clause) obj;

    if (clause.isFact()) {
      return head_.equals(clause.head_);
    }
    return head_.equals(clause.head_) && body_.equals(clause.body_);
  }

  @Override
  public int hashCode() {
    if (isFact()) {
      return head_.hashCode();
    }
    return Objects.hash(head_, body_);
  }

  @Override
  public String toString() {

    StringBuilder builder = new StringBuilder();
    builder.append(head_.toString());

    if (isRule()) {

      builder.append(" :- ");

      for (int i = 0; i < body_.size(); i++) {
        if (i > 0) {
          builder.append(", ");
        }
        Literal literal = body_.get(i);
        builder.append(literal.toString());
      }
    }
    return builder.toString();
  }

  /**
   * Get the current clause head.
   *
   * @return the clause head.
   */
  public Literal head() {
    return head_;
  }

  /**
   * Get the current clause body.
   *
   * @return the clause body.
   */
  public List<Literal> body() {
    return body_;
  }

  /**
   * Check if the current clause is a fact.
   *
   * @return true iif the current clause is a fact.
   */
  public boolean isFact() {
    if (isFact_ == null) {
      isFact_ = head_.isGrounded() && body_.isEmpty();
    }
    return isFact_;
  }

  /**
   * Check if the current clause is a rule.
   *
   * @return true iif the current clause is a rule.
   */
  public boolean isRule() {
    return !body_.isEmpty();
  }

  /**
   * Check if the current clause is grounded.
   *
   * @return true iif the current clause is grounded.
   */
  public boolean isGrounded() {
    if (isGrounded_ == null) {
      if (!head_.isGrounded()) {
        isGrounded_ = false;
        return isGrounded_;
      }
      for (Literal literal : body_) {
        if (!literal.isGrounded()) {
          isGrounded_ = false;
          return isGrounded_;
        }
      }
      isGrounded_ = true;
    }
    return isGrounded_;
  }

  /**
   * Check if two clauses can be unified.
   *
   * @param clause clause.
   * @return true iif the two clauses can be unified.
   */
  public boolean isRelevant(Clause clause) {

    Preconditions.checkNotNull(clause, "clause should not be null");

    if (!head_.isRelevant(clause.head_)) {
      return false;
    }
    if (body_.size() != clause.body_.size()) {
      return false;
    }

    for (int i = 0; i < body_.size(); i++) {
      if (!body_.get(i).isRelevant(clause.body_.get(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * A clause is safe if every variable in its head is in its body.
   *
   * @return true iif the current clause is safe.
   */
  public boolean isSafe() {
    for (AbstractTerm term : head_.terms()) {
      if (!term.isConst()) {
        if (!bodyHasTerm(term)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Check if a list of literals is a prefix of the current rule body literals.
   *
   * @param literals a list of literals.
   * @return true iif the list of literals is a prefix of the current rule body literals, false otherwise.
   */
  public boolean hasPrefix(List<Literal> literals) {

    Preconditions.checkNotNull(literals, "literals should not be null");
    Preconditions.checkState(isRule(), "the clause should be a rule : %s", toString());

    if (body_.size() < literals.size()) {
      return false;
    }

    for (int i = 0; i < literals.size(); i++) {
      if (!body_.get(i).isRelevant(literals.get(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Check if a list of literals is a suffix of the current rule body literals.
   *
   * @param literals a list of literals.
   * @return true iif the list of literals is a suffix of the current rule body literals, false otherwise.
   */
  public boolean hasSuffix(List<Literal> literals) {

    Preconditions.checkNotNull(literals, "literals should not be null");
    Preconditions.checkState(isRule(), "the clause should be a rule : %s", toString());

    if (body_.size() < literals.size()) {
      return false;
    }

    int disp = body_.size() - literals.size();

    for (int i = disp; i < body_.size(); i++) {
      if (!body_.get(i).isRelevant(literals.get(i - disp))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Rename the variables in a clause. Every variable in the head is in the body, so the head can be ignored while
   * generating an environment.
   *
   * @return a new clause.
   */
  public Clause rename() {

    @com.google.errorprone.annotations.Var Map<Var, AbstractTerm> env = new HashMap<>();

    for (Literal literal : body_) {
      env = literal.shuffle(env);
    }
    return subst(env);
  }

  /**
   * Clause substitution in which the substitution is applied to each each literal that makes up the clause.
   *
   * @param env environment.
   * @return a new clause.
   */
  public Clause subst(Map<Var, AbstractTerm> env) {

    if (env == null || env.isEmpty()) {
      return this;
    }

    Literal head = head_.subst(env);
    List<Literal> body = new ArrayList<>(body_.size());

    for (Literal literal : body_) {
      body.add(literal.subst(env));
    }
    return new Clause(head, body);
  }

  /**
   * Resolve the first literal in a rule with a given literal. If the two literals unify, a new clause is generated that
   * has the same number of literals in the body.
   *
   * @param literal literal.
   * @return a new clause or null on error.
   */
  public Clause resolve(Literal literal) {

    Preconditions.checkNotNull(literal, "literal should not be null");
    Preconditions.checkArgument(literal.isGrounded(), "literal should be grounded : %s", literal);

    if (isFact()) {
      return null;
    }

    Literal first = body_.get(0);
    Map<Var, AbstractTerm> env = first.unify(literal.rename());

    if (env == null) {
      return null;
    }

    Literal head = head_.subst(env);
    List<Literal> body = new ArrayList<>(body_.size());
    body.add(literal);

    for (int i = 1; i < body_.size(); i++) {
      body.add(body_.get(i).subst(env));
    }
    return new Clause(head, body);
  }

  /**
   * Check if the current clause body contains a given term.
   *
   * @param term term.
   * @return true iif the current clause body contains the given term.
   */
  private boolean bodyHasTerm(AbstractTerm term) {

    Preconditions.checkNotNull(term, "term should not be null");

    for (Literal literal : body_) {
      if (literal.hasTerm(term)) {
        return true;
      }
    }
    return false;
  }
}
