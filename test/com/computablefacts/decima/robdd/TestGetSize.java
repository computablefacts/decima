package com.computablefacts.decima.robdd;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class TestGetSize {

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

    Assert.assertEquals(3, manager.size(a13));
    Assert.assertEquals(4, manager.size(a12));
    Assert.assertEquals(3, manager.size(a11));
    Assert.assertEquals(3, manager.size(a10));
    Assert.assertEquals(4, manager.size(a9));
    Assert.assertEquals(4, manager.size(a8));
    Assert.assertEquals(5, manager.size(a7));
    Assert.assertEquals(4, manager.size(a6));
    Assert.assertEquals(6, manager.size(a5));
    Assert.assertEquals(6, manager.size(a4));
    Assert.assertEquals(8, manager.size(a3));
    Assert.assertEquals(8, manager.size(a2));
    Assert.assertEquals(12, manager.size(a1));
    Assert.assertEquals(16, manager.size(a0));

    BddNode res = manager.sifting(a0);
    int size = manager.size(res);

    Assert.assertEquals(8, size);
  }
}
