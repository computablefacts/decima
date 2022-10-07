package com.computablefacts.decima.robdd;

import com.computablefacts.asterix.Generated;
import com.google.common.collect.Lists;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents a BDD node.
 */
@CheckReturnValue
final public class BddNode {

  // The node value
  private Boolean value_;

  // The node index in the {@link Table}
  private int index_;

  // The node unique identifier
  private int id_;

  // Low node, i.e. the node to follow when variable {@link BddNode#index} is false.
  private BddNode low_;

  // High node, i.e. the node to follow when variable {@link BddNode#index} is true.
  private BddNode high_;

  // Reference count i.e. the number of time this node is referenced.
  private int refCount_;

  public BddNode() {
  }

  public BddNode(int index, BddNode high, BddNode low) {
    index_ = index;
    high_ = high;
    low_ = low;
  }

  public BddNode(int index, boolean value) {
    value_ = value;
    index_ = index;
  }

  @Generated
  @Override
  public String toString() {
    return String.format("[BddNode: Identifier=%s, Value=%s, Index=%s, Low=%s, High=%s, RefCount=%s]", id_, value_,
        index_, low_ != null ? low_.id_ : "null", high_ != null ? high_.id_ : "null", refCount_);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    BddNode other = (BddNode) obj;
    return Objects.equals(id_, other.id_) && Objects.equals(low_ == null ? null : low_.id_,
        other.low_ == null ? null : other.low_.id_) && Objects.equals(high_ == null ? null : high_.id_,
        other.high_ == null ? null : other.high_.id_);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id_, low_ == null ? null : low_.id_, high_ == null ? null : high_.id_);
  }

  /**
   * Get the node value.
   *
   * @return the node value.
   */
  public Boolean value() {
    return value_;
  }

  /**
   * Get the node identifier.
   *
   * @return The node identifier.
   */
  public int id() {
    return id_;
  }

  /**
   * Set the node identifier.
   */
  public void id(int id) {
    id_ = id;
  }

  /**
   * Get the node index.
   *
   * @return The node index.
   */
  public int index() {
    return index_;
  }

  /**
   * Set the node index.
   */
  public void index(int index) {
    index_ = index;
  }

  /**
   * Get the low node.
   *
   * @return The low node.
   */
  public BddNode low() {
    return low_;
  }

  /**
   * Set the low node but do not update the ref count.
   *
   * @param low Low node.
   */
  public void lowNoUpdate(BddNode low) {
    low_ = low;
  }

  /**
   * Set the low node and update the ref count.
   *
   * @param low Low node.
   */
  public void low(BddNode low) {
    low_ = low;
    low.refCount_++;
  }

  /**
   * Get the high node.
   *
   * @return The high node.
   */
  public BddNode high() {
    return high_;
  }

  /**
   * Set the high node but do not update the ref count.
   *
   * @param high High node.
   */
  public void highNoUpdate(BddNode high) {
    high_ = high;
  }

  /**
   * Set the high node and update the ref count.
   *
   * @param high High node.
   */
  public void high(BddNode high) {
    high_ = high;
    high.refCount_++;
  }

  /**
   * Get the reference count.
   *
   * @return The reference count.
   */
  public int refCount() {
    return refCount_;
  }

  /**
   * Increment the reference count.
   */
  public void incRefCount() {
    refCount_++;
  }

  /**
   * Decrement the reference count.
   */
  public void decRefCount() {
    refCount_--;
  }

  /**
   * Get all the nodes, including descendants.
   *
   * @return List of distinct nodes.
   */
  public List<BddNode> nodes() {

    if (low_ == null && high_ == null) {
      return Lists.newArrayList(this);
    }

    List<BddNode> nodes = new ArrayList<>();
    nodes.add(this);
    nodes.addAll(low_.nodes());
    nodes.addAll(high_.nodes());

    return nodes.stream().distinct().collect(Collectors.toList());
  }

  /**
   * Get the key composed by (Low.Id, High.Id).
   *
   * @return The key.
   */
  public Pair<Integer, Integer> key() {
    if (isZero()) {
      return new Pair<>(-1, -1);
    }
    if (isOne()) {
      return new Pair<>(-1, 0);
    }
    return new Pair<>(low_.id_, high_.id_);
  }

  /**
   * Get a value indicating whether this instance is the node one.
   *
   * @return True if this instance is one, false otherwise.
   */
  public boolean isOne() {
    return value_ != null && value_ == true;
  }

  /**
   * Get a value indicating whether this instance is the node zero.
   *
   * @return True if this instance is zero, false otherwise.
   */
  public boolean isZero() {
    return value_ != null && value_ == false;
  }
}
