package com.computablefacts.decima.problog;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.computablefacts.asterix.Generated;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CheckReturnValue;

/**
 * A subgoal is the item that is tabled by this algorithm.
 *
 * A subgoal has a literal, a set of facts, and an array of waiters. A waiter is a pair containing a
 * subgoal and a clause.
 *
 * All maps and lists should support concurrency because they will be updated and enumerated at the
 * same time by the tabling algorithm
 */
@CheckReturnValue
final public class Subgoal {

  private final Literal literal_;
  private final boolean computeProofs_;

  // Parent rules benefiting from this sub-goal resolution
  private final Set<Map.Entry<Subgoal, Clause>> waiters_ = ConcurrentHashMap.newKeySet();

  // Facts derived for this subgoal
  private final AbstractSubgoalFacts facts_;
  private final List<Clause> rules_ = new ArrayList<>();
  private final List<Clause> proofs_ = new ArrayList<>();

  public Subgoal(Literal literal, AbstractSubgoalFacts facts, boolean computeProofs) {

    Preconditions.checkNotNull(literal, "literal should not be null");
    Preconditions.checkNotNull(facts, "facts should not be null");

    literal_ = literal;
    facts_ = facts;
    computeProofs_ = computeProofs;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof Subgoal)) {
      return false;
    }
    Subgoal subgoal = (Subgoal) obj;
    return literal_.equals(subgoal.literal_);
  }

  @Override
  public int hashCode() {
    return literal_.hashCode();
  }

  @Generated
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("literal", literal_).add("facts", facts_)
        .omitNullValues().toString();
  }

  @Generated
  public Literal literal() {
    return literal_;
  }

  @Generated
  Set<Clause> rules() {
    return Sets.newHashSet(rules_);
  }

  @Generated
  void addRule(Clause rule) {
    if (rule != null) {
      rules_.add(rule);
    }
  }

  @Generated
  Iterator<Clause> facts() {
    return facts_.facts();
  }

  @Generated
  int nbFacts() {
    return facts_.size();
  }

  @Generated
  Set<Map.Entry<Subgoal, Clause>> waiters() {
    return waiters_;
  }

  void addWaiter(Subgoal subgoal, Clause clause) {

    Preconditions.checkNotNull(subgoal, "subgoal should not be null");
    Preconditions.checkNotNull(clause, "clause should not be null");
    Preconditions.checkArgument(clause.isRule(), "clause should be a rule : %s", clause.toString());

    waiters_.add(new AbstractMap.SimpleEntry<>(subgoal, clause));
  }

  /**
   * Try to add a fact to the subgoal.
   *
   * @param clause the fact to add.
   * @return true iif the fact is not already present and has been added, false otherwise.
   */
  boolean addFact(Clause clause) {

    Preconditions.checkNotNull(clause, "clause should not be null");
    Preconditions.checkArgument(clause.isFact(), "clause should be a fact : %s", clause.toString());

    boolean add = !facts_.contains(clause);

    if (add) {
      facts_.add(clause);
    }
    return add;
  }

  Collection<Clause> proofs() {
    return proofs_;
  }

  void push(Clause clause) {

    Preconditions.checkNotNull(clause, "clause should not be null");
    Preconditions.checkArgument(clause.isRule(), "clause should be a rule : %s", clause.toString());

    if (!computeProofs_) {
      return;
    }

    @com.google.errorprone.annotations.Var
    Clause prev = null;

    if (!proofs_.isEmpty()) {
      for (int i = proofs_.size() - 1; i >= 0; i--) {
        if (proofs_.get(i).head().isRelevant(clause.head())
            && proofs_.get(i).hasSuffix(clause.body())) {
          prev = proofs_.remove(i);
          break;
        }
      }
    }

    if (prev == null) {
      for (int i = rules_.size() - 1; i >= 0; i--) {
        if (rules_.get(i).head().isRelevant(clause.head())
            && rules_.get(i).hasSuffix(clause.body())) {
          prev = rules_.get(i);
          break;
        }
      }
    }

    Preconditions.checkState(prev != null, "prev should not be null");

    @com.google.errorprone.annotations.Var
    Clause proof = merge(clause, prev);

    if (!proof.isGrounded() && clause.isGrounded()) {

      int length = proof.body().size() - clause.body().size();
      List<Literal> prefix = proof.body().subList(0, length);

      // If the clause is grounded but the proof is not, we must backtrack in the tree
      for (int i = proofs_.size() - 1; i >= 0; i--) {
        if (proofs_.get(i).hasPrefix(prefix)) {

          List<Literal> body = new ArrayList<>(proofs_.get(i).body().subList(0, length));
          boolean isGrounded = body.stream().allMatch(Literal::isGrounded);

          Preconditions.checkState(isGrounded, "proof should be grounded : %s", proofs_.get(i));

          body.addAll(proof.body().subList(length, proof.body().size()));
          proof = new Clause(proof.head(), body);
          break;
        }
      }
    }

    proofs_.add(proof);
  }

  void pop(Clause clause) {

    Preconditions.checkNotNull(clause, "clause should not be null");

    if (!computeProofs_) {
      return;
    }

    // Deal with primitives
    if (!proofs_.isEmpty()) {

      List<Clause> removed = new ArrayList<>();

      for (int i = proofs_.size() - 1; i >= 0; i--) {
        if (proofs_.get(i).head().isRelevant(clause.head())
            && proofs_.get(i).hasSuffix(clause.body())) {
          removed.add(proofs_.get(i));
        }
      }

      proofs_.removeAll(removed);
    }

    // Deal with negation
    for (Map.Entry<Subgoal, Clause> waiter : waiters_) {

      List<Clause> removed = new ArrayList<>();
      List<Clause> stack = waiter.getKey().proofs_;

      for (int i = stack.size() - 1; i >= 0; i--) {
        if (stack.get(i).body().get(stack.get(i).body().size() - 1).isRelevant(clause.head())) {
          removed.add(stack.get(i));
        }
      }

      stack.removeAll(removed);
    }
  }

  private Clause merge(Clause cur, Clause prev) {

    Preconditions.checkNotNull(cur, "cur should not be null");
    Preconditions.checkArgument(cur.isRule(), "cur should be a rule : %s", cur.toString());
    Preconditions.checkNotNull(prev, "prev should not be null");
    Preconditions.checkArgument(prev.isRule(), "prev should be a rule : %s", cur.toString());
    Preconditions.checkArgument(cur.body().size() <= prev.body().size(),
        "mismatch in body length : %s vs %s", prev.body(), cur.body());

    // 1 - Build env
    Map<com.computablefacts.decima.problog.Var, AbstractTerm> env = prev.head().unify(cur.head());

    for (int i = 0; i < cur.body().size(); i++) {

      Literal lit1 = prev.body().get(prev.body().size() - cur.body().size() + i);
      Literal lit2 = cur.body().get(i);

      env.putAll(lit1.unify(lit2));
    }

    // 2 - Fill prev rule
    Clause merged = prev.subst(env);

    // 3 - Transfer probabilities to the right literals
    Literal head = merged.head();
    List<Literal> body =
        new ArrayList<>(merged.body().subList(0, merged.body().size() - cur.body().size()));

    if (!cur.body().isEmpty()) {
      body.add(cur.body().get(0));
      body.addAll(merged.body().subList(merged.body().size() - cur.body().size() + 1,
          merged.body().size()));
    }

    // 4 - Create a new cur
    return new Clause(head, body);
  }
}
