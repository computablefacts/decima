package com.computablefacts.decima.problog.graphs;

import static com.computablefacts.decima.problog.Parser.parseClause;
import static com.computablefacts.decima.problog.TestUtils.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.computablefacts.asterix.trie.Trie;
import com.computablefacts.decima.problog.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Extracted from https://dtai.cs.kuleuven.be/problog/tutorial/basic/04_pgraph.html
 */
public class GraphWithoutCycle3Test {

  @Test
  public void testGraph() {

    // Create kb
    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();

    // Init kb with facts
    kb.azzert(parseClause("0.6::edge(1, 2)."));
    kb.azzert(parseClause("0.1::edge(1, 3)."));
    kb.azzert(parseClause("0.4::edge(2, 5)."));
    kb.azzert(parseClause("0.3::edge(2, 6)."));
    kb.azzert(parseClause("0.3::edge(3, 4)."));
    kb.azzert(parseClause("0.8::edge(4, 5)."));
    kb.azzert(parseClause("0.2::edge(5, 6)."));

    // Init kb with rules
    kb.azzert(parseClause("path(X, Y) :- edge(X, Y)."));
    kb.azzert(parseClause("path(X, Y) :- path(X, Z), edge(Z, Y)."));

    // Query kb
    // path(1, 6)?
    Solver solver = new Solver(kb, true);
    Literal query = new Literal("path", new Const("1"), new Const("6"));
    Set<Clause> proofs = solver.proofs(query);
    Set<Clause> answers = Sets.newHashSet(solver.solve(query));
    Map<Literal, Trie<Literal>> tries = solver.tries(query);

    // Verify subgoals
    Assert.assertEquals(18, solver.nbSubgoals());

    // Verify answers
    // path(1, 6) :- 0.6::edge(1, 2), 0.3::edge(2, 6).
    // path(1, 6) :- 0.6::edge(1, 2), 0.4::edge(2, 5), 0.2::edge(5, 6).
    // path(1, 6) :- 0.1::edge(1, 3), 0.3::edge(3, 4), 0.8::edge(4, 5), 0.2::edge(5, 6).
    Assert.assertEquals(3, proofs.size());
    Assert.assertEquals(1, answers.size());
    Assert.assertEquals(1, tries.size());

    Clause answer1 =
        buildClause("path(1, 6)", Lists.newArrayList("0.6::edge(1, 2)", "0.3::edge(2, 6)"));
    Clause answer2 = buildClause("path(1, 6)",
        Lists.newArrayList("0.6::edge(1, 2)", "0.4::edge(2, 5)", "0.2::edge(5, 6)"));
    Clause answer3 = buildClause("path(1, 6)", Lists.newArrayList("0.1::edge(1, 3)",
        "0.3::edge(3, 4)", "0.8::edge(4, 5)", "0.2::edge(5, 6)"));

    Assert.assertTrue(checkAnswers(answers, Sets.newHashSet(answer1, answer2, answer3)));
    Assert.assertTrue(checkProofs(tries, Sets.newHashSet(answer1, answer2, answer3)));

    // Verify BDD answer
    // 0.2167296::path(1, 6).
    ProbabilityEstimator estimator = new ProbabilityEstimator(proofs);
    BigDecimal probability = estimator.probability(query, 7);

    Assert.assertEquals(0, BigDecimal.valueOf(0.2167296).compareTo(probability));
  }
}
