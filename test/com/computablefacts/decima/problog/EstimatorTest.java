package com.computablefacts.decima.problog;

import static com.computablefacts.decima.problog.TestUtils.isValid;
import static com.computablefacts.decima.problog.TestUtils.kb;
import static com.computablefacts.decima.problog.TestUtils.parseClause;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Lists;

public class EstimatorTest {

  @Test
  public void testComputeProbabilityWithoutProofs() {
    Estimator estimator = new Estimator(new HashSet<>());
    Assert.assertEquals(BigDecimal.ZERO, estimator.probability());
  }

  /**
   * See https://github.com/ML-KULeuven/problog/blob/master/test/swap.pl
   */
  @Test
  public void testLiteralsSwapping1() {

    // Create kb
    InMemoryKnowledgeBase kb = kb();

    // Init kb with facts
    kb.azzert(parseClause("0.5::f(1,2)."));
    kb.azzert(parseClause("0.5::f(2,1)."));
    kb.azzert(parseClause("0.5::f(1,3)."));
    kb.azzert(parseClause("0.5::f(2,3)."));
    kb.azzert(parseClause("0.5::b(1)."));
    kb.azzert(parseClause("0.5::b(2)."));
    kb.azzert(parseClause("0.5::b(3)."));

    // Init kb with rules
    kb.azzert(parseClause("s1(X) :- b(X)."));
    kb.azzert(parseClause("s1(X) :- f(X,Y),s1(Y)."));
    kb.azzert(parseClause("s2(X) :- f(X,Y),s2(Y)."));
    kb.azzert(parseClause("s2(X) :- b(X)."));

    // Query kb
    // s1(1)?
    Solver solver = new Solver(kb);
    Literal query = new Literal("s1", new Const(1));
    Set<Clause> proofs = solver.proofs(query);

    // Verify answers
    Assert.assertEquals(5, proofs.size());
    Assert.assertTrue(isValid(proofs, "s1(1)", Lists.newArrayList("0.5::b(1)")));
    Assert.assertTrue(isValid(proofs, "s1(1)", Lists.newArrayList("0.5::f(1, 2)", "0.5::b(2)")));
    Assert.assertTrue(isValid(proofs, "s1(1)", Lists.newArrayList("0.5::f(1, 3)", "0.5::b(3)")));
    Assert.assertTrue(
        isValid(proofs, "s1(1)", Lists.newArrayList("0.5::f(1, 2)", "0.5::f(2, 1)", "0.5::b(1)")));
    Assert.assertTrue(
        isValid(proofs, "s1(1)", Lists.newArrayList("0.5::f(1, 2)", "0.5::f(2, 3)", "0.5::b(3)")));

    // Verify BDD answer
    // 0.734375::s1(1).
    Estimator estimator = new Estimator(proofs);
    BigDecimal probability = estimator.probability();

    Assert.assertEquals(0, BigDecimal.valueOf(0.734375).compareTo(probability));
  }

  /**
   * See https://github.com/ML-KULeuven/problog/blob/master/test/swap.pl
   */
  @Test
  public void testLiteralsSwapping2() {

    // Create kb
    InMemoryKnowledgeBase kb = kb();

    // Init kb with facts
    kb.azzert(parseClause("0.5::f(1,2)."));
    kb.azzert(parseClause("0.5::f(2,1)."));
    kb.azzert(parseClause("0.5::f(1,3)."));
    kb.azzert(parseClause("0.5::f(2,3)."));
    kb.azzert(parseClause("0.5::b(1)."));
    kb.azzert(parseClause("0.5::b(2)."));
    kb.azzert(parseClause("0.5::b(3)."));

    // Init kb with rules
    kb.azzert(parseClause("s1(X) :- b(X)."));
    kb.azzert(parseClause("s1(X) :- f(X,Y),s1(Y)."));
    kb.azzert(parseClause("s2(X) :- f(X,Y),s2(Y)."));
    kb.azzert(parseClause("s2(X) :- b(X)."));

    // Query kb
    // s2(1)?
    Solver solver = new Solver(kb);
    Literal query = new Literal("s2", new Const(1));
    Set<Clause> proofs = solver.proofs(query);

    // Verify answers
    Assert.assertEquals(5, proofs.size());
    Assert.assertTrue(isValid(proofs, "s2(1)", Lists.newArrayList("0.5::b(1)")));
    Assert.assertTrue(isValid(proofs, "s2(1)", Lists.newArrayList("0.5::f(1, 2)", "0.5::b(2)")));
    Assert.assertTrue(isValid(proofs, "s2(1)", Lists.newArrayList("0.5::f(1, 3)", "0.5::b(3)")));
    Assert.assertTrue(
        isValid(proofs, "s2(1)", Lists.newArrayList("0.5::f(1, 2)", "0.5::f(2, 1)", "0.5::b(1)")));
    Assert.assertTrue(
        isValid(proofs, "s2(1)", Lists.newArrayList("0.5::f(1, 2)", "0.5::f(2, 3)", "0.5::b(3)")));

    // Verify BDD answer
    // 0.734375::s2(1).
    Estimator estimator = new Estimator(proofs);
    BigDecimal probability = estimator.probability();

    Assert.assertEquals(0, BigDecimal.valueOf(0.734375).compareTo(probability));
  }

  /**
   * Non-ground query
   *
   * See https://github.com/ML-KULeuven/problog/blob/master/test/non_ground_query.pl
   */
  @Test
  public void testNonGroundQuery() {

    // Create kb
    InMemoryKnowledgeBase kb = kb();

    // Init kb with facts
    kb.azzert(parseClause("0.1::b(1)."));
    kb.azzert(parseClause("0.2::b(2)."));
    kb.azzert(parseClause("0.3::e(1)."));
    kb.azzert(parseClause("0.4::e(3)."));
    kb.azzert(parseClause("d(1)."));
    kb.azzert(parseClause("d(2)."));
    kb.azzert(parseClause("d(3)."));

    // Init kb with rules
    kb.azzert(parseClause("a(X) :- b(2), c(X,Y)."));
    kb.azzert(parseClause("c(X,Y) :- c(X,Z), c(Z,Y)."));
    kb.azzert(parseClause("c(X,Y) :- d(X), d(Y)."));

    // Query kb
    // a(X)?
    Solver solver = new Solver(kb);
    Literal query = new Literal("a", new Var());
    Set<Clause> proofs = solver.proofs(query);

    // Verify answers
    // TODO : check the number of clauses

    // Verify BDD answer
    // 0.2::a(1).
    // 0.2::a(2).
    // 0.2::a(3).
    Estimator estimator = new Estimator(proofs);
    Map<Clause, BigDecimal> probabilities = estimator.probabilities();

    Clause a1 = new Clause(new Literal("a", new Const(1)));
    Clause a2 = new Clause(new Literal("a", new Const(2)));
    Clause a3 = new Clause(new Literal("a", new Const(3)));

    Assert.assertEquals(BigDecimal.valueOf(0.2), probabilities.get(a1));
    Assert.assertEquals(BigDecimal.valueOf(0.2), probabilities.get(a2));
    Assert.assertEquals(BigDecimal.valueOf(0.2), probabilities.get(a3));
  }
}
