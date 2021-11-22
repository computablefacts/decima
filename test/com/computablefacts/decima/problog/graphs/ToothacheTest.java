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

public class ToothacheTest {

  @Test
  public void testToothache() {

    // Create kb
    InMemoryKnowledgeBase kb = kb();

    // Init kb with facts
    kb.azzert(parseClause("0.10::cavity(a)."));
    kb.azzert(parseClause("0.05::gum_disease(a)."));

    // Init kb with rules
    kb.azzert(parseClause("1.00::toothache(X) :- cavity(X), gum_disease(X)."));
    kb.azzert(parseClause("0.60::toothache(X) :- cavity(X), ~gum_disease(X)."));
    kb.azzert(parseClause("0.30::toothache(X) :- ~cavity(X), gum_disease(X)."));
    kb.azzert(parseClause("0.05::toothache(X) :- ~cavity(X), ~gum_disease(X)."));

    // Query kb
    // path(1, 6)?
    Solver solver = new Solver(kb, true);
    Literal query = new Literal("toothache", new Const("a"));
    Set<Clause> proofs = solver.proofs(query);

    // Verify subgoals
    Assert.assertEquals(8, solver.nbSubgoals());

    // Verify answers
    Assert.assertEquals(4, proofs.size());

    Assert.assertTrue(isValid(proofs, "toothache(a)",
        Lists.newArrayList("0.9::~cavity(a)", "0.95::~gum_disease(a)")));
    Assert.assertTrue(isValid(proofs, "toothache(a)",
        Lists.newArrayList("0.1::cavity(a)", "0.05::gum_disease(a)")));
    Assert.assertTrue(isValid(proofs, "toothache(a)",
        Lists.newArrayList("0.9::~cavity(a)", "0.05::gum_disease(a)")));
    Assert.assertTrue(isValid(proofs, "toothache(a)",
        Lists.newArrayList("0.1::cavity(a)", "0.95::~gum_disease(a)")));

    // Verify BDD answer
    // 0.11825::toothache(a).
    ProbabilityEstimator estimator = new ProbabilityEstimator(proofs);
    BigDecimal probability = estimator.probability(query, 5);

    Assert.assertEquals(0, BigDecimal.valueOf(0.11082).compareTo(probability));
  }
}
