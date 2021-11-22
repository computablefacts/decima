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
import com.computablefacts.decima.problog.ProbabilityEstimator;
import com.computablefacts.decima.problog.InMemoryKnowledgeBase;
import com.computablefacts.decima.problog.Literal;
import com.computablefacts.decima.problog.Solver;
import com.google.common.collect.Lists;

/**
 * Extracted from Theofrastos Mantadelis and Gerda Janssens (2010). "Nesting Probabilistic
 * Inference"
 */
public class Graph4Test {

  @Test
  public void testGraph() {

    // Create kb
    InMemoryKnowledgeBase kb = kb();

    // Init kb with facts
    kb.azzert(parseClause("0.4::edge(a, b)."));
    kb.azzert(parseClause("0.55::edge(a, c)."));
    kb.azzert(parseClause("0.8::edge(b, e)."));
    kb.azzert(parseClause("0.2::edge(b, d)."));
    kb.azzert(parseClause("0.4::edge(c, d)."));
    kb.azzert(parseClause("0.3::edge(e, f)."));
    kb.azzert(parseClause("0.5::edge(d, f)."));
    kb.azzert(parseClause("0.6::edge(d, g)."));
    kb.azzert(parseClause("0.7::edge(f, h)."));
    kb.azzert(parseClause("0.7::edge(g, h)."));

    // Init kb with rules
    kb.azzert(parseClause("path(X, Y) :- edge(X, Y)."));
    kb.azzert(parseClause("path(X, Y) :- path(X, Z), edge(Z, Y)."));

    // Query kb
    // path(b, f)?
    Solver solver = new Solver(kb, true);
    Literal query = new Literal("path", new Const("b"), new Const("f"));
    Set<Clause> proofs = solver.proofs(query);

    // Verify subgoals
    Assert.assertEquals(14, solver.nbSubgoals());

    // Verify answers
    // path(b, f) :- 0.2::edge(b, d), 0.5::edge(d, f).
    // path(b, f) :- 0.8::edge(b, e), 0.3::edge(e, f).
    Assert.assertEquals(2, proofs.size());

    Assert.assertTrue(
        isValid(proofs, "path(b, f)", Lists.newArrayList("0.2::edge(b, d)", "0.5::edge(d, f)")));
    Assert.assertTrue(
        isValid(proofs, "path(b, f)", Lists.newArrayList("0.8::edge(b, e)", "0.3::edge(e, f)")));

    // Verify BDD answer
    // 0.316::path(b, f).
    ProbabilityEstimator estimator = new ProbabilityEstimator(proofs);
    BigDecimal probability = estimator.probability(query, 3);

    Assert.assertTrue(BigDecimal.valueOf(0.316).compareTo(probability) == 0);
  }
}
