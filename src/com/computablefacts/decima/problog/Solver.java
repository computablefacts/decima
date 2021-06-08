package com.computablefacts.decima.problog;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.computablefacts.logfmt.LogFormatter;
import com.google.common.base.Preconditions;
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

  protected final AbstractKnowledgeBase kb_;
  protected final Map<String, Subgoal> subgoals_;

  private final AtomicInteger id_ = new AtomicInteger(0);
  private final BiFunction<Integer, Literal, Subgoal> newSubgoal_;

  private Subgoal rootSubgoal_ = null;
  private int maxSampleSize_ = -1;

  public Solver(AbstractKnowledgeBase kb) {
    this(kb, true);
  }

  public Solver(AbstractKnowledgeBase kb, boolean trackRules) {
    this(kb, (id, literal) -> new Subgoal(id, literal, new InMemorySubgoalFacts(), trackRules));
  }

  public Solver(AbstractKnowledgeBase kb, BiFunction<Integer, Literal, Subgoal> newSubgoal) {

    Preconditions.checkNotNull(kb, "kb should not be null");
    Preconditions.checkNotNull(newSubgoal, "newSubgoal should not be null");

    kb_ = kb;
    subgoals_ = new ConcurrentHashMap<>();
    newSubgoal_ = newSubgoal;
  }

  /**
   * First, sets up and calls the subgoal search procedure. Then, extracts the answers AND UNFOLD
   * the proofs. In order to work, subgoals must track rules i.e. {@code trackRules = true}.
   *
   * @param query goal.
   * @return proofs.
   */
  public Set<Clause> proofs(Literal query) {
    return proofs(query, -1);
  }

  /**
   * First, sets up and calls the subgoal search procedure. Then, extracts the answers AND UNFOLD
   * the proofs until a maximum depth is reached. In order to work, subgoals must track rules i.e.
   * {@code trackRules = true}.
   *
   * @param query goal.
   * @param maxDepth maximum depth to unfold.
   * @return proofs.
   */
  public Set<Clause> proofs(Literal query, int maxDepth) {

    Preconditions.checkNotNull(query, "query should not be null");
    Preconditions.checkArgument(maxDepth == -1 || maxDepth >= 0,
        "maxDepth should be such as maxDepth == -1 or maxDepth >= 0");

    Subgoal subgoal = newSubgoal_.apply(id_.getAndIncrement(), query);
    subgoals_.put(query.tag(), subgoal);

    search(subgoal);

    Map<String, Set<Clause>> proofs = proofsAsTreePaths(subgoal, maxDepth);
    return proofs.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
  }

  /**
   * First, sets up and calls the subgoal search procedure. Then, extracts the answers BUT DO NOT
   * UNFOLD the proofs.
   *
   * @param query goal.
   * @return facts answering the query.
   */
  public Iterator<Clause> solve(Literal query) {
    return solve(query, -1);
  }

  /**
   * First, sets up and calls the subgoal search procedure. Then, extracts the answers BUT DO NOT
   * UNFOLD the proofs.
   *
   * @param query goal.
   * @param maxSampleSize stops the solver after the goal reaches this number of solutions or more.
   *        If this number is less than or equals to 0, returns all solutions.
   * @return facts answering the query.
   */
  public Iterator<Clause> solve(Literal query, int maxSampleSize) {

    Preconditions.checkNotNull(query, "query should not be null");

    Subgoal subgoal = newSubgoal_.apply(id_.getAndIncrement(), query);
    subgoals_.put(query.tag(), subgoal);

    rootSubgoal_ = subgoal;
    maxSampleSize_ = maxSampleSize <= 0 ? -1 : maxSampleSize;

    search(subgoal);
    return subgoal.facts();
  }

  /**
   * Check if the number of samples asked by the caller has been reached.
   *
   * @return true iif the number of samples has been reached, false otherwise.
   */
  private boolean maxSampleSizeReached() {
    return maxSampleSize_ > 0 && rootSubgoal_ != null && rootSubgoal_.nbFacts() >= maxSampleSize_;
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

    if (predicate.isPrimitive()) {

      Iterator<Literal> literals = literal.execute(kb_.definitions());

      if (literals == null) {
        cleanWaiters(subgoal, literal);
      } else {
        while (literals.hasNext()) {

          Clause clause = new Clause(literals.next());

          if (clause.isFact()) {
            add(subgoal, clause);
          } else {
            logger_.warn(LogFormatter.create(true)
                .message("Primitives must only return Stream of facts. Clause ignored.")
                .add("literal", literal.toString()).add("clause", clause.toString()).formatError());
          }
          if (maxSampleSizeReached()) {
            break;
          }
        }
      }
    } else if (predicate.isNegated()) {

      Preconditions.checkState(literal.isSemiGrounded(), "negated clauses should be grounded : %s",
          literal.toString());

      // Evaluate the positive version of the rule (i.e. negation as failure)
      Literal base = new Literal(predicate.baseName(), literal.terms());
      Subgoal sub = newSubgoal_.apply(id_.getAndIncrement(), base);

      subgoals_.put(sub.literal().tag(), sub);

      search(sub);

      String newPredicate = literal.predicate().name();
      List<AbstractTerm> newTerms = literal.terms().stream()
          .map(t -> t.isConst() ? t : new Const("_")).collect(Collectors.toList());

      if (!sub.hasFacts()) {

        // The positive version of the rule yielded no fact
        // => resume the current rule evaluation
        Literal newFact = new Literal(newPredicate, newTerms);

        add(subgoal, new Clause(newFact));
      } else {

        // The positive version of the rule yielded at least one fact
        // => fail the current rule evaluation iif the probability of the produced facts is 0
        Iterator<Clause> facts = sub.facts();

        while (facts.hasNext()) {

          Clause fact = facts.next();

          if (fact.head().isRelevant(base)) {

            Clause newFact;

            if (!sub.hasRules()) {

              // Negate a probabilistic fact
              newFact = new Clause(new Literal(BigDecimal.ONE.subtract(fact.head().probability()),
                  newPredicate, newTerms));
            } else {

              // Negate a probabilistic rule (ignore its probability for now)
              newFact = new Clause(new Literal(newPredicate, newTerms));
            }

            if (!BigDecimal.ZERO.equals(newFact.head().probability())) {
              add(subgoal, newFact);
            } else {
              cleanWaiters(subgoal, literal);
            }
          }
          if (maxSampleSizeReached()) {
            break;
          }
        }
      }
    } else {

      Iterator<Clause> clauses = kb_.clauses(literal);

      while (clauses.hasNext()) {

        Clause clause = clauses.next();
        Clause renamed = clause.rename();

        if (subgoal.trackRules_) {

          List<Literal> list = new ArrayList<>(renamed.body().size() + 1);
          list.add(renamed.head());
          list.addAll(renamed.body());

          subgoal.trie_.insert(list);
        }

        Map<com.computablefacts.decima.problog.Var, AbstractTerm> env =
            literal.unify(renamed.head());

        if (env != null) {
          add(subgoal, renamed.subst(env));
        }
        if (maxSampleSizeReached()) {
          break;
        }
      }
    }
  }

  private void add(Subgoal subgoal, Clause clause) {

    Preconditions.checkNotNull(subgoal, "subgoal should not be null");
    Preconditions.checkNotNull(clause, "clause should not be null");

    if (clause.isFact()) {
      fact(subgoal, clause);
    } else if (clause.isRule()) {
      rule(subgoal, clause);
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

    if (subgoal.containsFact(clause)) {
      return;
    }

    subgoal.addFact(clause);

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
   * @param clause rule.
   */
  private void rule(Subgoal subgoal, Clause clause) {

    Preconditions.checkNotNull(subgoal, "subgoal should not be null");
    Preconditions.checkNotNull(clause, "clause should not be null");
    Preconditions.checkArgument(clause.isRule(), "clause should be a rule : %s", clause.toString());

    Literal first = clause.body().get(0);
    @Var
    Subgoal sub = subgoals_.get(first.tag());

    if (sub == null) {

      sub = newSubgoal_.apply(id_.getAndIncrement(), first);
      sub.addWaiter(subgoal, clause);

      subgoals_.put(sub.literal().tag(), sub);

      search(sub);
    } else {

      sub.addWaiter(subgoal, clause);

      if (!sub.hasFacts() && first.isGrounded()) {
        cleanSubgoal(subgoal, first);
      }

      Iterator<Clause> facts = sub.facts();

      while (facts.hasNext()) {

        ground(subgoal, clause, facts.next());

        if (maxSampleSizeReached()) {
          return;
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

    // Rule minus first body literal
    Clause newClause = prevClause == null ? null
        : new Clause(prevClause.head(),
            Collections.unmodifiableList(prevClause.body().subList(1, prevClause.body().size())));

    // Original rule
    subgoal.update(prevClause);

    if (newClause != null) {
      add(subgoal, newClause);
    }
  }

  /**
   * For all waiters of a given subgoal, remove rules that contain a given literal.
   *
   * @param subgoal subgoal.
   * @param literal literal.
   */
  private void cleanWaiters(Subgoal subgoal, Literal literal) {

    Preconditions.checkNotNull(subgoal, "subgoal should not be null");
    Preconditions.checkNotNull(literal, "literal should not be null");

    for (Map.Entry<Subgoal, Clause> waiter : subgoal.waiters()) {
      cleanSubgoal(waiter.getKey(), literal);
    }
  }

  /**
   * For a given subgoal, remove rules that contain a given literal.
   *
   * @param subgoal subgoal.
   * @param literal literal.
   */
  private void cleanSubgoal(Subgoal subgoal, Literal literal) {

    Preconditions.checkNotNull(subgoal, "subgoal should not be null");
    Preconditions.checkNotNull(literal, "literal should not be null");

    subgoal.cleanup(literal);
  }

  /**
   * Get all proofs of a given subgoal.
   *
   * @param subgoal subgoal.
   * @param maxDepth maximum depth to unfold.
   * @return proofs.
   */
  private Map<String, Set<Clause>> proofsAsTreePaths(Subgoal subgoal, int maxDepth) {

    Preconditions.checkNotNull(subgoal, "subgoal should not be null");
    Preconditions.checkArgument(maxDepth == -1 || maxDepth >= 0,
        "maxDepth should be such as maxDepth == -1 or maxDepth >= 0");

    if (subgoal.hasRules()) {
      return ruleProofs(subgoal, maxDepth);
    }
    return factProofs(subgoal);
  }

  /**
   * Get all rules that solve the current goal.
   *
   * @param subgoal subgoal.
   * @param maxDepth maximum depth to unfold.
   * @return rules.
   */
  private Map<String, Set<Clause>> ruleProofs(Subgoal subgoal, int maxDepth) {

    Preconditions.checkNotNull(subgoal, "subgoal should not be null");

    Map<String, Set<Clause>> proofs = new HashMap<>();
    Map<Predicate, Set<Clause>> rules = new HashMap<>();
    Map<Predicate, Set<Clause>> facts = kbFactsUsedOnly();

    subgoals_.values().forEach(sg -> {
      for (Clause clause : sg.groundedRules()) {
        if (!rules.containsKey(clause.head().predicate())) {
          rules.put(clause.head().predicate(), new HashSet<>());
        }
        rules.get(clause.head().predicate()).add(clause);
      }
    });

    for (Clause rule : subgoal.groundedRules()) {

      // Compute all paths (+ remove cycles) that participate in the current proof
      Literal head = rule.head();
      Set<List<Literal>> paths =
          proofsAsTreePaths(facts, rules, rule, 0, new HashSet<>(), maxDepth);

      for (List<Literal> body : paths) {

        Literal newHead = new Literal(head.predicate().name(), head.terms());
        Clause clause = new Clause(newHead, body);
        String tag = clause.head().tag();

        if (!proofs.containsKey(tag)) {
          proofs.put(tag, new HashSet<>());
        }
        proofs.get(tag).add(clause);
      }
    }
    return proofs;
  }

  /**
   * Get all facts that solve the current goal.
   *
   * @param subgoal subgoal.
   * @return facts.
   */
  private Map<String, Set<Clause>> factProofs(Subgoal subgoal) {

    Preconditions.checkNotNull(subgoal, "subgoal should not be null");

    Map<String, Set<Clause>> proofs = new HashMap<>();

    subgoal.facts().forEachRemaining(fact -> {
      if (!proofs.containsKey(fact.head().tag())) {
        proofs.put(fact.head().tag(), new HashSet<>());
      }
      proofs.get(fact.head().tag()).add(fact);
    });
    return proofs;
  }

  /**
   * Build the tree of proofs.
   *
   * @param facts local KB.
   * @param rules local KB.
   * @param rule rule currently evaluated.
   * @param depth index of the rule body literal to evaluate.
   * @param visited visited literals.
   * @param maxDepth maximum tree depth. -1 means unfold the whole tree.
   * @return proofs as tree paths.
   */
  private Set<List<Literal>> proofsAsTreePaths(Map<Predicate, Set<Clause>> facts,
      Map<Predicate, Set<Clause>> rules, Clause rule, int depth, Set<Literal> visited,
      int maxDepth) {

    Preconditions.checkNotNull(facts, "facts should not be null");
    Preconditions.checkNotNull(rules, "rules should not be null");
    Preconditions.checkArgument(depth >= 0, "depth should be >= 0");
    Preconditions.checkNotNull(rule, "rule should not be null");
    Preconditions.checkArgument(rule.isRule(), "clause should be a rule : %s", rule.toString());
    Preconditions.checkNotNull(visited, "visited should not be null");
    Preconditions.checkArgument(maxDepth == -1 || maxDepth >= 0,
        "maxDepth should be such as maxDepth == -1 or maxDepth >= 0");

    if (depth >= rule.body().size()) {
      return new HashSet<>();
    }
    if (maxDepth == 0) {
      Set<List<Literal>> bodies = new HashSet<>();
      bodies.add(new ArrayList<>(rule.body()));
      return bodies;
    }

    boolean isNegated = rule.body().get(depth).predicate().isNegated();
    boolean isPrimitive = rule.body().get(depth).predicate().isPrimitive();
    Literal literal = isNegated ? rule.body().get(depth).negate() : rule.body().get(depth);

    Preconditions.checkState(!isNegated || !isPrimitive,
        "[S1] inconsistent state for literal %s\nparent rule is %s", literal.toString(),
        rule.toString());

    boolean isRule = rules.containsKey(literal.predicate())
        && rules.get(literal.predicate()).stream().anyMatch(r -> r.head().isRelevant(literal));
    boolean isFact = facts.containsKey(literal.predicate())
        && facts.get(literal.predicate()).stream().anyMatch(f -> f.head().isRelevant(literal));
    boolean isNegatedFact = !isRule && !isFact && isNegated;

    Preconditions.checkState(
        (isRule && !isFact && !isNegatedFact) || (!isRule && (isFact || isNegatedFact)),
        "[S2] inconsistent state for literal %s\nparent rule is %s", literal.toString(),
        rule.toString());

    Set<Literal> newVisitedIn = new HashSet<>(visited);

    if (isRule) {
      newVisitedIn.add(rule.head());
    }

    // Remove the first body literal and recurse on the body tail
    Set<List<Literal>> bodyTailPaths =
        proofsAsTreePaths(facts, rules, rule, depth + 1, newVisitedIn, maxDepth);

    if (isPrimitive) {

      // Here, the first body literal is a primitive (primitives cannot be negated)

      if (bodyTailPaths.isEmpty()) {
        List<Literal> path = new ArrayList<>();
        path.add(literal);
        bodyTailPaths.add(path);
        return bodyTailPaths;
      }

      for (List<Literal> path : bodyTailPaths) {
        path.add(0, literal);
      }
      return bodyTailPaths;
    }

    if (isFact || isNegatedFact) {

      // Here, the first body literal is a fact (possibly negated)

      if (bodyTailPaths.isEmpty()) {
        List<Literal> path = new ArrayList<>();
        path.add(isNegated ? literal.negate() : literal);
        bodyTailPaths.add(path);
        return bodyTailPaths;
      }

      for (List<Literal> path : bodyTailPaths) {
        path.add(0, isNegated ? literal.negate() : literal);
      }
      return bodyTailPaths;
    }

    Set<List<Literal>> paths = new HashSet<>();

    // Here, the first body literal is either a positive or a negative rule

    Preconditions.checkState(rules.containsKey(literal.predicate()),
        "[S3] inconsistent state for literal %s\nparent rule is %s", literal.toString(),
        rule.toString());

    @Var
    Set<Clause> matchedRules = rules.get(literal.predicate()).stream()
        .filter(clause -> !visited.contains(clause.head()) && clause.head().isRelevant(literal))
        .collect(Collectors.toSet());

    if (matchedRules.isEmpty()) {

      matchedRules =
          rules.get(literal.predicate()).stream().filter(clause -> clause.head().isRelevant(literal)
              && isOnlyMadeOfFactsAndPrimitives(facts, clause)).collect(Collectors.toSet());

      if (bodyTailPaths.isEmpty()) {
        return matchedRules.stream().map(clause -> new ArrayList<>(clause.body()))
            .collect(Collectors.toSet());
      }
      return new HashSet<>();
    }

    for (Clause clause : matchedRules) {

      Set<List<Literal>> bodyFirstPaths;

      if (isOnlyMadeOfFactsAndPrimitives(facts, clause)) {
        bodyFirstPaths = new HashSet<>();
        bodyFirstPaths.add(new ArrayList<>(clause.body()));
      } else {

        Set<Literal> tmpVisitedIn = new HashSet<>(newVisitedIn);
        tmpVisitedIn.add(clause.head());

        bodyFirstPaths = proofsAsTreePaths(facts, rules, clause, 0, tmpVisitedIn,
            maxDepth == -1 ? maxDepth : maxDepth - 1);
      }

      if (bodyTailPaths.isEmpty()) {
        if (!isNegated) {
          paths.addAll(bodyFirstPaths);
        } else {
          for (List<Literal> path : bodyFirstPaths) {
            for (Literal lit : path) {

              List<Literal> body = new ArrayList<>();

              if (!lit.predicate().isPrimitive()) {
                body.add(lit.negate());
              }

              if (!body.isEmpty()) {
                paths.add(body);
              }
            }
          }
        }
      } else {
        if (!isNegated) {
          for (List<Literal> firstPath : bodyFirstPaths) {
            for (List<Literal> tailPath : bodyTailPaths) {

              List<Literal> body = new ArrayList<>();

              body.addAll(firstPath);
              body.addAll(tailPath);

              paths.add(body);
            }
          }
        } else {
          for (List<Literal> firstPath : bodyFirstPaths) {
            for (List<Literal> tailPath : bodyTailPaths) {
              for (Literal lit : firstPath) {

                List<Literal> body = new ArrayList<>();

                if (!lit.predicate().isPrimitive()) {
                  body.add(lit.negate());
                }

                body.addAll(tailPath);
                paths.add(body);
              }
            }
          }
        }
      }
    }
    return paths;
  }

  /**
   * Check if a rule body literals are only made of facts and primitives.
   *
   * @param facts local KB.
   * @param clause rule.
   * @return true iif, the body of the rule is only made of facts and primitives.
   */
  private boolean isOnlyMadeOfFactsAndPrimitives(Map<Predicate, Set<Clause>> facts, Clause clause) {

    Preconditions.checkNotNull(facts, "facts should not be null");
    Preconditions.checkNotNull(clause, "clause should not be null");
    Preconditions.checkArgument(clause.isRule(), "clause should be a rule");

    return clause.body().stream().allMatch(literal -> isFactOrPrimitive(facts, literal));
  }

  /**
   * Check if a literal is a fact or primitive.
   *
   * @param facts local KB.
   * @param literal literal.
   * @return true iif the literal is a fact or a primitive.
   */
  private boolean isFactOrPrimitive(Map<Predicate, Set<Clause>> facts, Literal literal) {

    Preconditions.checkNotNull(facts, "facts should not be null");
    Preconditions.checkNotNull(literal, "literal should not be null");

    return literal.predicate().isPrimitive() || (facts.containsKey(literal.predicate()) && facts
        .get(literal.predicate()).stream().anyMatch(fact -> fact.head().isRelevant(literal)));
  }

  /**
   * Compute the list of KB facts used while proving a goal.
   *
   * @return list of KB facts.
   */
  private Map<Predicate, Set<Clause>> kbFactsUsedOnly() {

    Map<Predicate, Set<Clause>> facts = new HashMap<>();

    subgoals_.values().stream().filter(s -> !s.hasRules())
        .flatMap(s -> StreamSupport
            .stream(Spliterators.spliteratorUnknownSize(s.facts(), Spliterator.ORDERED), false))
        .forEach(fact -> {
          if (!facts.containsKey(fact.head().predicate())) {
            facts.put(fact.head().predicate(), new HashSet<>());
          }
          facts.get(fact.head().predicate()).add(fact);
        });
    return facts;
  }
}

