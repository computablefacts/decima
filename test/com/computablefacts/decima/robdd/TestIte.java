package com.computablefacts.decima.robdd;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class TestIte {

  @Test
  public void testSimpleITE() {

    BddManager manager = new BddManager(4);

    BddNode b = manager.create(1, manager.One, manager.Zero);
    BddNode f = manager.create(0, manager.One, b);

    BddNode c = manager.create(2, manager.One, manager.Zero);
    BddNode g = manager.create(0, c, manager.Zero);

    BddNode d = manager.create(3, manager.One, manager.Zero);
    BddNode h = manager.create(1, manager.One, d);

    Map<Integer, String> dict = new HashMap<>();
    dict.put(0, "a");
    dict.put(1, "b");
    dict.put(2, "c");
    dict.put(3, "d");

    BddNode res = manager.ite(f, g, h);
    manager.reduce(res);

    Assert.assertEquals(0, res.index());
    Assert.assertEquals(2, res.high().index());
    Assert.assertEquals(1, res.low().index());
    Assert.assertEquals(3, res.low().low().index());

    Assert.assertEquals(true, res.high().high().value());
    Assert.assertEquals(false, res.high().low().value());
    Assert.assertEquals(false, res.low().high().value());
    Assert.assertEquals(true, res.low().low().high().value());
    Assert.assertEquals(false, res.low().low().low().value());
  }
}
