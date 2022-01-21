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
 * Extracted from Mantadelis, Theofrastos & Janssens, Gerda. (2010). "Dedicated Tabling for a
 * Probabilistic Setting". Technical Communications of ICLP. 7. 124-133.
 * 10.4230/LIPIcs.ICLP.2010.124.
 */
public class GraphWithCycle1Test {

  @Test
  public void testGraph() {

    // Create kb
    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();

    // Init kb with facts
    kb.azzert(parseClause("0.9::edge(1, 2)."));//
    // kb.azzert(parseClause("0.9::edge(2, 1)."));
    kb.azzert(parseClause("0.2::edge(5, 4)."));//
    kb.azzert(parseClause("0.4::edge(6, 5)."));//
    // kb.azzert(parseClause("0.4::edge(5, 6)."));
    // kb.azzert(parseClause("0.2::edge(4, 5)."));
    kb.azzert(parseClause("0.8::edge(2, 3)."));//
    // kb.azzert(parseClause("0.8::edge(3, 2)."));
    kb.azzert(parseClause("0.7::edge(1, 6)."));//
    kb.azzert(parseClause("0.5::edge(2, 6)."));//
    kb.azzert(parseClause("0.5::edge(6, 2)."));//
    // kb.azzert(parseClause("0.7::edge(6, 1)."));
    kb.azzert(parseClause("0.7::edge(5, 3)."));//
    kb.azzert(parseClause("0.7::edge(3, 5)."));//
    kb.azzert(parseClause("0.6::edge(3, 4)."));//
    // kb.azzert(parseClause("0.6::edge(4, 3)."));

    // Init kb with rules
    kb.azzert(parseClause("path(X, Y) :- edge(X, Y)."));
    kb.azzert(parseClause("path(X, Y) :- edge(X, Z), fn_is_false(fn_eq(Z, Y)),  path(Z, Y)."));

    // Query kb
    // path(1, 4)?
    Solver solver = new Solver(kb, true);
    Literal query = new Literal("path", new Const("1"), new Const("4"));
    Set<Clause> proofs = solver.proofs(query);
    Set<Clause> answers = Sets.newHashSet(solver.solve(query));
    Map<Literal, Trie<Literal>> tries = solver.tries(query);

    // Verify subgoals
    Assert.assertEquals(18, solver.nbSubgoals());

    // Verify proofs
    Assert.assertEquals(13, proofs.size());
    Assert.assertEquals(1, answers.size());
    Assert.assertEquals(1, tries.size());

    Clause answer1 = buildClause("path(1, 4)",
        Lists.newArrayList("0.7::edge(1, 6)", "0.5::edge(6, 2)", "0.8::edge(2, 3)",
            "0.7::edge(3, 5)", "0.2::edge(5, 4)", "fn_eq(false, 5, 4)", "fn_is_false(false)",
            "fn_eq(false, 3, 4)", "fn_is_false(false)", "fn_eq(false, 2, 4)", "fn_is_false(false)",
            "fn_eq(false, 6, 4)", "fn_is_false(false)"));
    Clause answer2 = buildClause("path(1, 4)",
        Lists.newArrayList("0.9::edge(1, 2)", "0.5::edge(2, 6)", "0.4::edge(6, 5)",
            "0.7::edge(5, 3)", "0.6::edge(3, 4)", "fn_eq(false, 3, 4)", "fn_is_false(false)",
            "fn_eq(false, 5, 4)", "fn_is_false(false)", "fn_eq(false, 6, 4)", "fn_is_false(false)",
            "fn_eq(false, 2, 4)", "fn_is_false(false)"));
    Clause answer3 = buildClause("path(1, 4)",
        Lists.newArrayList("0.7::edge(1, 6)", "0.4::edge(6, 5)", "0.7::edge(5, 3)",
            "0.7::edge(3, 5)", "0.2::edge(5, 4)", "fn_eq(false, 5, 4)", "fn_is_false(false)",
            "fn_eq(false, 3, 4)", "fn_is_false(false)", "fn_eq(false, 5, 4)", "fn_is_false(false)",
            "fn_eq(false, 6, 4)", "fn_is_false(false)"));
    Clause answer4 = buildClause("path(1, 4)",
        Lists.newArrayList("0.9::edge(1, 2)", "0.5::edge(2, 6)", "0.4::edge(6, 5)",
            "0.7::edge(5, 3)", "0.7::edge(3, 5)", "0.2::edge(5, 4)", "fn_eq(false, 5, 4)",
            "fn_is_false(false)", "fn_eq(false, 3, 4)", "fn_is_false(false)", "fn_eq(false, 5, 4)",
            "fn_is_false(false)", "fn_eq(false, 6, 4)", "fn_is_false(false)", "fn_eq(false, 2, 4)",
            "fn_is_false(false)"));
    Clause answer5 = buildClause("path(1, 4)",
        Lists.newArrayList("0.7::edge(1, 6)", "0.4::edge(6, 5)", "0.7::edge(5, 3)",
            "0.6::edge(3, 4)", "fn_eq(false, 3, 4)", "fn_is_false(false)", "fn_eq(false, 5, 4)",
            "fn_is_false(false)", "fn_eq(false, 6, 4)", "fn_is_false(false)"));
    Clause answer6 = buildClause("path(1, 4)",
        Lists.newArrayList("0.9::edge(1, 2)", "0.8::edge(2, 3)", "0.7::edge(3, 5)",
            "0.2::edge(5, 4)", "fn_eq(false, 5, 4)", "fn_is_false(false)", "fn_eq(false, 3, 4)",
            "fn_is_false(false)", "fn_eq(false, 2, 4)", "fn_is_false(false)"));
    Clause answer7 = buildClause("path(1, 4)",
        Lists.newArrayList("0.9::edge(1, 2)", "0.5::edge(2, 6)", "0.4::edge(6, 5)",
            "0.2::edge(5, 4)", "fn_eq(false, 5, 4)", "fn_is_false(false)", "fn_eq(false, 6, 4)",
            "fn_is_false(false)", "fn_eq(false, 2, 4)", "fn_is_false(false)"));
    Clause answer8 = buildClause("path(1, 4)",
        Lists.newArrayList("0.7::edge(1, 6)", "0.5::edge(6, 2)", "0.5::edge(2, 6)",
            "0.4::edge(6, 5)", "0.7::edge(5, 3)", "0.6::edge(3, 4)", "fn_eq(false, 3, 4)",
            "fn_is_false(false)", "fn_eq(false, 5, 4)", "fn_is_false(false)", "fn_eq(false, 6, 4)",
            "fn_is_false(false)", "fn_eq(false, 2, 4)", "fn_is_false(false)", "fn_eq(false, 6, 4)",
            "fn_is_false(false)"));
    Clause answer9 = buildClause("path(1, 4)",
        Lists.newArrayList("0.7::edge(1, 6)", "0.5::edge(6, 2)", "0.5::edge(2, 6)",
            "0.4::edge(6, 5)", "0.7::edge(5, 3)", "0.7::edge(3, 5)", "0.2::edge(5, 4)",
            "fn_eq(false, 5, 4)", "fn_is_false(false)", "fn_eq(false, 3, 4)", "fn_is_false(false)",
            "fn_eq(false, 5, 4)", "fn_is_false(false)", "fn_eq(false, 6, 4)", "fn_is_false(false)",
            "fn_eq(false, 2, 4)", "fn_is_false(false)", "fn_eq(false, 6, 4)",
            "fn_is_false(false)"));
    Clause answer10 = buildClause("path(1, 4)",
        Lists.newArrayList("0.9::edge(1, 2)", "0.8::edge(2, 3)", "0.6::edge(3, 4)",
            "fn_eq(false, 3, 4)", "fn_is_false(false)", "fn_eq(false, 2, 4)",
            "fn_is_false(false)"));
    Clause answer11 = buildClause("path(1, 4)",
        Lists.newArrayList("0.7::edge(1, 6)", "0.5::edge(6, 2)", "0.5::edge(2, 6)",
            "0.4::edge(6, 5)", "0.2::edge(5, 4)", "fn_eq(false, 5, 4)", "fn_is_false(false)",
            "fn_eq(false, 6, 4)", "fn_is_false(false)", "fn_eq(false, 2, 4)", "fn_is_false(false)",
            "fn_eq(false, 6, 4)", "fn_is_false(false)"));
    Clause answer12 = buildClause("path(1, 4)",
        Lists.newArrayList("0.7::edge(1, 6)", "0.5::edge(6, 2)", "0.8::edge(2, 3)",
            "0.6::edge(3, 4)", "fn_eq(false, 3, 4)", "fn_is_false(false)", "fn_eq(false, 2, 4)",
            "fn_is_false(false)", "fn_eq(false, 6, 4)", "fn_is_false(false)"));
    Clause answer13 = buildClause("path(1, 4)",
        Lists.newArrayList("0.7::edge(1, 6)", "0.4::edge(6, 5)", "0.2::edge(5, 4)",
            "fn_eq(false, 5, 4)", "fn_is_false(false)", "fn_eq(false, 6, 4)",
            "fn_is_false(false)"));

    Assert.assertTrue(checkAnswers(answers, Sets.newHashSet(answer1, answer2, answer3, answer4,
        answer5, answer6, answer7, answer8, answer9, answer10, answer11, answer12, answer13)));
    Assert
        .assertTrue(checkProofs(tries, Sets.newHashSet(answer1, answer2, answer3, answer4, answer5,
            answer6, answer7, answer8, answer9, answer10, answer11, answer12, answer13), true));

    // Verify BDD answer
    // 0.53864::path(1, 4).
    ProbabilityEstimator estimator = new ProbabilityEstimator(proofs);
    BigDecimal probability = estimator.probability(query, 5);

    Assert.assertEquals(0, BigDecimal.valueOf(0.53864).compareTo(probability));
  }
}
