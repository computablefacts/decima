package com.computablefacts.decima.trie;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.computablefacts.nona.Generated;
import com.google.common.base.MoreObjects;
import com.google.errorprone.annotations.CheckReturnValue;

@CheckReturnValue
final public class TrieNode<T> {

  private final static AtomicInteger ID = new AtomicInteger(0);
  private final Map<T, TrieNode<T>> children_ = new HashMap<>();
  private final int id_;
  private boolean isEndOfSequence_;

  public TrieNode() {
    id_ = ID.getAndIncrement();
  }

  @Generated
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("id", id_).add("isEndOfSequence", isEndOfSequence_)
        .add("children", children_).omitNullValues().toString();
  }

  public int id() {
    return id_;
  }

  public Map<T, TrieNode<T>> children() {
    return children_;
  }

  public boolean isEndOfSequence() {
    return isEndOfSequence_;
  }

  public void endOfSequence(boolean leaf) {
    isEndOfSequence_ = leaf;
  }
}
