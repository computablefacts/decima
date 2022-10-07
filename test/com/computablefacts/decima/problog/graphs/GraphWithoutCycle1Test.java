package com.computablefacts.decima.problog.graphs;

import static com.computablefacts.decima.problog.AbstractTerm.newConst;
import static com.computablefacts.decima.problog.Parser.parseClause;
import static com.computablefacts.decima.problog.TestUtils.buildClause;
import static com.computablefacts.decima.problog.TestUtils.checkAnswers;
import static com.computablefacts.decima.problog.TestUtils.checkProofs;

import com.computablefacts.asterix.trie.Trie;
import com.computablefacts.decima.problog.Clause;
import com.computablefacts.decima.problog.InMemoryKnowledgeBase;
import com.computablefacts.decima.problog.Literal;
import com.computablefacts.decima.problog.ProbabilityEstimator;
import com.computablefacts.decima.problog.Solver;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

/**
 * Extracted from Theofrastos Mantadelis and Gerda Janssens (2010). "Nesting Probabilistic Inference"
 */
public class GraphWithoutCycle1Test {

  @Test
  public void testGraph() {

    // Create kb
    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();

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
    Literal query = new Literal("path", newConst("b"), newConst("f"));
    Set<Clause> proofs = solver.proofs(query);
    Set<Clause> answers = Sets.newHashSet(solver.solve(query));
    Map<Literal, Trie<Literal>> tries = solver.tries(query);

    // Verify subgoals
    Assert.assertEquals(14, solver.nbSubgoals());

    // Verify proofs
    Assert.assertEquals(2, proofs.size());
    Assert.assertEquals(1, answers.size());
    Assert.assertEquals(1, tries.size());

    Clause answer1 = buildClause("path(b, f)", Lists.newArrayList("0.2::edge(b, d)", "0.5::edge(d, f)"));
    Clause answer2 = buildClause("path(b, f)", Lists.newArrayList("0.8::edge(b, e)", "0.3::edge(e, f)"));

    Assert.assertTrue(checkAnswers(answers, Sets.newHashSet(answer1, answer2)));
    Assert.assertTrue(checkProofs(tries, Sets.newHashSet(answer1, answer2)));

    // Verify BDD answer
    // 0.316::path(b, f).
    ProbabilityEstimator estimator = new ProbabilityEstimator(proofs);
    BigDecimal probability = estimator.probability(query, 3);

    Assert.assertTrue(BigDecimal.valueOf(0.316).compareTo(probability) == 0);
  }

  @Test
  public void testExtractClausesInProofs() {

    // Create kb
    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();

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
    Literal query = new Literal("path", newConst("b"), newConst("f"));
    List<String> table = solver.tableOfProofs(query);

    Assert.assertEquals(8, table.size());
    Assert.assertEquals("[fact] depth=0, 0.3::edge(\"e\", \"f\")\n" + "[fact] depth=0, 0.5::edge(\"d\", \"f\")\n"
        + "[fact] depth=1, 0.2::edge(\"b\", \"d\")\n" + "[fact] depth=1, 0.8::edge(\"b\", \"e\")\n"
        + "[rule] depth=0, path(\"b\", \"f\") :- path(\"b\", \"d\"), 0.5::edge(\"d\", \"f\")\n"
        + "[rule] depth=0, path(\"b\", \"f\") :- path(\"b\", \"e\"), 0.3::edge(\"e\", \"f\")\n"
        + "[rule] depth=1, path(\"b\", \"d\") :- 0.2::edge(\"b\", \"d\")\n"
        + "[rule] depth=1, path(\"b\", \"e\") :- 0.8::edge(\"b\", \"e\")", Joiner.on("\n").join(table));
  }
}
