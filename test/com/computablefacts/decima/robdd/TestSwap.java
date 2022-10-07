package com.computablefacts.decima.robdd;

import com.google.errorprone.annotations.Var;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class TestSwap extends TestBdd {

  @Test(expected = IllegalStateException.class)
  public void testSwapLastVariable() {

    BddManager manager = new BddManager(2);
    BddNode n3 = manager.create(1, manager.One, manager.Zero);
    BddNode n4 = manager.create(1, manager.Zero, manager.One);
    BddNode root = manager.create(0, n3, n4);
    BddNode res = manager.Swap(root, 1, 2);
  }

  @Test(expected = IllegalStateException.class)
  public void testSwapNotAdjacentVariable() {

    BddManager manager = new BddManager(2);
    BddNode n3 = manager.create(1, manager.One, manager.Zero);
    BddNode n4 = manager.create(1, manager.Zero, manager.One);
    BddNode root = manager.create(0, n3, n4);
    BddNode res = manager.Swap(root, 0, 2);
  }

  @Test
  public void testSwapSimple() {

    BddManager manager = new BddManager(2);
    BddNode n3 = manager.create(1, manager.One, manager.Zero);
    BddNode n4 = manager.create(1, manager.Zero, manager.One);
    BddNode root = manager.create(0, n3, n4);

    Map<String, Boolean> truth = buildThruthTable(manager, root);

    System.out.println(manager.toDot(root, (x) -> Integer.toString(x.refCount(), 10), true));

    BddNode res = manager.Swap(root, 0, 1);

    System.out.println(manager.toDot(res, (x) -> Integer.toString(x.refCount(), 10), true));

    checkThruthTable(truth, res);
  }

  @Test
  public void testSwapAsymetric() {

    BddManager manager = new BddManager(2);
    BddNode n3 = manager.create(1, manager.One, manager.Zero);
    BddNode root = manager.create(0, n3, manager.One);

    Map<String, Boolean> truth = buildThruthTable(manager, root);

    System.out.println(manager.toDot(root, (x) -> Integer.toString(x.refCount(), 10), true));

    BddNode res = manager.reduce(manager.Swap(root, 0, 1));

    System.out.println(manager.toDot(res, (x) -> Integer.toString(x.refCount(), 10), true));

    checkThruthTable(truth, res);
  }

  @Test
  public void testSwapSimple2() {

    BddManager manager = new BddManager(3);
    BddNode n2 = manager.create(2, manager.One, manager.Zero);
    BddNode n3 = manager.create(1, manager.One, n2);
    BddNode n4 = manager.create(1, manager.Zero, manager.One);
    BddNode root = manager.create(0, n3, n4);

    Map<Integer, String> dict = new HashMap<>();
    dict.put(0, "a");
    dict.put(1, "b");
    dict.put(2, "c");

    Map<String, Integer> rdict = new HashMap<>();
    rdict.put("a", 0);
    rdict.put("b", 1);
    rdict.put("c", 2);

    Map<String, Boolean> truth = buildThruthTable(manager, root);

    System.out.println(manager.toDot(root, (x) -> Integer.toString(x.refCount(), 10), true));

    @Var BddNode res = manager.Swap(root, rdict.get("b"), rdict.get("c"));
    checkThruthTable(truth, res);

    System.out.println(manager.toDot(root, (x) -> Integer.toString(x.refCount(), 10), true));

    res = manager.reduce(res);
    checkThruthTable(truth, res);

    System.out.println(manager.toDot(root, (x) -> Integer.toString(x.refCount(), 10), true));
  }

  @Test
  public void testSwapSimple2Chained() {

    BddManager manager = new BddManager(3);
    BddNode n2 = manager.create(2, manager.One, manager.Zero);
    BddNode n3 = manager.create(1, manager.One, n2);
    BddNode n4 = manager.create(1, manager.Zero, manager.One);
    BddNode root = manager.create(0, n3, n4);

    Map<Integer, String> dict = new HashMap<>();
    dict.put(0, "a");
    dict.put(1, "b");
    dict.put(2, "c");

    Map<String, Integer> rdict = new HashMap<>();
    rdict.put("a", 0);
    rdict.put("b", 1);
    rdict.put("c", 2);

    Map<String, Boolean> truth = buildThruthTable(manager, root);

    @Var BddNode res = manager.Swap(root, rdict.get("b"), rdict.get("c"));
    checkThruthTable(truth, res);

    res = manager.Swap(root, rdict.get("a"), rdict.get("c"));
    checkThruthTable(truth, res);

    res = manager.reduce(res);
    checkThruthTable(truth, res);
  }

  @Test
  public void testSwapBug() {

    Map<Integer, String> dict = new HashMap<>();
    dict.put(0, "x1");
    dict.put(1, "x2");
    dict.put(2, "x4");
    dict.put(3, "x3");
    dict.put(4, "x5");
    dict.put(5, "x6");

    Map<String, Integer> rdict = new HashMap<>();
    rdict.put("x1", 0);
    rdict.put("x2", 1);
    rdict.put("x4", 2);
    rdict.put("x3", 3);
    rdict.put("x5", 4);
    rdict.put("x6", 5);

    BddManager manager = new BddManager(6);
    manager.variableString(dict::get);

    BddNode n9 = manager.create(rdict.get("x6"), 1, 0);
    BddNode n8 = manager.create(rdict.get("x5"), 0, n9);
    BddNode n7 = manager.create(rdict.get("x5"), n9, 0);
    BddNode n6 = manager.create(rdict.get("x3"), n8, n7);
    BddNode n5 = manager.create(rdict.get("x3"), 1, n7);
    BddNode n4 = manager.create(rdict.get("x4"), n5, n6);
    BddNode root = n4;

    Map<String, Boolean> truth = buildThruthTable(manager, root);

    System.out.println(manager.toDot(root, (x) -> Integer.toString(x.refCount(), 10), true));

    BddNode res = manager.Swap(root, rdict.get("x5"), rdict.get("x6"));

    checkThruthTable(truth, res);

    System.out.println(manager.toDot(root, (x) -> Integer.toString(x.refCount(), 10), true));
  }
}
