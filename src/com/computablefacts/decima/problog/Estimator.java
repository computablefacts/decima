package com.computablefacts.decima.problog;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.computablefacts.decima.robdd.BddManager;
import com.computablefacts.decima.robdd.BddNode;
import com.computablefacts.nona.helpers.RandomString;
import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Var;

/**
 * See Theofrastos Mantadelis and Gerda Janssens (2010). "Nesting Probabilistic Inference" for
 * details.
 */
public class Estimator {

  private final RandomString randomString_ = new RandomString(7);
  private final Set<Clause> proofs_;

  public Estimator(Set<Clause> proofs) {

    Preconditions.checkNotNull(proofs, "proofs should not be null");
    Preconditions.checkArgument(proofs.stream().allMatch(Clause::isGrounded),
        "All proofs should be grounded");

    proofs_ = proofs;
  }

  @Beta
  public Map<Clause, BigDecimal> probabilities() {
    return probabilities(5);
  }

  /**
   * Compute the probability associated with each fact.
   *
   * @param nbSignificantDigits number of significant digits.
   * @return map between facts and probabilities.
   */
  public Map<Clause, BigDecimal> probabilities(int nbSignificantDigits) {

    if (proofs_.isEmpty()) {
      return new HashMap<>();
    }

    Map<Clause, BigDecimal> probabilities = new HashMap<>();

    for (Clause clause : proofs_) {

      Clause fact = new Clause(clause.head());

      if (!probabilities.containsKey(fact)) {
        probabilities.put(fact, probability(fact, nbSignificantDigits));
      }
    }
    return probabilities;
  }

  @Beta
  public BigDecimal probability(Literal literal) {

    Preconditions.checkNotNull(literal, "literal should not be null");

    return probability(literal, 5);
  }

  @Beta
  public BigDecimal probability(Literal literal, int nbSignificantDigits) {

    Preconditions.checkNotNull(literal, "literal should not be null");

    return probability(new Clause(literal), nbSignificantDigits);
  }

  /**
   * Compute the probability associated with a given clause.
   *
   * @param clause clause.
   * @param nbSignificantDigits number of significant digits.
   * @return probability.
   */
  public BigDecimal probability(Clause clause, int nbSignificantDigits) {

    Preconditions.checkNotNull(clause, "clause should not be null");
    Preconditions.checkArgument(clause.isFact(), "clause should be a fact : %s", clause.toString());
    Preconditions.checkArgument(nbSignificantDigits > 0, "nbSignificantDigits should be > 0");

    if (proofs_.isEmpty()) {
      return BigDecimal.ZERO;
    }

    Estimator estimator = new Estimator(
        proofs_.stream().filter(p -> p.isGrounded() && p.head().tag().equals(clause.head().tag()))
            .collect(Collectors.toSet()));
    int newScale =
        nbSignificantDigits - estimator.probability().precision() + estimator.probability().scale();

    return estimator.probability().setScale(newScale, RoundingMode.HALF_UP);
  }

  private BigDecimal probability() {

    if (proofs_.isEmpty()) {
      return BigDecimal.ZERO;
    }

    Preconditions.checkArgument(
        proofs_.stream().map(p -> p.head().tag()).collect(Collectors.toSet()).size() == 1,
        "All proofs should be about the same fact");

    BddManager mgr = new BddManager(10);
    BiMap<BddNode, Literal> bddVars = HashBiMap.create();

    Set<Clause> newProofs = proofs_.stream().map(this::rewriteRuleBody).collect(Collectors.toSet());

    newProofs.stream()
        .flatMap(p -> p.isFact() ? ImmutableList.of(p.head()).stream() : p.body().stream())
        .distinct().forEach(literal -> {

          // Literals with probability of 1 do not contribute to the final score
          if (BigDecimal.ONE.compareTo(literal.probability()) != 0) {
            bddVars.put(mgr.create(mgr.createVariable(), mgr.One, mgr.Zero), literal);
          }
        });

    List<BddNode> trees = new ArrayList<>();

    for (Clause proof : newProofs) {

      List<Literal> body = proof.isFact() ? ImmutableList.of(proof.head()) : proof.body();
      BddNode bddNode = and(mgr, bddVars.inverse(), body);

      if (bddNode != null) {
        trees.add(bddNode);
      }
    }

    if (trees.isEmpty()) {
      return BigDecimal.ONE;
    }
    return probability(bddVars, or(mgr, trees));
  }

  private BigDecimal probability(BiMap<BddNode, Literal> bddVars, BddNode node) {

    Preconditions.checkNotNull(bddVars, "bddVars should not be null");
    Preconditions.checkNotNull(node, "node should not be null");

    if (node.isOne()) {
      return BigDecimal.ONE;
    }
    if (node.isZero()) {
      return BigDecimal.ZERO;
    }

    BigDecimal probH = probability(bddVars, node.high());
    BigDecimal probL = probability(bddVars, node.low());

    Optional<Literal> fact = bddVars.entrySet().stream()
        .filter(e -> e.getKey().index() == node.index()).map(Map.Entry::getValue).findFirst();

    BigDecimal probability = fact.get().probability();

    return probability.multiply(probH).add(BigDecimal.ONE.subtract(probability).multiply(probL));
  }

  private BddNode and(BddManager mgr, BiMap<Literal, BddNode> bddVars, List<Literal> body) {

    Preconditions.checkNotNull(mgr, "mgr should not be null");
    Preconditions.checkNotNull(bddVars, "bddVars should not be null");
    Preconditions.checkNotNull(body, "body should not be null");
    Preconditions.checkArgument(!body.isEmpty(), "body should not be empty");

    @com.google.errorprone.annotations.Var
    BddNode bdd = null;

    for (int i = 0; i < body.size(); i++) {

      Literal literal = body.get(i);

      // Literals with probability of 1 do not contribute to the final score
      if (BigDecimal.ONE.compareTo(literal.probability()) != 0) {
        if (bdd == null) {
          bdd = bddVars.get(literal);
        } else {
          bdd = mgr.and(bdd, bddVars.get(literal));
        }
      }
    }
    return bdd;
  }

  private BddNode or(BddManager mgr, List<BddNode> trees) {

    Preconditions.checkNotNull(mgr, "mgr should not be null");
    Preconditions.checkNotNull(trees, "trees should not be null");
    Preconditions.checkArgument(!trees.isEmpty(), "trees should not be empty");

    @Var
    BddNode bdd = null;

    for (int i = 0; i < trees.size(); i++) {
      if (bdd == null) {
        bdd = trees.get(i);
      } else {
        bdd = mgr.or(bdd, trees.get(i));
      }
    }
    return bdd;
  }

  /**
   * Replace all probabilistic literals created by
   * {@link AbstractKnowledgeBase#rewriteRuleHead(Clause)} with a unique literal with the same
   * probability.
   *
   * @param clause fact or rule.
   * @return rewritten clause.
   */
  private Clause rewriteRuleBody(Clause clause) {

    Preconditions.checkNotNull(clause, "clause should not be null");

    if (clause.isFact()) {
      return clause;
    }

    Literal head = clause.head();
    List<Literal> body = new ArrayList<>(clause.body().size());

    for (Literal literal : clause.body()) {
      if (!literal.predicate().baseName().startsWith("proba_")) {
        body.add(literal);
      } else {
        String predicate = randomString_.nextString().toLowerCase();
        body.add(new Literal(literal.probability(),
            (literal.predicate().isNegated() ? "~" : "") + predicate, literal.terms()));
      }
    }
    return new Clause(head, body);
  }
}
