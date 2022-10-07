package com.computablefacts.decima.robdd;

import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class TestReduce extends TestBdd {

  @Test
  public void testReduceSimple() {

    BddManager manager = new BddManager(3);
    BddNode n3 = manager.create(2, manager.One, manager.One);
    BddNode n4 = manager.create(1, n3, manager.Zero);
    BddNode n2 = manager.create(1, n3, manager.Zero);
    BddNode root = manager.create(0, n2, n4);

    Map<String, Boolean> truth = buildThruthTable(manager, root);
    BddNode res = manager.reduce(root);

    checkThruthTable(truth, res);

    Assert.assertEquals(1, res.index());
    Assert.assertEquals(false, res.low().value());
    Assert.assertEquals(true, res.high().value());
  }

  /**
   * Example from http://www.inf.ed.ac.uk/teaching/courses/ar/slides/bdd-ops.pdf
   */
  @Test
  public void testReduceInfEdAcUk() {

    BddManager manager = new BddManager(3);
    BddNode n1 = manager.create(2, manager.One, manager.Zero);
    BddNode n2 = manager.create(2, manager.One, manager.Zero);
    BddNode n3 = manager.create(1, n1, manager.Zero);
    BddNode n4 = manager.create(1, n2, n1);
    BddNode n5 = manager.create(0, n4, n3);

    Map<String, Boolean> truth = buildThruthTable(manager, n5);
    BddNode res = manager.reduce(n5);

    checkThruthTable(truth, res);

    Assert.assertEquals(0, res.index());
    Assert.assertEquals(1, res.low().index());
    Assert.assertEquals(2, res.low().high().index());
    Assert.assertEquals(2, res.high().index());

    Assert.assertEquals(false, res.low().low().value());
    Assert.assertEquals(false, res.low().high().low().value());
    Assert.assertEquals(false, res.high().low().value());

    Assert.assertEquals(true, res.high().high().value());
    Assert.assertEquals(true, res.low().high().high().value());
  }

  /**
   * Example from http://www.inf.unibz.it/~artale/FM/slide7.pdf
   */
  @Test
  public void testReduceInfUnibzIt() {

    BddManager manager = new BddManager(4);
    BddNode n1 = manager.create(3, manager.Zero, manager.Zero);
    BddNode n2 = manager.create(3, manager.One, manager.Zero);
    BddNode n3 = manager.create(3, manager.Zero, manager.Zero);
    BddNode n4 = manager.create(3, manager.One, manager.Zero);
    BddNode n5 = manager.create(3, manager.Zero, manager.Zero);
    BddNode n6 = manager.create(3, manager.One, manager.Zero);
    BddNode n7 = manager.create(3, manager.One, manager.One);
    BddNode n8 = manager.create(3, manager.One, manager.One);
    BddNode n9 = manager.create(2, n2, n1);
    BddNode n10 = manager.create(2, n4, n3);
    BddNode n11 = manager.create(2, n6, n5);
    BddNode n12 = manager.create(2, n8, n7);
    BddNode n13 = manager.create(1, n10, n9);
    BddNode n14 = manager.create(1, n12, n11);
    BddNode n15 = manager.create(0, n14, n13);

    Map<String, Boolean> truth = buildThruthTable(manager, n15);
    BddNode res = manager.reduce(n15);

    checkThruthTable(truth, res);

    Assert.assertEquals(0, res.index());
    Assert.assertEquals(2, res.low().index());
    Assert.assertEquals(3, res.low().high().index());
    Assert.assertEquals(1, res.high().index());
    Assert.assertEquals(2, res.high().low().index());

    Assert.assertEquals(false, res.low().low().value());
    Assert.assertEquals(false, res.low().high().low().value());
    Assert.assertEquals(false, res.high().low().low().value());

    Assert.assertEquals(true, res.high().high().value());
    Assert.assertEquals(true, res.low().high().high().value());
  }
}
