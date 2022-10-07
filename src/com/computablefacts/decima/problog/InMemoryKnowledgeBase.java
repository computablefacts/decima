package com.computablefacts.decima.problog;

import com.google.errorprone.annotations.CheckReturnValue;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.validation.constraints.NotNull;

@CheckReturnValue
final public class InMemoryKnowledgeBase extends AbstractKnowledgeBase {

  private Map<Predicate, Set<Clause>> facts_ = new ConcurrentHashMap<>();
  private Map<Predicate, Set<Clause>> rules_ = new ConcurrentHashMap<>();

  public InMemoryKnowledgeBase() {
  }

  @Override
  protected void azzertFact(@NotNull Clause fact) {

    Literal head = fact.head();
    Predicate predicate = head.predicate();

    if (!facts_.containsKey(predicate)) {
      facts_.put(predicate, ConcurrentHashMap.newKeySet());
    }
    facts_.get(predicate).add(fact);
  }

  @Override
  protected void azzertRule(@NotNull Clause rule) {

    Literal head = rule.head();
    Predicate predicate = head.predicate();

    if (!rules_.containsKey(predicate)) {
      rules_.put(predicate, ConcurrentHashMap.newKeySet());
    }
    rules_.get(predicate).add(rule);
  }

  @Override
  protected Iterator<Clause> facts(@NotNull Literal literal) {
    return facts_.getOrDefault(literal.predicate(), ConcurrentHashMap.newKeySet()).stream()
        .filter(f -> f.head().isRelevant(literal)).iterator();
  }

  @Override
  protected Iterator<Clause> rules(@NotNull Literal literal) {
    return rules_.getOrDefault(literal.predicate(), ConcurrentHashMap.newKeySet()).stream()
        .filter(r -> r.head().isRelevant(literal)).iterator();
  }

  @Override
  public Iterator<Clause> facts() {
    return facts_.values().stream().flatMap(Collection::stream).iterator();
  }

  @Override
  public Iterator<Clause> rules() {
    return rules_.values().stream().flatMap(Collection::stream).iterator();
  }
}
