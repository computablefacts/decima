package com.computablefacts.decima.trie;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import com.computablefacts.decima.robdd.Pair;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.Var;

final public class Trie<T> {

  private final TrieNode<T> root_ = new TrieNode<>();

  public Trie() {}

  /**
   * Get the Trie root.
   *
   * @return the Trie root.
   */
  public TrieNode<T> root() {
    return root_;
  }

  /**
   * Traverse the whole Trie and decide for each node if we should start a process or not.
   * 
   * @param fnSkipSequence predicate that returns true if the current Trie path should trigger a
   *        processing task and false otherwise.
   * @param fnProcessSequence function/task triggered when fnSkipSequence returns false.
   */
  public void traverse(Function<Stack<Pair<Integer, T>>, Boolean> fnSkipSequence,
      Function<Stack<Pair<Integer, T>>, Void> fnProcessSequence) {

    Preconditions.checkNotNull(fnSkipSequence, "fnSkipSequence should not be null");
    Preconditions.checkNotNull(fnProcessSequence, "fnProcessSequence should not be null");

    Stack<Pair<Integer, T>> parents = new Stack<>();

    for (T child : root_.children().keySet()) {

      TrieNode<T> node = root_.children().get(child);
      parents.push(new Pair<>(node.id(), child));

      if (!fnSkipSequence.apply(parents)) {
        traverse(node, parents, fnSkipSequence, fnProcessSequence);
      }

      parents.pop();
    }
  }

  /**
   * Traverse the whole Trie and decide for each node if it should be kept or removed.
   * 
   * @param fnRemoveNode predicate that returns true if the current node should be removed and false
   *        otherwise.
   */
  public void remove(Function<T, Boolean> fnRemoveNode) {

    Preconditions.checkNotNull(fnRemoveNode, "fnRemoveNode should not be null");

    remove(root_, fnRemoveNode);
  }

  /**
   * Add a sequence to a Trie.
   *
   * @param list sequence to add.
   */
  public void insert(List<T> list) {

    Preconditions.checkNotNull(list, "list should not be null");

    @Var
    TrieNode<T> current = root_;

    for (int i = 0; i < list.size(); i++) {
      current = current.children().computeIfAbsent(list.get(i), c -> new TrieNode<>());
    }

    current.endOfSequence(true);
  }

  /**
   * Remove a sequence from a Trie.
   *
   * @param list sequence to remove.
   * @return true if the sequence has been removed, false otherwise.
   */
  public boolean delete(List<T> list) {

    Preconditions.checkNotNull(list, "list should not be null");

    return delete(root_, list, 0);
  }

  /**
   * Test if a sequence exists in a Trie.
   *
   * @param list sequence to test.
   * @return true iif the sequence exists in the Trie, false otherwise.
   */
  public boolean contains(List<T> list) {

    Preconditions.checkNotNull(list, "list should not be null");

    @Var
    TrieNode<T> current = root_;

    for (int i = 0; i < list.size(); i++) {

      T element = list.get(i);
      TrieNode<T> node = current.children().get(element);

      if (node == null) {
        return false;
      }

      current = node;
    }
    return current.isEndOfSequence();
  }

  /**
   * Test if a Trie is empty.
   *
   * @return true iif the Trie is empty, false otherwise.
   */
  public boolean isEmpty() {
    return root_.children().isEmpty();
  }

  private void traverse(TrieNode<T> node, Stack<Pair<Integer, T>> parents,
      Function<Stack<Pair<Integer, T>>, Boolean> fnSkipPath,
      Function<Stack<Pair<Integer, T>>, Void> fnProcessPath) {

    Preconditions.checkNotNull(node, "node should not be null");
    Preconditions.checkNotNull(parents, "parents should not be null");
    Preconditions.checkNotNull(fnSkipPath, "fnSkipPath should not be null");
    Preconditions.checkNotNull(fnProcessPath, "fnProcessPath should not be null");

    if (node.isEndOfSequence()) {
      if (!fnSkipPath.apply(parents)) {
        fnProcessPath.apply(parents);
      }
    } else {
      for (T child : node.children().keySet()) {

        TrieNode<T> n = node.children().get(child);
        parents.push(new Pair<>(n.id(), child));

        if (!fnSkipPath.apply(parents)) {
          traverse(n, parents, fnSkipPath, fnProcessPath);
        }

        parents.pop();
      }
    }
  }

  private boolean remove(TrieNode<T> current, Function<T, Boolean> fnRemoveNode) {

    Preconditions.checkNotNull(current, "current should not be null");
    Preconditions.checkNotNull(fnRemoveNode, "fnRemoveNode should not be null");

    Set<T> remove = new HashSet<>();

    for (T t : current.children().keySet()) {

      TrieNode<T> next = current.children().get(t);

      if (fnRemoveNode.apply(t)) {
        remove.add(t);
      } else {
        if (remove(next, fnRemoveNode)) {
          remove.add(t);
        }
      }
    }

    for (T t : remove) {
      current.children().remove(t);
    }

    if (current.children().isEmpty()) {
      current.endOfSequence(true);
    }
    return !remove.isEmpty() && current.children().isEmpty();
  }

  private boolean delete(TrieNode<T> current, List<T> list, int index) {

    Preconditions.checkNotNull(current, "current should not be null");
    Preconditions.checkNotNull(list, "list should not be null");
    Preconditions.checkArgument(index >= 0, "index should be >= 0");

    if (index == list.size()) {

      if (!current.isEndOfSequence()) {
        return false;
      }

      current.endOfSequence(false);
      return current.children().isEmpty();
    }

    T element = list.get(index);
    TrieNode<T> node = current.children().get(element);

    if (node == null) {
      return false;
    }

    boolean shouldDeleteCurrentNode = delete(node, list, index + 1) && !node.isEndOfSequence();

    if (shouldDeleteCurrentNode) {
      current.children().remove(element);
      return current.children().isEmpty();
    }
    return false;
  }
}
