package com.computablefacts.decima.robdd;

import com.computablefacts.asterix.Generated;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.Objects;

/**
 * A pair consisting of two elements.
 *
 * @param <T> first element.
 * @param <U> second element.
 */
@CheckReturnValue
public final class Pair<T, U> {

  public final T t;
  public final U u;

  public Pair(T t, U u) {
    this.t = t;
    this.u = u;
  }

  @Generated
  @Override
  public String toString() {
    return String.format("[Pair: t=%s, u=%s]", t == null ? "null" : t.toString(), u == null ? "null" : u.toString());
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof Pair)) {
      return false;
    }
    Pair tuple = (Pair) o;
    return Objects.equals(this.t, tuple.t) && Objects.equals(this.u, tuple.u);
  }

  @Override
  public int hashCode() {
    return Objects.hash(t, u);
  }
}
