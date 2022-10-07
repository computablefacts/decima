package com.computablefacts.decima.problog;

import com.google.errorprone.annotations.CheckReturnValue;
import java.util.Iterator;

@CheckReturnValue
public abstract class AbstractSubgoalFacts {

  public abstract boolean contains(Clause clause);

  public abstract Iterator<Clause> facts();

  public abstract int size();

  public abstract void add(Clause clause);
}
