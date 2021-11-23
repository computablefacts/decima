package com.computablefacts.decima.problog;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.computablefacts.asterix.Generated;
import com.computablefacts.asterix.trie.Trie;
import com.computablefacts.logfmt.LogFormatter;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;

/**
 * Tabling algorithm in a non probabilistic setting :
 *
 * <ul>
 * <li>Chen, Weidong et al. "Efficient Top-Down Computation of Queries under the Well-Founded
 * Semantics." J. Log. Program. 24 (1995): 161-199.</li>
 * <li>Chen, Weidong &amp; Warren, David. (1996). Tabled evaluation with delaying for general logic
 * programs. J. ACM. 43. 20-74. 10.1145/227595.227597.</li>
 * </ul>
 *
 * Tabling algorithm in a probabilistic setting :
 *
 * <ul>
 * <li>Luc De Raedt, Angelika Kimmig (2015). "Probabilistic (logic) programming concepts"</li>
 * <li>Mantadelis, Theofrastos &amp; Janssens, Gerda. (2010). "Dedicated Tabling for a Probabilistic
 * Setting.". Technical Communications of ICLP. 7. 124-133. 10.4230/LIPIcs.ICLP.2010.124.</li>
 * </ul>
 */
@CheckReturnValue
final public class Solver {

  private static final Logger logger_ = LoggerFactory.getLogger(Solver.class);

  private final AbstractKnowledgeBase kb_;
  private final Map<String, Subgoal> subgoals_;
  private final Function<Literal, Subgoal> newSubgoal_;

  private Subgoal root_ = null;
  private int maxSampleSize_ = -1;

  public Solver(AbstractKnowledgeBase kb, boolean computeProofs) {
    this(kb, literal -> new Subgoal(literal, new InMemorySubgoalFacts(), computeProofs));
  }

  public Solver(AbstractKnowledgeBase kb, Function<Literal, Subgoal> newSubgoal) {

    Preconditions.checkNotNull(kb, "kb should not be null");
    Preconditions.checkNotNull(newSubgoal, "newSubgoal should not be null");

    kb_ = kb;
    subgoals_ = new ConcurrentHashMap<>();
    newSubgoal_ = newSubgoal;
  }

  /**
   * Return the number of subgoals.
   *
   * @return the number of subgoals.
   */
  @Generated
  public int nbSubgoals() {
    return subgoals_.size();
  }

  /**
   * First, sets up and calls the subgoal search procedure. Then, extracts the answers and unfold
   * the proofs. In order to work, subgoals must track rules i.e. {@code computeProofs = true}.
   *
   * @param query goal.
   * @return proofs.
   */
  public Set<Clause> proofs(Literal query) {

    Preconditions.checkNotNull(query, "query should not be null");

    root_ = newSubgoal_.apply(query);
    subgoals_.put(query.tag(), root_);

    search(root_);

    ProofAssistant assistant = new ProofAssistant(subgoals_.values());
    return assistant.proofs(root_.literal());
  }

  /**
   * Map each proof to a trie.
   *
   * @param query goal.
   * @return proofs.
   */
  public Map<Literal, Trie<Literal>> tries(Literal query) {

    Preconditions.checkNotNull(query, "query should not be null");

    Set<Clause> proofs = proofs(query);
    Map<Literal, Trie<Literal>> tries = new HashMap<>();

    for (Clause proof : proofs) {
      if (!tries.containsKey(proof.head())) {
        tries.put(proof.head(), new Trie<>());
      }
      tries.get(proof.head()).insert(proof.body());
    }
    return tries;
  }

  /**
   * First, sets up and calls the subgoal search procedure. Then, extracts the answers but do not
   * unfold the proofs.
   *
   * @param query goal.
   * @return facts answering the query.
   */
  public Iterator<Clause> solve(Literal query) {
    return solve(query, -1);
  }

  /**
   * First, sets up and calls the subgoal search procedure. Then, extracts the answers but do not
   * unfold the proofs.
   *
   * @param query goal.
   * @param maxSampleSize stops the solver after the goal reaches this number of solutions or more.
   *        If this number is less than or equals to 0, returns all solutions.
   * @return facts answering the query.
   */
  public Iterator<Clause> solve(Literal query, int maxSampleSize) {

    Preconditions.checkNotNull(query, "query should not be null");

    root_ = newSubgoal_.apply(query);
    subgoals_.put(query.tag(), root_);
    maxSampleSize_ = maxSampleSize <= 0 ? -1 : maxSampleSize;

    search(root_);
    return root_.facts();
  }

  /**
   * Check if the number of samples asked by the caller has been reached.
   *
   * @return true iif the number of samples has been reached, false otherwise.
   */
  private boolean maxSampleSizeReached() {
    return maxSampleSize_ > 0 && root_ != null && root_.nbFacts() >= maxSampleSize_;
  }

  /**
   * Search for derivations of the literal associated with {@param subgoal}.
   *
   * @param subgoal subgoal.
   */
  private void search(Subgoal subgoal) {

    Preconditions.checkNotNull(subgoal, "subgoal should not be null");

    Literal literal = subgoal.literal();
    Predicate predicate = literal.predicate();

    Preconditions.checkState(!predicate.isPrimitive(), "predicate should not be a primitive : %s",
        literal.toString());

    if (predicate.isNegated()) {

      Preconditions.checkState(literal.isSemiGrounded(), "negated clauses should be grounded : %s",
          literal.toString());

      // Evaluate the positive version of the rule (i.e. negation as failure)
      Literal base = new Literal(predicate.baseName(), literal.terms());
      Subgoal sub = newSubgoal_.apply(base);

      subgoals_.put(sub.literal().tag(), sub);

      search(sub);

      String newPredicate = literal.predicate().name();
      List<AbstractTerm> newTerms = literal.terms().stream()
          .map(t -> t.isConst() ? t : new Const("_")).collect(Collectors.toList());
      Iterator<Clause> facts = sub.facts();

      if (!facts.hasNext()) {

        // The positive version of the rule yielded no fact
        // => resume the current rule evaluation
        fact(subgoal, new Clause(new Literal(newPredicate, newTerms)));
      } else {

        // The positive version of the rule yielded at least one fact
        // => fail the current rule evaluation iif the probability of the produced facts is 0
        while (facts.hasNext()) {

          Clause fact = facts.next();

          if (fact.head().isRelevant(base)) {
            if (sub.rules().isEmpty()) {

              // Negate a probabilistic fact
              Clause newFact =
                  new Clause(new Literal(BigDecimal.ONE.subtract(fact.head().probability()),
                      newPredicate, newTerms));

              if (!BigDecimal.ZERO.equals(newFact.head().probability())) {
                fact(subgoal, newFact);
              } else {
                subgoal.pop(new Clause(literal));
              }
            } else {

              // Negate a probabilistic rule
              // i.e. if (q :- a, b) then ~q is rewritten as (~q :- ~a) or (~q :- ~b)
              for (Clause rule : sub.rules()) {
                for (Literal lit : rule.body()) {
                  if (!lit.predicate().isPrimitive()) {

                    Clause newRule = new Clause(new Literal(newPredicate, newTerms),
                        Lists.newArrayList(lit.negate()));

                    subgoal.addRule(newRule);
                  }
                }
              }

              List<Clause> rules =
                  sub.proofs().stream().filter(Clause::isGrounded).collect(Collectors.toList());

              for (Clause rule : rules) {
                for (Literal lit : rule.body()) {
                  if (!lit.predicate().isPrimitive()) {

                    Clause newLiteral = new Clause(new Literal(newPredicate, newTerms),
                        Lists.newArrayList(lit.negate()));

                    rule(subgoal, newLiteral, false);
                  }
                }
              }
            }
          }
          if (maxSampleSizeReached()) {
            break;
          }
        }
      }
    } else {

      if (logger_.isDebugEnabled()) {
        logger_.debug(LogFormatter.create(true).message("loading clauses")
            .add("is_primitive", false).add("literal", literal).formatDebug());
      }

      @Var
      Stopwatch stopwatch = Stopwatch.createStarted();
      Iterator<Clause> facts = kb_.facts(literal);
      stopwatch.stop();

      if (logger_.isDebugEnabled()) {
        logger_.debug(LogFormatter.create(true)
            .message(
                String.format("facts loaded in %d ms", stopwatch.elapsed(TimeUnit.MILLISECONDS)))
            .add("is_primitive", false).add("literal", literal).formatDebug());
      }

      @Var
      boolean match = false;

      while (facts.hasNext()) {

        Clause fact = facts.next();
        Clause renamed = fact.rename();

        Map<com.computablefacts.decima.problog.Var, AbstractTerm> env =
            literal.unify(renamed.head());

        if (env != null) {
          fact(subgoal, renamed.subst(env));
          match = true;
        }
        if (maxSampleSizeReached()) {
          break;
        }
      }

      stopwatch = Stopwatch.createStarted();
      Iterator<Clause> rules = kb_.rules(literal);
      stopwatch.stop();

      if (logger_.isDebugEnabled()) {
        logger_.debug(LogFormatter.create(true)
            .message(
                String.format("rules loaded in %d ms", stopwatch.elapsed(TimeUnit.MILLISECONDS)))
            .add("is_primitive", false).add("literal", literal).formatDebug());
      }

      while (rules.hasNext()) {

        Clause rule = rules.next();
        Clause renamed = rule.rename();

        Map<com.computablefacts.decima.problog.Var, AbstractTerm> env =
            literal.unify(renamed.head());

        if (env != null) {
          rule(subgoal, renamed.subst(env), true);
          match = true;
        }
        if (maxSampleSizeReached()) {
          break;
        }
      }

      if (!match) {
        subgoal.pop(new Clause(literal));
      }
    }
  }

  /**
   * Store a fact, and inform all waiters of the fact too.
   *
   * @param subgoal subgoal.
   * @param clause fact.
   */
  private void fact(Subgoal subgoal, Clause clause) {

    Preconditions.checkNotNull(subgoal, "subgoal should not be null");
    Preconditions.checkNotNull(clause, "clause should not be null");
    Preconditions.checkArgument(clause.isFact(), "clause should be a fact : %s", clause.toString());

    if (!subgoal.addFact(clause)) {
      return;
    }

    for (Map.Entry<Subgoal, Clause> entry : subgoal.waiters()) {

      ground(entry.getKey(), entry.getValue(), clause);

      if (maxSampleSizeReached()) {
        return;
      }
    }
  }

  /**
   * Evaluate a newly derived rule.
   *
   * @param subgoal subgoal.
   * @param rule rule.
   * @param isInKb true iif the rule has been loaded from the KB, false otherwise.
   */
  private void rule(Subgoal subgoal, Clause rule, boolean isInKb) {

    Preconditions.checkNotNull(subgoal, "subgoal should not be null");
    Preconditions.checkNotNull(rule, "rule should not be null");
    Preconditions.checkArgument(rule.isRule(), "rule should be a rule : %s", rule.toString());

    subgoal.addRule(isInKb ? rule : null);
    Literal first = rule.body().get(0);

    if (first.predicate().isPrimitive()) {

      if (logger_.isDebugEnabled()) {
        logger_.debug(
            LogFormatter.create(true).message("evaluating function").add("is_primitive", true)
                .add("first_body_literal", first).add("rule", rule).formatDebug());
      }

      Stopwatch stopwatch = Stopwatch.createStarted();
      Iterator<Literal> literals = first.execute(kb_.definitions());
      stopwatch.stop();

      if (logger_.isDebugEnabled()) {
        logger_.debug(LogFormatter.create(true)
            .message(String.format("function evaluated in %d ms",
                stopwatch.elapsed(TimeUnit.MILLISECONDS)))
            .add("is_primitive", true).add("first_body_literal", first).add("rule", rule)
            .formatDebug());
      }

      if (literals != null) {
        while (literals.hasNext()) {

          ground(subgoal, rule, new Clause(literals.next()));

          if (maxSampleSizeReached()) {
            break;
          }
        }
        return;
      }
      subgoal.pop(rule);
      return;
    }

    @Var
    Subgoal sub = subgoals_.get(first.tag());

    if (sub == null) {

      sub = newSubgoal_.apply(first);
      sub.addWaiter(subgoal, rule);

      subgoals_.put(sub.literal().tag(), sub);

      search(sub);
    } else {

      sub.addWaiter(subgoal, rule);

      Iterator<Clause> facts = sub.facts();

      if (!facts.hasNext()) {
        subgoal.pop(rule);
      } else {
        while (facts.hasNext()) {

          ground(subgoal, rule, facts.next());

          if (maxSampleSizeReached()) {
            return;
          }
        }
      }
    }
  }

  /**
   * Start grounding a rule.
   *
   * @param subgoal subgoal.
   * @param rule rule.
   * @param fact fact.
   */
  private void ground(Subgoal subgoal, Clause rule, Clause fact) {

    Preconditions.checkNotNull(subgoal, "subgoal should not be null");
    Preconditions.checkNotNull(rule, "rule should not be null");
    Preconditions.checkArgument(rule.isRule(), "clause should be a rule : %s", rule.toString());
    Preconditions.checkNotNull(fact, "fact should not be null");
    Preconditions.checkArgument(fact.isFact(), "clause should be a fact : %s", fact.toString());

    // Rule with first body literal
    Clause prevClause = rule.resolve(fact.head());

    Preconditions.checkState(prevClause != null, "resolution failed : rule = %s / head = %s",
        rule.toString(), fact.toString());

    // Rule minus first body literal
    Clause newClause = new Clause(prevClause.head(),
        Collections.unmodifiableList(prevClause.body().subList(1, prevClause.body().size())));

    subgoal.push(prevClause);

    if (newClause.isFact()) {
      fact(subgoal, newClause);
    } else {
      rule(subgoal, newClause, false);
    }
  }
}
