package com.computablefacts.decima.problog;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import com.computablefacts.decima.robdd.Pair;
import com.computablefacts.nona.Function;
import com.computablefacts.nona.functions.comparisonoperators.Equal;
import com.computablefacts.nona.functions.csvoperators.CsvValue;
import com.computablefacts.nona.functions.stringoperators.StrLength;
import com.computablefacts.nona.functions.stringoperators.ToInteger;
import com.computablefacts.nona.functions.stringoperators.ToLowerCase;
import com.computablefacts.nona.functions.stringoperators.ToUpperCase;
import com.computablefacts.nona.types.BoxedType;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

/**
 * This class allows us to be agnostic from the storage layer. It is used to assert facts and rules.
 */
public abstract class AbstractKnowledgeBase {

  private final RandomString randomString_ = new RandomString(7);
  private final Map<String, Function> definitions_ = new ConcurrentHashMap<>();

  public AbstractKnowledgeBase() {
    setDefinitions();
  }

  @Override
  public String toString() {

    StringBuilder builder = new StringBuilder();

    for (Clause fact : facts()) {
      builder.append(fact.toString());
      builder.append(".\n");
    }

    for (Clause rule : rules()) {
      builder.append(rule.toString());
      builder.append(".\n");
    }

    return builder.toString();
  }

  /**
   * Add a new fact or rule to the database. There are two assumptions here :
   *
   * <ul>
   * <li>facts and rules predicates must not overlap ;</li>
   * <li>a rule body literals should not have probabilities attached.</li>
   * </ul>
   *
   * @param clause fact or rule.
   */
  public void azzert(Clause clause) {

    Preconditions.checkNotNull(clause, "clause should not be null");
    Preconditions.checkArgument(clause.isSafe(), "clause should be safe : %s", clause.toString());

    if (clause.head().predicate().isPrimitive()) {
      return; // Ignore assertions for primitives
    }

    Clause newClause;

    if (clause.isFact()) {
      newClause = clause;
    } else {

      // Remove probability from the rule head (otherwise it is a no-op)
      Pair<Clause, Clause> clauses = rewriteRuleHead(clause);

      if (clauses.u != null) {
        azzert(clauses.u); // Assert created fact (if any)
      }

      newClause = clauses.t; // Assert rewritten rule
    }

    Literal head = newClause.head();
    BigDecimal probability = head.probability();

    Preconditions.checkState(!BigDecimal.ZERO.equals(probability),
        "head probability must be != 0.0 : %s", newClause.toString());

    if (clause.isFact()) {
      azzertFact(newClause);
    } else {

      Preconditions.checkState(BigDecimal.ONE.equals(probability),
          "rule head should not have a probability attached : %s", newClause.toString());

      for (int i = 0; i < newClause.body().size(); i++) {

        Literal literal = newClause.body().get(i);

        Preconditions.checkState(BigDecimal.ONE.equals(literal.probability()),
            "body literals should not have probabilities attached : %s", newClause.toString());
      }

      azzertRule(newClause);
    }
  }

  /**
   * Adds new facts or rules to the database.
   *
   * @param clauses clauses.
   */
  public void azzert(Set<Clause> clauses) {

    Preconditions.checkNotNull(clauses, "clauses should not be null");

    clauses.forEach(this::azzert);
  }

  protected abstract void azzertFact(@NotNull Clause fact);

  protected abstract void azzertRule(@NotNull Clause rule);

  /**
   * Returns matching clauses (e.g. facts or rules) for a literal query.
   *
   * @param literal literal.
   * @return matching clauses.
   */
  public Set<Clause> clauses(Literal literal) {

    Preconditions.checkNotNull(literal, "literal should not be null");

    Set<Clause> facts = facts(literal);

    if (!facts.isEmpty()) {
      return facts;
    }
    return rules(literal);
  }

  protected abstract Set<Clause> facts(@NotNull Literal literal);

  protected abstract Set<Clause> rules(@NotNull Literal literal);

  public abstract Set<Clause> facts();

  public abstract Set<Clause> rules();

  /**
   * Return the list of available definitions for primitives.
   *
   * @return list of definitions.
   */
  public Map<String, Function> definitions() {
    return definitions_;
  }

  /**
   * Set the list of available primitives.
   */
  protected void setDefinitions() {

    Map<String, Function> defs = Function.definitions();

    for (Map.Entry<String, Function> def : defs.entrySet()) {
      definitions_.put("FN_" + def.getKey(), def.getValue());
    }

    // TODO : legacy functions. Remove ASAP.
    definitions_.put("FN_EQ", new Equal());
    definitions_.put("FN_CSV_VALUE", new CsvValue());
    definitions_.put("FN_LOWER_CASE", new ToUpperCase());
    definitions_.put("FN_UPPER_CASE", new ToLowerCase());
    definitions_.put("FN_INT", new ToInteger());
    definitions_.put("FN_LENGTH", new StrLength());

    // Special operator. See {@link Literal#execute} for details.
    definitions_.put("FN_EXIST_IN_KB", new Function("EXISTINKB") {

      @Override
      protected boolean isCacheable() {

        // The function's cache is shared between multiple processes
        return false;
      }

      @Override
      public BoxedType evaluate(List<BoxedType> parameters) {

        Preconditions.checkArgument(parameters.size() > 1,
            "EXISTINKB takes at least two parameters.");

        String predicate = parameters.get(0).asString();
        List<String> terms = parameters.subList(1, parameters.size()).stream().map(bt -> {

          String term = bt.asString();

          if (bt.isNumber() || bt.isBoolean()) {
            return term;
          }
          return "_".equals(term) ? "_" : "\"" + term + "\"";
        }).collect(Collectors.toList());

        String fact = predicate + "(" + Joiner.on(',').join(terms) + ")?";
        Literal query = Parser.parseQuery(fact);
        Set<Clause> clauses = clauses(query);
        boolean matchAtLeastOneFact = clauses.stream().anyMatch(Clause::isFact);

        return BoxedType.create(matchAtLeastOneFact);
      }
    });
  }

  /**
   * Perform the following actions :
   *
   * <ul>
   * <li>Remove probability from the clause head ;</li>
   * <li>Create a random fact name with the clause head probability ;</li>
   * <li>Add a reference to the newly created fact in the clause body.</li>
   * </ul>
   *
   * @param clause clause.
   * @return a {@link Pair} with {@link Pair#t} containing the rewritten clause and {@link Pair#u}
   *         the newly created fact.
   */
  protected Pair<Clause, Clause> rewriteRuleHead(Clause clause) {

    Preconditions.checkNotNull(clause, "clause should not be null");
    Preconditions.checkArgument(clause.isRule(), "clause should be a clause : %s", clause);

    Literal head = clause.head();

    if (BigDecimal.ONE.compareTo(head.probability()) == 0) {
      return new Pair<>(clause, null);
    }

    String predicate = head.predicate().name();
    BigDecimal probability = head.probability();

    // Create fact
    String newPredicate = "proba_" + randomString_.nextString().toLowerCase();
    Literal newLiteral = new Literal(probability,
        (head.predicate().isNegated() ? "~" : "") + newPredicate, new Const(true));
    Clause newFact = new Clause(newLiteral);

    // Rewrite clause
    Literal newHead = new Literal(predicate, clause.head().terms());
    List<Literal> newBody = new ArrayList<>(clause.body());
    newBody.add(new Literal(newPredicate, new Const(true)));
    Clause newRule = new Clause(newHead, newBody);

    return new Pair<>(newRule, newFact);
  }

  /**
   * Compute the intersection of two sets.
   *
   * @param set1 first set.
   * @param set2 second set.
   * @param <T> element type.
   * @return intersection of set1 with set2.
   */
  protected <T> Set<T> intersection(Set<T> set1, Set<T> set2) {

    Set<T> minCardinalitySet;
    Set<T> maxCardinalitySet;
    Set<T> intersection = new HashSet<>();

    if (set1.size() <= set2.size()) {
      minCardinalitySet = set1;
      maxCardinalitySet = set2;
    } else {
      minCardinalitySet = set2;
      maxCardinalitySet = set1;
    }

    for (T element : minCardinalitySet) {
      if (maxCardinalitySet.contains(element)) {
        intersection.add(element);
      }
    }
    return intersection;
  }
}
