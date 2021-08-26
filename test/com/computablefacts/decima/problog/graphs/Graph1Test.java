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
 * Extracted from Mantadelis, Theofrastos & Janssens, Gerda. (2010). "Dedicated Tabling for a
 * Probabilistic Setting". Technical Communications of ICLP. 7. 124-133.
 * 10.4230/LIPIcs.ICLP.2010.124.
 */
public class Graph1Test {

  @Test
  public void testGraph() {

    // Create kb
    InMemoryKnowledgeBase kb = kb();

    // Init kb with facts
    kb.azzert(parseClause("0.9::edge(1, 2)."));
    kb.azzert(parseClause("0.9::edge(2, 1)."));
    kb.azzert(parseClause("0.2::edge(5, 4)."));
    kb.azzert(parseClause("0.4::edge(6, 5)."));
    kb.azzert(parseClause("0.4::edge(5, 6)."));
    kb.azzert(parseClause("0.2::edge(4, 5)."));
    kb.azzert(parseClause("0.8::edge(2, 3)."));
    kb.azzert(parseClause("0.8::edge(3, 2)."));
    kb.azzert(parseClause("0.7::edge(1, 6)."));
    kb.azzert(parseClause("0.5::edge(2, 6)."));
    kb.azzert(parseClause("0.5::edge(6, 2)."));
    kb.azzert(parseClause("0.7::edge(6, 1)."));
    kb.azzert(parseClause("0.7::edge(5, 3)."));
    kb.azzert(parseClause("0.7::edge(3, 5)."));
    kb.azzert(parseClause("0.6::edge(3, 4)."));
    kb.azzert(parseClause("0.6::edge(4, 3)."));

    // Init kb with rules
    kb.azzert(parseClause("path(X, Y) :- edge(X, Y)."));
    kb.azzert(parseClause("path(X, Y) :- path(X, Z), fn_eq(U, X, Z), fn_is_false(U), edge(Z, Y)."));

    // Query kb
    // path(1, 4)?
    Solver solver = new Solver(kb);
    Literal query = new Literal("path", new Const("1"), new Const("4"));
    Set<Clause> proofs = solver.proofs(query);

    // Verify subgoals
    Assert.assertEquals(14, solver.nbSubgoals());

    // Verify answers
    Assert.assertEquals(8, proofs.size());

    Assert.assertTrue(isValid(proofs, "path(1, 4)", Lists.newArrayList("0.9::edge(1,2)",
        "0.8::edge(2,3)", "0.7::edge(3,5)", "0.2::edge(5,4)")));
    Assert.assertTrue(isValid(proofs, "path(1, 4)",
        Lists.newArrayList("0.9::edge(1,2)", "0.8::edge(2,3)", "0.6::edge(3,4)")));
    Assert.assertTrue(isValid(proofs, "path(1, 4)", Lists.newArrayList("0.9::edge(1,2)",
        "0.5::edge(2,6)", "0.4::edge(6,5)", "0.7::edge(5,3)", "0.6::edge(3,4)")));
    Assert.assertTrue(isValid(proofs, "path(1, 4)", Lists.newArrayList("0.9::edge(1,2)",
        "0.5::edge(2,6)", "0.4::edge(6,5)", "0.2::edge(5,4)")));
    Assert.assertTrue(isValid(proofs, "path(1, 4)", Lists.newArrayList("0.7::edge(1,6)",
        "0.5::edge(6,2)", "0.8::edge(2,3)", "0.7::edge(3,5)", "0.2::edge(5,4)")));
    Assert.assertTrue(isValid(proofs, "path(1, 4)", Lists.newArrayList("0.7::edge(1,6)",
        "0.5::edge(6,2)", "0.8::edge(2,3)", "0.6::edge(3,4)")));
    Assert.assertTrue(isValid(proofs, "path(1, 4)", Lists.newArrayList("0.7::edge(1,6)",
        "0.4::edge(6,5)", "0.7::edge(5,3)", "0.6::edge(3,4)")));
    Assert.assertTrue(isValid(proofs, "path(1, 4)",
        Lists.newArrayList("0.7::edge(1,6)", "0.4::edge(6,5)", "0.2::edge(5,4)")));

    // Verify BDD answer
    // 0.53864::path(1, 4).
    Estimator estimator = new Estimator(proofs);
    BigDecimal probability = estimator.probability(query, 5);

    Assert.assertTrue(BigDecimal.valueOf(0.53864).compareTo(probability) == 0);
  }
}
