package com.computablefacts.decima.robdd;

import com.google.errorprone.annotations.Var;
import java.util.Random;
import org.junit.Test;

public class TestLargeModel {

  private BddManager manager;
  private Random r;

  @Test(timeout = 10 * 1000)
  public void testLarge01() {

    int m1 = 3;
    int m2 = 1;

    manager = new BddManager(0);
    r = new Random();

    @Var BddNode bdd = generate(0, m1, m2);

    System.out.println("Number of nodes in BDD: " + manager.size(bdd) + "/" + manager.N());

    bdd = manager.sifting(bdd);

    System.out.println("Number of nodes in BDD: " + manager.size(bdd));
  }

  private BddNode generate(int height, int max, int m2) {

    if (height > max) {
      int id = manager.createVariable();
      return manager.create(id, manager.Zero, manager.One);
    }

    @Var BddNode acc = getAndOr(height, max, m2);

    for (int i = 0; i < m2; i++) {
      acc = manager.and(acc, generate(height + 1, max, m2));
    }
    return acc;
  }

  private BddNode getAndOr(int height, int max, int m2) {
    if (r.nextDouble() > .5) {
      return manager.or(generate(height + 1, max, m2), generate(height + 1, max, m2));
    }
    return manager.and(generate(height + 1, max, m2), generate(height + 1, max, m2));
  }
}
