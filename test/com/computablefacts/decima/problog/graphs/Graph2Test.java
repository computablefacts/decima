package com.computablefacts.decima.problog.graphs;

import static com.computablefacts.decima.problog.TestUtils.*;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.computablefacts.decima.problog.*;
import com.google.common.collect.Lists;

/**
 * Extracted from Mantadelis, Theofrastos & Janssens, Gerda. (2010). "Dedicated Tabling for a
 * Probabilistic Setting". Technical Communications of ICLP. 7. 124-133.
 * 10.4230/LIPIcs.ICLP.2010.124.
 */
public class Graph2Test {

  @Test
  public void testGraph() {

    // Create kb
    InMemoryKnowledgeBase kb = kb();

    // Init kb with facts
    kb.azzert(parseClause("0.1::edge(1, 2)."));
    kb.azzert(parseClause("0.5::edge(1, 3)."));
    kb.azzert(parseClause("0.7::edge(3, 1)."));
    kb.azzert(parseClause("0.3::edge(2, 3)."));
    kb.azzert(parseClause("0.2::edge(3, 2)."));
    kb.azzert(parseClause("0.6::edge(2, 4)."));

    // Init kb with rules
    kb.azzert(parseClause("path(X, Y) :- edge(X, Y)."));
    kb.azzert(parseClause("path(X, Y) :- path(X, Z), fn_eq(U, X, Z), fn_is_false(U), edge(Z, Y)."));

    // Query kb
    // path(1, 4)?
    Solver solver = new Solver(kb, true);
    Literal query = new Literal("path", new Const("1"), new Const("4"));
    Set<Clause> proofs = solver.proofs(query);

    // Verify subgoals
    Assert.assertEquals(10, solver.nbSubgoals());

    // Verify answers
    Assert.assertEquals(3, proofs.size());

    Assert.assertTrue(
        isValid(proofs, "path(1,4)", Lists.newArrayList("0.1::edge(1,2)", "0.6::edge(2,4)")));
    Assert.assertTrue(isValid(proofs, "path(1,4)",
        Lists.newArrayList("0.5::edge(1,3)", "0.2::edge(3,2)", "0.6::edge(2,4)")));
    Assert.assertTrue(isValid(proofs, "path(1,4)", Lists.newArrayList("0.1::edge(1,2)",
        "0.3::edge(2,3)", "0.2::edge(3,2)", "0.6::edge(2,4)")));

    // Verify BDD answer
    // 0.114::path(1, 4).
    ProbabilityEstimator estimator = new ProbabilityEstimator(proofs);
    BigDecimal probability = estimator.probability(query, 3);

    Assert.assertTrue(BigDecimal.valueOf(0.114).compareTo(probability) == 0);
  }
}
