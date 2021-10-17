package com.computablefacts.decima.problog;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.computablefacts.decima.robdd.Pair;
import com.computablefacts.decima.trie.Trie;
import com.computablefacts.nona.Generated;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;

/**
 * A subgoal is the item that is tabled by this algorithm.
 *
 * A subgoal has a literal, a set of facts, and an array of waiters. A waiter is a pair containing a
 * subgoal and a clause.
 */
@CheckReturnValue
final public class Subgoal {

  // KB rules benefiting from this sub-goal resolution
  public final Trie<Literal> trie_ = new Trie<>();
  public final Set<Clause> parents_ = new HashSet<>();
  public final boolean trackRules_;

  // All maps and lists should support concurrency because they will be updated and enumerated at
  // the same time by the tabling algorithm
  private final Literal literal_;

  // Parent rules benefiting from this sub-goal resolution
  private final Set<Map.Entry<Subgoal, Clause>> waiters_ = ConcurrentHashMap.newKeySet();

  // Facts derived for this subgoal
  private final AbstractSubgoalFacts facts_;

  public Subgoal(Literal literal, AbstractSubgoalFacts facts, boolean trackRules) {

    Preconditions.checkNotNull(literal, "literal should not be null");
    Preconditions.checkNotNull(facts, "facts should not be null");

    literal_ = literal;
    facts_ = facts;
    trackRules_ = trackRules;
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
        .add("parents", parents_).omitNullValues().toString();
  }

  @Generated
  public Literal literal() {
    return literal_;
  }

  Iterator<Clause> facts() {
    return facts_.facts();
  }

  int nbFacts() {
    return facts_.nbFacts();
  }

  Set<Map.Entry<Subgoal, Clause>> waiters() {
    return waiters_;
  }

  void addWaiter(Subgoal subgoal, Clause clause) {

    Preconditions.checkNotNull(subgoal, "subgoal should not be null");
    Preconditions.checkNotNull(clause, "clause should not be null");
    Preconditions.checkArgument(clause.isRule(), "clause should be a rule : %s", clause.toString());

    waiters_.add(new AbstractMap.SimpleEntry<>(subgoal, clause));
  }

  Set<Clause> groundedRules() {
    return new HashSet<>(parents_);
  }

  boolean hasFacts() {
    return !facts_.isEmpty();
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

    boolean add = !containsFact(clause);

    if (add) {
      facts_.add(clause);
    }
    return add;
  }

  boolean hasRules() {
    return !parents_.isEmpty();
  }

  /**
   * Remove all rules where one of the body literal matches a given literal.
   *
   * @param literal literal.
   */
  void cleanup(Literal literal) {

    Preconditions.checkNotNull(literal, "literal should not be null");

    if (!trackRules_) {
      return;
    }

    trie_.remove(lit -> {
      if (lit.isRelevant(literal)) {

        @Var
        int nbMatch = 0;
        @Var
        int nbNoMatch = 0;

        for (int i = 0; i < literal.terms().size(); i++) {

          AbstractTerm term1 = literal.terms().get(i);
          AbstractTerm term2 = lit.terms().get(i);

          // Match const against const and var against var
          if (term1.getClass() == term2.getClass()) {
            nbMatch++;
          } else {
            nbNoMatch++;
          }
        }

        return nbMatch != 0 && nbNoMatch == 0;
      }
      return false;
    });
  }

  /**
   * Get the rule associated with a given clause.
   *
   * @param clause clause.
   * @return matching rule or null.
   */
  void update(Clause clause) {

    Preconditions.checkNotNull(clause, "clause should not be null");
    Preconditions.checkArgument(clause.isRule(), "clause should be a rule : %s", clause.toString());

    if (!trackRules_) {
      return;
    }

    // Due to how the tabling algorithm works, a good candidate rule to update is such that :
    // - the clause body MUST BE a suffix of the candidate rule body
    // - the body literals in [0, candidate.body.size - clause.body.size[ of the candidate rule MUST
    // BE grounded

    List<Pair<Integer, Clause>> parents = new ArrayList<>();
    Function<Stack<Pair<Integer, Literal>>, Boolean> fnSkip = input -> {

      if (input.size() == 1) { // At depth 0 of the trie is the clause head
        return !input.get(0).u.isRelevant(clause.head());
      }
      return false;
    };
    Function<Stack<Pair<Integer, Literal>>, Void> fnProcess = input -> {

      int max = input.stream().mapToInt(p -> p.t).max().getAsInt();

      if (!parents.isEmpty() && parents.get(0).t >= max) {
        return null;
      }

      Literal head = input.get(0).u;
      List<Literal> body = input.stream().skip(1).map(p -> p.u).collect(Collectors.toList());
      Clause candidate = new Clause(head, body);

      if (body.size() < clause.body().size()) {
        return null;
      }

      @Var
      boolean isOk = true;

      for (int k = 0; k < body.size(); k++) {

        Literal lit1 = body.get(k);

        if (k < (body.size() - clause.body().size())) {

          // Check if the candidate rule prefix is grounded
          if (!lit1.isGrounded()) {
            isOk = false;
            break;
          }
        } else {

          // Check if the candidate rule suffix is relevant
          Literal lit2 = clause.body().get(k - (body.size() - clause.body().size()));

          if (!lit1.isRelevant(lit2)) {
            isOk = false;
            break;
          }
        }
      }

      if (isOk) {
        parents.clear();
        parents.add(new Pair<>(max, candidate));
      }
      return null;
    };

    trie_.traverse(fnSkip, fnProcess);

    if (parents.isEmpty()) {

      // Quand on a une règle du type (R -> PREFIX LITERAL SUFFIX), et que :
      //
      // - le solveur a résolu chacun des litéraux de PREFIX
      // - le solveur vient de résoudre LITERAL et cherche à mettre à jour la règle parent
      // - le solveur a supprimé la règle parent car la résolution d'une autre règle a flaggé SUFFIX
      // comme contenant un litéral invalide
      //
      // Alors la fusion de LITERAL avec la règle parent (inexistante) échoue.
      return;
    }

    // Only apply substitution to the most recent parent
    Clause parent = parents.get(0).u;
    Clause newParent = merge(clause, parent);

    List<Literal> list = new ArrayList<>(newParent.body().size() + 1);
    list.add(newParent.head());
    list.addAll(newParent.body());

    trie_.insert(list);

    if (newParent.isGrounded() && clause.body().size() == 1) {
      parents_.add(newParent); // Here, we evaluated the last body literal of a rule
    }
  }

  private Clause merge(Clause clause, Clause candidate) {

    Preconditions.checkNotNull(clause, "clause should not be null");
    Preconditions.checkArgument(clause.isRule(), "clause should be a rule : %s", clause.toString());
    Preconditions.checkNotNull(candidate, "candidate should not be null");
    Preconditions.checkArgument(candidate.isRule(), "candidate should be a rule : %s",
        clause.toString());

    // 1 - Build env
    Map<com.computablefacts.decima.problog.Var, AbstractTerm> env =
        candidate.head().unify(clause.head());

    for (int i = 0; i < clause.body().size(); i++) {

      Literal lit1 = candidate.body().get(candidate.body().size() - clause.body().size() + i);
      Literal lit2 = clause.body().get(i);

      env.putAll(lit1.unify(lit2));
    }

    // 2 - Fill candidate rule
    Clause merged = candidate.subst(env);

    // 3 - Transfer probabilities to the right literals
    Literal head = merged.head();
    List<Literal> body =
        new ArrayList<>(merged.body().subList(0, merged.body().size() - clause.body().size()));

    if (!clause.body().isEmpty()) {
      body.add(clause.body().get(0));
      body.addAll(merged.body().subList(merged.body().size() - clause.body().size() + 1,
          merged.body().size()));
    }

    // 4 - Create a new clause
    return new Clause(head, body);
  }

  private boolean containsFact(Clause clause) {

    Preconditions.checkNotNull(clause, "clause should not be null");
    Preconditions.checkArgument(clause.isFact(), "clause should be a fact : %s", clause.toString());

    return facts_.contains(clause);
  }
}
