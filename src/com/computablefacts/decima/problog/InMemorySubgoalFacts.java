package com.computablefacts.decima.problog;

import com.google.errorprone.annotations.CheckReturnValue;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@CheckReturnValue
final public class InMemorySubgoalFacts extends AbstractSubgoalFacts {

  private final Set<Clause> facts_ = ConcurrentHashMap.newKeySet();

  public InMemorySubgoalFacts() {
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
  public int size() {
    return facts_.size();
  }

  @Override
  public void add(Clause clause) {
    facts_.add(clause);
  }
}
