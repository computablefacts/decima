package com.computablefacts.decima.problog.graphs;

import static com.computablefacts.decima.problog.TestUtils.isValid;
import static com.computablefacts.decima.problog.TestUtils.kb;
import static com.computablefacts.decima.problog.TestUtils.parseClause;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.computablefacts.decima.problog.Clause;
import com.computablefacts.decima.problog.Const;
import com.computablefacts.decima.problog.Estimator;
import com.computablefacts.decima.problog.InMemoryKnowledgeBase;
import com.computablefacts.decima.problog.Literal;
import com.computablefacts.decima.problog.Solver;
import com.google.common.collect.Lists;

/**
 * Extracted from Angelika Kimmig, Bart Demoen and Luc De Raedt (2010). "On the Implementation of
 * the Probabilistic Logic Programming Language ProbLog"
 */
public class Graph3Test {

  @Test
  public void testGraph() {

    // Create kb
    InMemoryKnowledgeBase kb = kb();

    // Init kb with facts
    kb.azzert(parseClause("0.8::edge(a, c)."));
    kb.azzert(parseClause("0.6::edge(b, c)."));
    kb.azzert(parseClause("0.7::edge(a, b)."));
    kb.azzert(parseClause("0.9::edge(c, d)."));
    kb.azzert(parseClause("0.8::edge(c, e)."));
    kb.azzert(parseClause("0.5::edge(e, d)."));

    // Init kb with rules
    kb.azzert(parseClause("path(X, Y) :- edge(X, Y)."));
    kb.azzert(parseClause("path(X, Y) :- path(X, Z), edge(Z, Y)."));

    // Query kb
    // path(a, d)?
    Solver solver = new Solver(kb);
    Literal query1 = new Literal("path", new Const("a"), new Const("d"));
    Set<Clause> proofs1 = solver.proofs(query1);

    // Verify subgoals
    Assert.assertEquals(12, solver.nbSubgoals());

    // Verify answers
    Assert.assertEquals(4, proofs1.size());

    Assert.assertTrue(
        isValid(proofs1, "path(a, d)", Lists.newArrayList("0.8::edge(a, c)", "0.9::edge(c, d)")));
    Assert.assertTrue(isValid(proofs1, "path(a, d)",
        Lists.newArrayList("0.7::edge(a, b)", "0.6::edge(b, c)", "0.9::edge(c, d)")));
    Assert.assertTrue(isValid(proofs1, "path(a, d)",
        Lists.newArrayList("0.8::edge(a, c)", "0.8::edge(c, e)", "0.5::edge(e, d)")));
    Assert.assertTrue(isValid(proofs1, "path(a, d)", Lists.newArrayList("0.7::edge(a, b)",
        "0.6::edge(b, c)", "0.8::edge(c, e)", "0.5::edge(e, d)")));

    // Verify BDD answer
    // 0.83096::path(a, d).
    Estimator estimator1 = new Estimator(proofs1);
    BigDecimal probability1 = estimator1.probability(query1, 5);

    Assert.assertTrue(BigDecimal.valueOf(0.83096).compareTo(probability1) == 0);

    // Query kb
    // path(c, d)?
    Literal query2 = new Literal("path", new Const("c"), new Const("d"));
    Set<Clause> proofs2 = solver.proofs(query2);

    // Verify answers
    Assert.assertEquals(2, proofs2.size());

    Assert.assertTrue(
        isValid(proofs2, "path(c, d)", Lists.newArrayList("0.8::edge(c, e)", "0.5::edge(e, d)")));
    Assert.assertTrue(isValid(proofs2, "path(c, d)", Lists.newArrayList("0.9::edge(c, d)")));

    // Verify BDD answer
    // 0.94::path(c, d).
    Estimator estimator2 = new Estimator(proofs2);
    BigDecimal probability2 = estimator2.probability(query2, 2);

    Assert.assertTrue(BigDecimal.valueOf(0.94).compareTo(probability2) == 0);
  }
}
