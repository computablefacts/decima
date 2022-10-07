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
 * Extracted from Angelika Kimmig, Bart Demoen and Luc De Raedt (2010). "On the Implementation of the Probabilistic
 * Logic Programming Language ProbLog"
 */
public class GraphWithoutCycle2Test {

  @Test
  public void testGraph() {

    // Create kb
    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();

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
    Solver solver = new Solver(kb, true);
    Literal query1 = new Literal("path", newConst("a"), newConst("d"));
    Set<Clause> proofs1 = solver.proofs(query1);
    Set<Clause> answers1 = Sets.newHashSet(solver.solve(query1));
    Map<Literal, Trie<Literal>> tries1 = solver.tries(query1);

    // Verify subgoals
    Assert.assertEquals(12, solver.nbSubgoals());

    // Verify answers
    Assert.assertEquals(4, proofs1.size());
    Assert.assertEquals(1, answers1.size());
    Assert.assertEquals(1, tries1.size());

    Clause answer1 = buildClause("path(a, d)", Lists.newArrayList("0.8::edge(a, c)", "0.9::edge(c, d)"));
    Clause answer2 = buildClause("path(a, d)",
        Lists.newArrayList("0.7::edge(a, b)", "0.6::edge(b, c)", "0.9::edge(c, d)"));
    Clause answer3 = buildClause("path(a, d)",
        Lists.newArrayList("0.8::edge(a, c)", "0.8::edge(c, e)", "0.5::edge(e, d)"));
    Clause answer4 = buildClause("path(a, d)",
        Lists.newArrayList("0.7::edge(a, b)", "0.6::edge(b, c)", "0.8::edge(c, e)", "0.5::edge(e, d)"));

    Assert.assertTrue(checkAnswers(answers1, Sets.newHashSet(answer1, answer2, answer3, answer4)));
    Assert.assertTrue(checkProofs(tries1, Sets.newHashSet(answer1, answer2, answer3, answer4)));

    // Verify BDD answer
    // 0.83096::path(a, d).
    ProbabilityEstimator estimator1 = new ProbabilityEstimator(proofs1);
    BigDecimal probability1 = estimator1.probability(query1, 5);

    Assert.assertTrue(BigDecimal.valueOf(0.83096).compareTo(probability1) == 0);

    // Query kb
    // path(c, d)?
    Literal query2 = new Literal("path", newConst("c"), newConst("d"));
    Set<Clause> proofs2 = solver.proofs(query2);
    Set<Clause> answers2 = Sets.newHashSet(solver.solve(query2));
    Map<Literal, Trie<Literal>> tries2 = solver.tries(query2);

    // Verify proofs
    Assert.assertEquals(2, proofs2.size());
    Assert.assertEquals(1, answers2.size());
    Assert.assertEquals(1, tries2.size());

    Clause answer5 = buildClause("path(c, d)", Lists.newArrayList("0.8::edge(c, e)", "0.5::edge(e, d)"));
    Clause answer6 = buildClause("path(c, d)", Lists.newArrayList("0.9::edge(c, d)"));

    Assert.assertTrue(checkAnswers(answers2, Sets.newHashSet(answer5, answer6)));
    Assert.assertTrue(checkProofs(tries2, Sets.newHashSet(answer5, answer6)));

    // Verify BDD answer
    // 0.94::path(c, d).
    ProbabilityEstimator estimator2 = new ProbabilityEstimator(proofs2);
    BigDecimal probability2 = estimator2.probability(query2, 2);

    Assert.assertTrue(BigDecimal.valueOf(0.94).compareTo(probability2) == 0);
  }

  @Test
  public void testExtractClausesInProofs() {

    // Create kb
    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();

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
    Solver solver = new Solver(kb, true);
    Literal query1 = new Literal("path", newConst("a"), newConst("d"));
    List<String> table1 = solver.tableOfProofs(query1);

    Assert.assertEquals(12, table1.size());
    Assert.assertEquals("[fact] depth=0, 0.5::edge(\"e\", \"d\")\n" + "[fact] depth=0, 0.9::edge(\"c\", \"d\")\n"
        + "[fact] depth=1, 0.6::edge(\"b\", \"c\")\n" + "[fact] depth=1, 0.8::edge(\"a\", \"c\")\n"
        + "[fact] depth=1, 0.8::edge(\"c\", \"e\")\n" + "[fact] depth=2, 0.7::edge(\"a\", \"b\")\n"
        + "[rule] depth=0, path(\"a\", \"d\") :- path(\"a\", \"c\"), 0.9::edge(\"c\", \"d\")\n"
        + "[rule] depth=0, path(\"a\", \"d\") :- path(\"a\", \"e\"), 0.5::edge(\"e\", \"d\")\n"
        + "[rule] depth=1, path(\"a\", \"c\") :- 0.8::edge(\"a\", \"c\")\n"
        + "[rule] depth=1, path(\"a\", \"c\") :- path(\"a\", \"b\"), 0.6::edge(\"b\", \"c\")\n"
        + "[rule] depth=1, path(\"a\", \"e\") :- path(\"a\", \"c\"), 0.8::edge(\"c\", \"e\")\n"
        + "[rule] depth=2, path(\"a\", \"b\") :- 0.7::edge(\"a\", \"b\")", Joiner.on("\n").join(table1));

    // Query kb
    // path(c, d)?
    Literal query2 = new Literal("path", newConst("c"), newConst("d"));
    List<String> table2 = solver.tableOfProofs(query2);

    Assert.assertEquals(6, table2.size());
    Assert.assertEquals("[fact] depth=0, 0.5::edge(\"e\", \"d\")\n" + "[fact] depth=0, 0.9::edge(\"c\", \"d\")\n"
        + "[fact] depth=1, 0.8::edge(\"c\", \"e\")\n"
        + "[rule] depth=0, path(\"c\", \"d\") :- 0.9::edge(\"c\", \"d\")\n"
        + "[rule] depth=0, path(\"c\", \"d\") :- path(\"c\", \"e\"), 0.5::edge(\"e\", \"d\")\n"
        + "[rule] depth=1, path(\"c\", \"e\") :- 0.8::edge(\"c\", \"e\")", Joiner.on("\n").join(table2));
  }
}
