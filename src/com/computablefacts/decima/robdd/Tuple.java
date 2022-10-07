package com.computablefacts.decima.robdd;

import com.computablefacts.asterix.Generated;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.Objects;

/**
 * A triple consisting of three elements.
 *
 * @param <T> first element.
 * @param <U> second element.
 * @param <V> third element.
 */
@CheckReturnValue
public final class Tuple<T, U, V> {

  public final T t;
  public final U u;
  public final V v;

  public Tuple(T t, U u, V v) {
    this.t = t;
    this.u = u;
    this.v = v;
  }

  @Generated
  @Override
  public String toString() {
    return String.format("[Tuple: t=%s, u=%s, v=%s]", t == null ? "null" : t.toString(),
        u == null ? "null" : u.toString(), v == null ? "null" : v.toString());
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof Tuple)) {
      return false;
    }
    Tuple tuple = (Tuple) o;
    return Objects.equals(this.t, tuple.t) && Objects.equals(this.u, tuple.u) && Objects.equals(this.v, tuple.v);
  }

  @Override
  public int hashCode() {
    return Objects.hash(t, u, v);
  }
}
