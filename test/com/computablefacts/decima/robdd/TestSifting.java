package com.computablefacts.decima.robdd;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class TestSifting extends TestBdd {

  @Test
  public void testSwapSimple() {

    BddManager manager = new BddManager(3);
    BddNode n2 = manager.create(2, manager.One, manager.Zero);
    BddNode n3 = manager.create(1, manager.One, n2);
    BddNode n4 = manager.create(1, manager.Zero, manager.One);
    BddNode root = manager.create(0, n3, n4);

    Map<String, Boolean> truth = buildThruthTable(manager, root);
    BddNode res = manager.sifting(root);

    checkThruthTable(truth, res);
  }

  @Test
  public void testDiamond() {

    Map<Integer, String> dict = new HashMap<>();
    dict.put(0, "x0");
    dict.put(1, "x1");
    dict.put(2, "x2");
    dict.put(3, "x3");
    dict.put(4, "x4");
    dict.put(5, "x5");

    Map<String, Integer> rdict = new HashMap<>();
    rdict.put("x0", 0);
    rdict.put("x1", 1);
    rdict.put("x2", 2);
    rdict.put("x3", 3);
    rdict.put("x4", 4);
    rdict.put("x5", 5);

    BddManager manager = new BddManager(6);
    manager.variableString((x) -> x < 6 ? dict.get(x) : "sink");

    BddNode a1 = manager.create(rdict.get("x0"), 0, 1);
    BddNode a2 = manager.create(rdict.get("x2"), 0, a1);
    BddNode a3 = manager.create(rdict.get("x2"), a1, 1);
    BddNode a4 = manager.create(rdict.get("x3"), a2, a3);

    Map<String, Boolean> truth = buildThruthTable(manager, a4);
    BddNode res = manager.sifting(a4);

    checkThruthTable(truth, res);
  }

  /**
   * From Algorithms and data structures in VLSI design, page 124.
   */
  @Test
  public void testComplex() {

    Map<Integer, String> dict = new HashMap<>();
    dict.put(0, "x1");
    dict.put(1, "x3");
    dict.put(2, "x5");
    dict.put(3, "x2");
    dict.put(4, "x4");
    dict.put(5, "x6");

    Map<String, Integer> rdict = new HashMap<>();
    rdict.put("x1", 0);
    rdict.put("x3", 1);
    rdict.put("x5", 2);
    rdict.put("x2", 3);
    rdict.put("x4", 4);
    rdict.put("x6", 5);

    BddManager manager = new BddManager(6);
    manager.variableString((x) -> x < 6 ? dict.get(x) : "sink");

    BddNode a13 = manager.create(rdict.get("x6"), manager.One, manager.Zero);
    BddNode a12 = manager.create(rdict.get("x4"), manager.One, a13);
    BddNode a11 = manager.create(rdict.get("x4"), manager.One, manager.Zero);
    BddNode a10 = manager.create(rdict.get("x2"), manager.One, manager.Zero);
    BddNode a9 = manager.create(rdict.get("x2"), manager.One, a13);
    BddNode a8 = manager.create(rdict.get("x2"), manager.One, a11);
    BddNode a7 = manager.create(rdict.get("x2"), manager.One, a12);
    BddNode a6 = manager.create(rdict.get("x5"), a13, manager.Zero);
    BddNode a5 = manager.create(rdict.get("x5"), a12, a11);
    BddNode a4 = manager.create(rdict.get("x5"), a9, a10);
    BddNode a3 = manager.create(rdict.get("x5"), a7, a8);
    BddNode a2 = manager.create(rdict.get("x3"), a5, a6);
    BddNode a1 = manager.create(rdict.get("x3"), a3, a4);
    BddNode a0 = manager.create(rdict.get("x1"), a1, a2);

    Map<String, Boolean> truth = buildThruthTable(manager, a0);

    System.out.println(manager.toDot(a0, (x) -> "x" + x.index() + " (" + x.refCount() + ")", true));

    BddNode res = manager.sifting(a0);

    System.out.println(manager.toDot(res, (x) -> "x" + x.index() + " (" + x.refCount() + ")", true));

    checkThruthTable(truth, res);

    int size = manager.size(res);
    Assert.assertEquals(8, size);
  }
}
