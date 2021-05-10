package com.computablefacts.decima.problog;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.errorprone.annotations.CheckReturnValue;

@CheckReturnValue
final public class InMemorySubgoalFacts extends AbstractSubgoalFacts {

  private final Set<Clause> facts_ = ConcurrentHashMap.newKeySet();

  public InMemorySubgoalFacts() {}

  @Override
  public boolean isEmpty() {
    return facts_.isEmpty();
  }

  @Override
  public boolean contains(Clause clause) {
    return facts_.contains(clause);
  }

  @Override
  public Iterator<Clause> facts() {
    return facts_.iterator();
  }

  @Override
  public int nbFacts() {
    return facts_.size();
  }

  @Override
  public void add(Clause clause) {
    facts_.add(clause);
  }

  @Override
  public void add(AbstractSubgoalFacts facts) {
    facts.facts().forEachRemaining(facts_::add);
  }
}
