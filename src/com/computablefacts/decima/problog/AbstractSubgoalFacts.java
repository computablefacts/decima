package com.computablefacts.decima.problog;

import java.util.Iterator;

import com.google.errorprone.annotations.CheckReturnValue;

@CheckReturnValue
public abstract class AbstractSubgoalFacts {

  public abstract boolean isEmpty();

  public abstract boolean contains(Clause clause);

  public abstract Iterator<Clause> facts();

  public abstract void add(Clause clause);

  public abstract void add(AbstractSubgoalFacts facts);
}
