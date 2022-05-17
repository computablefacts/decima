package com.computablefacts.decima.problog;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import com.computablefacts.asterix.trie.Trie;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;

/**
 * See Mantadelis, Theofrastos & Janssens, Gerda. (2010). "Dedicated Tabling for a Probabilistic
 * Setting". Technical Communications of ICLP. 7. 124-133. 10.4230/LIPIcs.ICLP.2010.124. for
 * details.
 */
@CheckReturnValue
final public class ProofAssistant {

  // The intermediate proofs generated by the tabling algorithm
  private final Set<Literal> facts_;
  private final Set<Clause> rulesWithSubRules_;
  private final Set<Clause> rulesWithoutSubRules_;
  private final Map<Clause, Trie<Literal>> proofs_ = new HashMap<>();
  private final Set<Map.Entry<Integer, Set<Clause>>> factsInProofs_ = new HashSet<>();
  private final Set<Map.Entry<Integer, Set<Clause>>> rulesInProofs_ = new HashSet<>();

  public ProofAssistant(Collection<Subgoal> subgoals) {

    Preconditions.checkNotNull(subgoals, "subgoals should not be null");

    facts_ = subgoals.stream().filter(subgoal -> subgoal.proofs().isEmpty())
        .flatMap(subgoal -> Sets.newHashSet(subgoal.facts()).stream()).map(Clause::head)
        .collect(Collectors.toSet());

    rulesWithoutSubRules_ =
        subgoals.stream().flatMap(subgoal -> subgoal.proofs().stream()).filter(Clause::isGrounded)
            .filter(rule -> rule.body().stream()
                .allMatch(literal -> literal.predicate().isPrimitive() || facts_.contains(literal)))
            .collect(Collectors.toSet());

    rulesWithSubRules_ =
        subgoals.stream().flatMap(subgoal -> subgoal.proofs().stream()).filter(Clause::isGrounded)
            .filter(rule -> rule.body().stream().anyMatch(
                literal -> !literal.predicate().isPrimitive() && !facts_.contains(literal)))
            .collect(Collectors.toSet());
  }

  public List<String> tableOfProofs() {
    return Sets.union(factsInProofs_, rulesInProofs_).stream().flatMap(e -> {

      int depth = e.getKey();
      Set<Clause> clauses = e.getValue();

      return clauses.stream().map(c -> {
        if (c.isFact()) {
          if (c.head().predicate().isPrimitive()) {
            return "[prim] depth=" + depth + ", " + c;
          }
          return "[fact] depth=" + depth + ", " + c;
        }
        return "[rule] depth=" + depth + ", " + c;
      });
    }).sorted().collect(Collectors.toList());
  }

  public Set<Clause> proofs(Literal curLiteral) {
    return proofs(curLiteral, 0, new HashSet<>());
  }

  private Set<Clause> proofs(Literal curLiteral, int depth, Set<Clause> visited) {

    Preconditions.checkNotNull(curLiteral, "curLiteral should not be null");
    Preconditions.checkArgument(depth >= 0, "depth should be >= 0");
    Preconditions.checkNotNull(visited, "visited should not be null");

    if (facts_.stream().anyMatch(fact -> fact.isRelevant(curLiteral))) {
      return facts_.stream().filter(literal -> literal.isRelevant(curLiteral)).map(Clause::new)
          .peek(clause -> addFactAtDepth(clause, depth)).collect(Collectors.toSet());
    }

    Set<Clause> rulesWithSubRules =
        rulesWithSubRules_.stream().filter(clause -> !visited.contains(clause))
            .filter(clause -> clause.head().isRelevant(curLiteral)).collect(Collectors.toSet());

    Set<Clause> rulesWithoutSubRules = rulesWithoutSubRules_.stream()
        .filter(clause -> clause.head().isRelevant(curLiteral)).collect(Collectors.toSet());

    Set<Clause> proofs = new HashSet<>();
    List<Clause> rules = new ArrayList<>();
    rules.addAll(rulesWithoutSubRules);
    rules.addAll(rulesWithSubRules);

    for (Clause rule : rules) {

      Set<List<Literal>> newBodies = new HashSet<>();

      for (Literal literal : rule.body()) {

        Set<Literal> facts =
            facts_.stream().filter(fact -> fact.isRelevant(literal)).collect(Collectors.toSet());
        Set<List<Literal>> newNewBodies = new HashSet<>();

        if (literal.predicate().isPrimitive() /* function */) {
          if (newBodies.isEmpty()) {
            newNewBodies.add(Lists.newArrayList(literal));
          } else {
            for (List<Literal> body : newBodies) {
              List<Literal> nb = new ArrayList<>(body);
              nb.add(literal);
              newNewBodies.add(nb);
            }
          }
          addFactAtDepth(new Clause(literal), depth);
        } else if (!facts.isEmpty() /* fact */) {
          for (Literal fact : facts) {
            if (newBodies.isEmpty()) {
              newNewBodies.add(Lists.newArrayList(fact));
            } else {
              for (List<Literal> body : newBodies) {
                List<Literal> nb = new ArrayList<>(body);
                nb.add(literal);
                newNewBodies.add(nb);
              }
            }
            addFactAtDepth(new Clause(literal), depth);
          }
        } else { /* rule */

          @Var
          Set<Clause> proofz =
              proofs_.entrySet().stream().filter(e -> e.getKey().head().isRelevant(literal))
                  .flatMap(
                      e -> e.getValue().paths().stream().map(path -> new Clause(literal, path)))
                  .collect(Collectors.toSet());

          if (proofz.isEmpty()) {
            Set<Clause> newVisited = new HashSet<>(visited);
            newVisited.add(rule);
            proofz = proofs(literal, depth + 1, newVisited);
          }

          Preconditions.checkState(!proofz.isEmpty(), "goal cannot be proven : %s", rule);

          // --[ BEGIN SHENANIGANS ]--
          // Here, we have two kinds of proofs : 1/ proofs where all the body literals have a
          // probability of 1 and 2/ proofs where at least one of the body literals have a
          // probability different from 1. If more than one proof are in 1/, only keep the shortest
          // ones. This reduction in the number of proofs should have no impact on the computation
          // performed by the ProbabilityEstimator class.

          Set<Clause> ones = new HashSet<>();
          Set<Clause> others = new HashSet<>();

          for (Clause proof : proofz) {

            Preconditions.checkState(proof.isRule(), "proof should be a rule : %s", proof);

            if (proof.body().stream().map(Literal::probability)
                .allMatch(prob -> BigDecimal.ONE.compareTo(prob) == 0)) {
              ones.add(proof);
            } else {
              others.add(proof);
            }
          }

          OptionalInt minSize = ones.stream().mapToInt(proof -> proof.body().size()).min();
          proofz = Sets.union(ones.stream().filter(p -> p.body().size() == minSize.getAsInt())
              .collect(Collectors.toSet()), others);

          // --[ END SHENANIGANS ]--

          for (Clause proof : proofz) {
            if (newBodies.isEmpty()) {
              newNewBodies.add(new ArrayList<>(proof.body()));
            } else {
              for (List<Literal> body : newBodies) {
                List<Literal> nb = new ArrayList<>(body);
                nb.addAll(proof.body());
                newNewBodies.add(nb);
              }
            }
          }
        }

        newBodies.clear();
        newBodies.addAll(newNewBodies);
      }
      if (!proofs_.containsKey(rule)) {
        proofs_.put(rule, new Trie<>());
      }
      for (List<Literal> body : newBodies) {
        proofs.add(new Clause(rule.head(), body));
        proofs_.get(rule).insert(body);
      }
      addRuleAtDepth(rule, depth);
    }
    return proofs;
  }

  private void addFactAtDepth(Clause clause, int depth) {

    Optional<Map.Entry<Integer, Set<Clause>>> opt =
        factsInProofs_.stream().filter(r -> r.getKey().equals(depth)).findFirst();

    if (opt.isPresent()) {
      opt.get().getValue().add(clause);
    } else {
      Set<Clause> set = new HashSet<>();
      set.add(clause);
      factsInProofs_.add(new AbstractMap.SimpleImmutableEntry<>(depth, set));
    }
  }

  private void addRuleAtDepth(Clause clause, int depth) {

    Optional<Map.Entry<Integer, Set<Clause>>> opt =
        rulesInProofs_.stream().filter(r -> r.getKey().equals(depth)).findFirst();

    if (opt.isPresent()) {
      opt.get().getValue().add(clause);
    } else {
      Set<Clause> set = new HashSet<>();
      set.add(clause);
      rulesInProofs_.add(new AbstractMap.SimpleImmutableEntry<>(depth, set));
    }
  }
}
