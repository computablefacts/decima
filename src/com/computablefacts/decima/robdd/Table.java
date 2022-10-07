package com.computablefacts.decima.robdd;

import com.google.errorprone.annotations.CheckReturnValue;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@CheckReturnValue
final public class Table {

  private final Map<Integer, List<BddNode>> table_ = new ConcurrentHashMap<>();

  public Table() {
  }

  /**
   * Initializes a new instance of the {@link Table} class.
   *
   * @param n The initial number of buckets.
   */
  public void init(int n) {
    for (int i = 0; i < n; i++) {
      table_.put(i, new CopyOnWriteArrayList<>());
    }
  }

  /**
   * Returns the number of key-value pairs in this symbol table.
   *
   * @return The number of key-value pairs in this symbol table.
   */
  public int size() {
    return table_.keySet().size();
  }

  /**
   * Returns the value associated with the specified key in this symbol table.
   *
   * @param index Index.
   * @param low   Low identifier.
   * @param high  High identifier.
   * @return The value associated with key in the symbol table, null if no such value.
   */
  public BddNode get(int index, int low, int high) {

    if (!table_.containsKey(index)) {
      return null;
    }

    Optional<BddNode> node = table_.get(index).stream().filter(n -> n.low().id() == low && n.high().id() == high)
        .findFirst();
    return node.orElse(null);
  }

  /**
   * Delete the specified node from the table.
   *
   * @param node Node.
   */
  public void delete(BddNode node) {
    delete(node.index(), node.low() == null ? -1 : node.low().id(), node.high() == null ? -1 : node.high().id());
  }

  /**
   * Delete the node at specified index, low and high identifier.
   *
   * @param index Index.
   * @param low   Low identifier.
   * @param high  High identifier.
   */
  public void delete(int index, int low, int high) {

    if (!table_.containsKey(index)) {
      return;
    }

    BddNode node = get(index, low, high);

    if (node != null) {
      table_.get(index).remove(node);
    }
  }

  /**
   * Put the specified node in the table.
   *
   * @param val node.
   */
  public void put(BddNode val) {
    put(val.index(), val.low() == null ? -1 : val.low().id(), val.high() == null ? -1 : val.high().id(), val);
  }

  /**
   * Put the specified node at index, low, and high identifier.
   *
   * @param index Index.
   * @param low   Low identifier.
   * @param high  High identifier.
   * @param val   Value.
   */
  public void put(int index, int low, int high, BddNode val) {

    if (val == null) {
      delete(index, low, high);
      return;
    }

    if (!table_.containsKey(index)) {
      table_.put(index, new CopyOnWriteArrayList<>());
    }

    List<BddNode> nodes = table_.get(index);

    if (!nodes.contains(val)) {
      nodes.add(val);
    }
  }

  /**
   * Returns the nodes contained in a given bucket index.
   *
   * @param index Bucket index.
   * @return List of nodes.
   */
  public List<BddNode> nodes(int index) {
    return table_.get(index);
  }
}
