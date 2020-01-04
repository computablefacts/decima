package com.computablefacts.decima.robdd;

import java.util.Objects;

import com.google.errorprone.annotations.CheckReturnValue;

/**
 * A triple consisting of three elements.
 *
 * @param <T> first element.
 * @param <U> second element.
 * @param <V> third element.
 */
@CheckReturnValue
public final class Tuple<T, U, V> {

  public T t;
  public U u;
  public V v;

  public Tuple(T t, U u, V v) {
    this.t = t;
    this.u = u;
    this.v = v;
  }

  @Override
  public String toString() {
    return String.format("[Tuple: t=%s, u=%s, v=%s]", t == null ? "null" : t.toString(),
        u == null ? "null" : u.toString(), v == null ? "null" : v.toString());
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }
    if (!(o instanceof Tuple)) {
      return false;
    }
    Tuple tuple = (Tuple) o;
    return Objects.equals(this.t, tuple.t) && Objects.equals(this.u, tuple.u)
        && Objects.equals(this.v, tuple.v);
  }

  @Override
  public int hashCode() {
    return Objects.hash(t, u, v);
  }
}
