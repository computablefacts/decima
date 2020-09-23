package com.computablefacts.decima.problog;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

final public class InMemoryKnowledgeBase extends AbstractKnowledgeBase {

  private Map<Predicate, Set<Clause>> facts_ = new HashMap<>();
  private Map<Predicate, Set<Clause>> rules_ = new HashMap<>();

  public InMemoryKnowledgeBase() {}

  @Override
  protected void azzertFact(@NotNull Clause fact) {

    Literal head = fact.head();
    Predicate predicate = head.predicate();

    if (!facts_.containsKey(predicate)) {
      facts_.put(predicate, new HashSet<>());
    }
    facts_.get(predicate).add(fact);
  }

  @Override
  protected void azzertRule(@NotNull Clause rule) {

    Literal head = rule.head();
    Predicate predicate = head.predicate();

    if (!rules_.containsKey(predicate)) {
      rules_.put(predicate, new HashSet<>());
    }
    rules_.get(predicate).add(rule);
  }

  @Override
  protected Set<Clause> facts(@NotNull Literal literal) {
    return facts_.getOrDefault(literal.predicate(), new HashSet<>()).stream()
        .filter(f -> f.head().isRelevant(literal)).collect(Collectors.toSet());
  }

  @Override
  protected Set<Clause> rules(@NotNull Literal literal) {
    return rules_.getOrDefault(literal.predicate(), new HashSet<>()).stream()
        .filter(r -> r.head().isRelevant(literal)).collect(Collectors.toSet());
  }

  @Override
  public Set<Clause> facts() {
    return facts_.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
  }

  @Override
  public Set<Clause> rules() {
    return rules_.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
  }
}
