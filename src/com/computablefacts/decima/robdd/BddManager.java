package com.computablefacts.decima.robdd;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;

/**
 * Represents a BDD.
 */
@CheckReturnValue
final public class BddManager {

  private static final int MIN_INIT_SIZE = 4;
  private static final double MAX_GROWTH = 1.2;

  private final Table uniqueTable_ = new Table();
  public BddNode Zero;
  public BddNode One;
  private int nextId = 0;
  private int n_;
  private Map<Tuple<Integer, Integer, Integer>, WeakReference> iteCache_;
  private List<Integer> variableOrder_;
  private Function<Integer, String> variableString_;

  /**
   * Initializes a new instance of the {@link BddManager} class.
   * 
   * @param n The number of variables.
   */
  public BddManager(int n) {

    this.Zero = create(n, false);
    this.One = create(n, true);

    n_ = n;
    iteCache_ = new HashMap<>();
    variableOrder_ = new ArrayList<>(n);

    for (int i = 0; i < n; i++) {
      variableOrder_.add(i);
    }

    variableString_ = Object::toString;
    uniqueTable_.init(Math.max(n, MIN_INIT_SIZE));
  }

  /**
   * Get the number of variables.
   *
   * @return The number of variables.
   */
  public int N() {
    return n_;
  }

  /**
   * Set the number of variables.
   *
   * @param n The number of variables.
   */
  public void N(int n) {
    n_ = n;
    Zero.index(n_);
    One.index(n_);
  }

  /**
   * Get the BDD node corresponding to the specified index variable, low and high identifier.
   *
   * @param index Index.
   * @param low The low node.
   * @param high The high node.
   * @return The unique BDD node.
   */
  public BddNode get(int index, int low, int high) {
    return uniqueTable_.get(index, low, high);
  }

  /**
   * Get the function returning the string corresponding the variable at index. This is used for
   * debugging purpose.
   *
   * @return {@link java.util.function.Function}.
   */
  public Function<Integer, String> variableString() {
    return variableString_;
  }

  /**
   * Set the function returning the string corresponding the variable at index. This is used for
   * debugging purpose.
   * 
   * @param variableString {@link java.util.function.Function}.
   */
  public void variableString(Function<Integer, String> variableString) {
    variableString_ =
        Preconditions.checkNotNull(variableString, "variableString should not be null");
  }

  /**
   * Create the BDD Node corresponding to the variable index, with high and low children.
   *
   * @param index The index of the variable.
   * @param high The high node, or 1-node.
   * @param low The low node, or 0-node.
   * @return The created node.
   */
  public BddNode create(int index, int high, BddNode low) {
    return create(index, high == 0 ? Zero : One, low);
  }

  /**
   * Create the BDD Node corresponding to the variable index, with high and low children.
   *
   * @param index The index of the variable.
   * @param high The high node, or 1-node.
   * @param low The low node, or 0-node.
   * @return The created node.
   */
  public BddNode create(int index, BddNode high, int low) {
    return create(index, high, low == 0 ? Zero : One);
  }

  /**
   * Create the BDD Node corresponding to the variable index, with high and low children.
   *
   * @param index The index of the variable.
   * @param high The high node, or 1-node.
   * @param low The low node, or 0-node.
   * @return The created node.
   */
  public BddNode create(int index, int high, int low) {
    return create(index, high == 0 ? Zero : One, low == 0 ? Zero : One);
  }

  /**
   * Create the BDD Node corresponding to the variable index, with high and low children.
   * 
   * @param index The index of the variable.
   * @param high The high node, or 1-node.
   * @param low The low node, or 0-node.
   * @return The created node.
   */
  public BddNode create(int index, BddNode high, BddNode low) {

    Preconditions.checkArgument((high == null && low == null) || (high != null && low != null),
        "node should not be null");

    @Var
    BddNode unique = uniqueTable_.get(index, low.id(), high.id());

    if (unique != null) {
      return unique;
    }

    unique = new BddNode(index, high, low);
    unique.id(nextId++);

    high.incRefCount();
    low.incRefCount();

    uniqueTable_.put(unique);
    return unique;
  }

  /**
   * Create the sink node corresponding to the specified value.
   *
   * @param index The index for the sink node (shall be n+1 where n is the number of variables).
   * @param value The low node, or 0-node.
   * @return The created node.
   */
  public BddNode create(int index, boolean value) {
    BddNode node = new BddNode(index, value);
    node.id(nextId++);
    return node;
  }

  /**
   * Creates a new variable.
   *
   * @return The variable index.
   */
  public int createVariable() {

    // Table is automatically resized

    int temp = N();
    N(N() + 1);
    variableOrder_.add(temp);

    return temp;
  }

  /**
   * Swap the specified variables. The two variables shall be adjacent. index shall be followed by
   * index2 in the variable order.
   * 
   * @param root The root node of the BDD.
   * @param index Variable index.
   * @param index2 Variable index.
   * @return
   */
  @CanIgnoreReturnValue
  public BddNode Swap(BddNode root, int index, int index2) {

    Preconditions.checkNotNull(root, "root should not be null");

    int i = variableOrder_.indexOf(index) + 1;

    Preconditions.checkState(i < variableOrder_.size(),
        "'" + index + "' is the last variable in the variable order.");

    int nextIndex = variableOrder_.get(i);

    Preconditions.checkState(index2 == nextIndex, "Cannot swap variables not adjacents.");

    variableOrder_.set(i - 1, nextIndex);
    variableOrder_.set(i, index);

    List<BddNode> nodesAtIndex = uniqueTable_.nodes(index);

    for (BddNode n : nodesAtIndex) {
      swapStep(n, index, nextIndex);
    }
    return root;
  }

  /**
   *
   * @param node
   * @param currentIndex
   * @param nextIndex
   */
  public void swapStep(BddNode node, int currentIndex, int nextIndex) {

    Preconditions.checkNotNull(node, "node should not be null");

    if (node.value() != null) {
      return;
    }

    Preconditions.checkState(node.index() == currentIndex,
        "Got %s and should be %s in the unique table.", node.index(), currentIndex);

    if (node.high().index() != nextIndex && node.low().index() != nextIndex) {
      return;
    }

    @Var
    BddNode f11, f10, f01, f00;

    if (node.high().index() == nextIndex) {
      f11 = node.high().high();
      f10 = node.high().low();
    } else {
      f11 = node.high();
      f10 = node.high();
    }

    if (node.low().index() == nextIndex) {
      f01 = node.low().high();
      f00 = node.low().low();
    } else {
      f01 = node.low();
      f00 = node.low();
    }

    @Var
    BddNode a, b;

    if (f11.equals(f01)) {
      a = f11;
    } else {
      a = create(node.index(), f11, f01);
    }

    if (f10.equals(f00)) {
      b = f10;
    } else {
      b = create(node.index(), f10, f00);
    }

    @Var
    BddNode oldLow, oldHigh;

    uniqueTable_.delete(node);
    node.index(nextIndex);

    oldLow = node.low();
    oldHigh = node.high();

    oldLow.decRefCount();

    if (node.low().refCount() == 0) {
      deleteNode(node.low());
    }

    oldHigh.decRefCount();

    if (node.high().refCount() == 0) {
      deleteNode(node.high());
    }

    node.high(a);
    node.low(b);

    uniqueTable_.put(node);
  }

  /**
   * Deletes the specified node.
   *
   * @param node The node to delete.
   */
  public void deleteNode(BddNode node) {

    Preconditions.checkNotNull(node, "node should not be null");

    if (node.value() != null) {
      return;
    }
    if (node.refCount() != 0) {
      return;
    }

    uniqueTable_.delete(node);
    node.low().decRefCount();

    if (node.low().refCount() == 0) {
      deleteNode(node.low());
    }

    node.high().decRefCount();

    if (node.high().refCount() == 0) {
      deleteNode(node.high());
    }
  }

  /**
   * Returns the size of a given BDD.
   *
   * @param root The root node of the BDD.
   * @return The BDD size.
   */
  public int size(BddNode root) {
    return size(root, new HashSet<>());
  }

  /**
   * Returns the size of a given BDD.
   *
   * @param root The root node of the BDD.
   * @param visited The list of visited nodes.
   * @return The BDD size.
   */
  public int size(BddNode root, Set<Integer> visited) {

    Preconditions.checkNotNull(visited, "visited should not be null");

    if (root == null) {
      return 0;
    }
    if (visited.contains(root.id())) {
      return 0;
    }
    visited.add(root.id());
    return size(root.low(), visited) + size(root.high(), visited) + 1;
  }

  /**
   * Applies the sifting algorithm to reduce the size of the BDD by changing the variable order.
   * 
   * @param root The root node of the BDD.
   * @return The BDD with the new variable order.
   */
  public BddNode sifting(BddNode root) {

    Preconditions.checkNotNull(root, "root should not be null");

    // int initial_size = GetSize(root);
    int[] reverseOrder = new int[N()];

    for (int i = 0; i < N(); i++) {
      reverseOrder[variableOrder_.get(i)] = i;
    }

    for (int i = 0; i < N(); i++) {

      // Move variable xi through the order
      @Var
      int optSize = size(root);
      @Var
      int optPos, curPos, startPos = reverseOrder[i];
      optPos = startPos;
      curPos = startPos;

      for (int j = startPos - 1; j >= 0; j--) {

        curPos = j;
        Swap(root, variableOrder_.get(j), variableOrder_.get(j + 1));

        int new_size = size(root);

        if (new_size < optSize) {
          optSize = new_size;
          optPos = j;
        } else if (new_size > MAX_GROWTH * optSize) {
          break;
        }
      }

      for (int j = curPos + 1; j < N(); j++) {

        curPos = j;
        Swap(root, variableOrder_.get(j - 1), variableOrder_.get(j));

        int newSize = size(root);

        if (newSize < optSize) {
          optSize = newSize;
          optPos = j;
        } else if (newSize > MAX_GROWTH * optSize) {
          break;
        }
      }

      if (curPos > optPos) {
        for (int j = curPos - 1; j >= optPos; j--) {
          Swap(root, variableOrder_.get(j), variableOrder_.get(j + 1));
        }
      } else {
        for (int j = curPos + 1; j <= optPos; j++) {
          Swap(root, variableOrder_.get(j - 1), variableOrder_.get(j));
        }
      }
    }
    return root;
  }

  /**
   * Reduce the specified BDD.
   *
   * @param root BDD to reduce.
   * @return The reduced BDD.
   */
  @CanIgnoreReturnValue
  public BddNode reduce(BddNode root) {

    Preconditions.checkNotNull(root, "root should not be null");

    List<BddNode> nodes = root.nodes();
    int size = nodes.size();

    BddNode[] subgraph = new BddNode[size];
    List<BddNode>[] vlist = new ArrayList[N() + 1];

    for (int i = 0; i < size; i++) {
      if (vlist[nodes.get(i).index()] == null) {
        vlist[nodes.get(i).index()] = new ArrayList<>();
      }
      vlist[nodes.get(i).index()].add(nodes.get(i));
    }

    @Var
    int nextId = -1;

    for (int k = N(); k >= 0; k--) {

      int i = (k == N()) ? N() : variableOrder_.get(k);
      List<BddNode> Q = new ArrayList<>();

      if (vlist[i] == null) {
        continue;
      }

      for (BddNode u : vlist[i]) {
        if (u.index() == N()) {
          Q.add(u);
        } else {
          if (u.low().id() == u.high().id()) {
            u.id(u.low().id());
          } else {
            Q.add(u);
          }
        }
      }

      Collections.sort(Q, (x, y) -> {

        Integer xlk = x.key().t;
        Integer xhk = x.key().u;
        Integer ylk = y.key().t;
        Integer yhk = y.key().u;
        int res = xlk.compareTo(ylk);

        return res == 0 ? xhk.compareTo(yhk) : res;
      });

      @Var
      Pair<Integer, Integer> oldKey = new Pair<>(-2, -2);

      for (BddNode u : Q) {
        if (u.key().equals(oldKey)) {
          u.id(nextId);
        } else {

          nextId++;
          u.id(nextId);
          subgraph[nextId] = u;
          u.lowNoUpdate(u.low() == null ? null : subgraph[u.low().id()]);
          u.highNoUpdate(u.high() == null ? null : subgraph[u.high().id()]);

          Preconditions.checkState(
              (u.low() == null && u.high() == null) || (u.low() != null && u.high() != null));

          oldKey = u.key();
        }
      }
    }
    return subgraph[root.id()];
  }

  /**
   * Restrict the specified bdd using the positive and negative sets.
   *
   * @param root The root node of the BDD.
   * @param positive Index of the positive variable.
   * @param negative Index of the negative variable.
   * @return A node.
   */
  public BddNode restrict(BddNode root, int positive, int negative) {
    return restrict(root, positive, negative, null);
  }

  /**
   * Restrict the specified bdd using the positive and negative sets.
   *
   * @param root The root node of the BDD.
   * @param positive Index of the positive variable.
   * @param negative Index of the negative variable.
   * @param cache Cache.
   * @return A node.
   */
  public BddNode restrict(BddNode root, int positive, int negative, @Var BddNode[] cache) {

    Preconditions.checkNotNull(root, "root should not be null");

    if (root.value() != null) {
      return root;
    }

    BddNode cached;

    if (cache == null) {
      cache = new BddNode[nextId];
    } else if ((cached = cache[root.id()]) != null) {
      return cached;
    }

    BddNode ret;

    if (negative == root.index()) {
      ret = root.low(); // Restrict(n.Low, positive, negative, cache);
    } else if (positive == root.index()) {
      ret = root.high(); // Restrict(n.High, positive, negative, cache);
    } else {
      root.lowNoUpdate(restrict(root.low(), positive, negative, cache));
      root.highNoUpdate(restrict(root.high(), positive, negative, cache));
      ret = root;
      cache[root.id()] = ret;
    }
    return ret;
  }

  /**
   * Performs the and operation between the specified f and g.
   *
   * @param f The left node.
   * @param g The right node.
   * @return A node.
   */
  public BddNode and(BddNode f, BddNode g) {
    return ite(f, g, Zero);
  }

  /**
   * Performs the Or operation between the specified f and g.
   *
   * @param f The left node.
   * @param g The right node.
   * @return A node.
   */
  public BddNode or(BddNode f, BddNode g) {
    return ite(f, One, g);
  }

  /**
   * Performs the If-Then-Else operation on nodes f, g, h.
   *
   * @param f The left node.
   * @param g The right node.
   * @param h Node.
   * @return A node.
   */
  public BddNode ite(BddNode f, BddNode g, BddNode h) {

    Preconditions.checkNotNull(f, "f should not be null");
    Preconditions.checkNotNull(g, "g should not be null");
    Preconditions.checkNotNull(h, "h should not be null");

    // ite(f, 1, 0) = f
    if (g.isOne() && h.isZero()) {
      return f;
    }

    // ite(f, 0, 1) = !f
    if (g.isZero() && h.isOne()) {
      return negate(f);
    }

    // ite(1, g, h) = g
    if (f.isOne()) {
      return g;
    }

    // ite(0, g, h) = h
    if (f.isZero()) {
      return h;
    }

    // ite(f, g, g) = g
    if (g.equals(h)) {
      return g;
    }

    Tuple<Integer, Integer, Integer> cacheKey = new Tuple<>(f.id(), g.id(), h.id());

    if (iteCache_.containsKey(cacheKey)) {

      WeakReference wr = iteCache_.get(cacheKey);

      if (wr.get() != null) {
        return (BddNode) wr.get();
      } else {
        iteCache_.remove(cacheKey);
      }
    }

    @Var
    int index = f.index();

    if (g.index() < index) {
      index = g.index();
    }
    if (h.index() < index) {
      index = h.index();
    }

    BddNode fv0 = restrict(f, -1, index);
    BddNode gv0 = restrict(g, -1, index);
    BddNode hv0 = restrict(h, -1, index);

    BddNode fv1 = restrict(f, index, -1);
    BddNode gv1 = restrict(g, index, -1);
    BddNode hv1 = restrict(h, index, -1);

    BddNode node = create(index, ite(fv1, gv1, hv1), ite(fv0, gv0, hv0));

    iteCache_.put(cacheKey, new WeakReference<>(node));
    return node;
  }

  /**
   * Negate the specified node.
   *
   * @param node The node.
   * @return The negated node.
   */
  public BddNode negate(BddNode node) {

    Preconditions.checkNotNull(node, "node should not be null");

    if (node.isZero()) {
      return One;
    }
    if (node.isOne()) {
      return Zero;
    }
    return create(node.index(), negate(node.high()), negate(node.low()));
  }

  /**
   * Returns the dot representation of a given node.
   *
   * @param root The root node of the BDD.
   * @param fnLabel
   * @param showAll
   * @return The dot code.
   */
  public String toDot(BddNode root, @Var Function<BddNode, String> fnLabel, boolean showAll) {

    Preconditions.checkNotNull(root, "root should not be null");

    List<BddNode> nodes = root.nodes();
    StringBuilder t = new StringBuilder("digraph G {\n");

    if (fnLabel == null) {
      fnLabel = (x) -> variableString_.apply(x.index());
    }

    for (int i = 0; i < N(); i++) {

      t.append("\tsubgraph cluster_box_" + i + " {{\n");
      t.append("\tstyle=invis;\n");

      for (BddNode n : uniqueTable_.nodes(i)) {

        @Var
        String color = "grey";

        if (nodes.contains(n)) {
          color = "black";
        }

        if (showAll || nodes.contains(n)) {
          t.append("\t\t" + n.id() + " [label=\"" + fnLabel.apply(n) + "\", " + "color=\"" + color
              + "\"];\n");
        }
      }
      t.append("\t}\n");
    }

    t.append("\tsubgraph cluster_box_sink {\n");
    t.append("\t" + Zero.id() + " [shape=box,label=\"0 (" + Zero.refCount() + ")\"];\n");
    t.append("\t" + One.id() + " [shape=box,label=\"1 (" + One.refCount() + ")\"];\n");
    t.append("\t}\n");

    for (int i = 0; i < N(); i++) {
      for (BddNode n : uniqueTable_.nodes(i)) {

        @Var
        String color = "grey";

        if (nodes.contains(n)) {
          color = "black";
        }

        if (n.index() < N() && (showAll || nodes.contains(n))) {
          t.append("\t" + n.id() + " -> " + n.high().id() + " [color=\"" + color + "}\"];\n");
          t.append(
              "\t" + n.id() + " -> " + n.low().id() + " [style=dotted,color=\"" + color + "\"];\n");
        }
      }
    }

    t.append("}");
    return t.toString();
  }
}
