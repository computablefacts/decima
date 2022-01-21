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
 * See http://csci431.artifice.cc/notes/problog.html
 */
public class SocialNetworkTest {

  @Test
  public void test1() {

    // Query kb
    // smokes(angelika)?
    Solver solver = new Solver(kb(), true);
    Literal query = new Literal("smokes", new Const("angelika"));
    Set<Clause> proofs = solver.proofs(query);
    Set<Clause> answers = Sets.newHashSet(solver.solve(query));
    Map<Literal, Trie<Literal>> tries = solver.tries(query);

    // Verify subgoals
    Assert.assertEquals(11, solver.nbSubgoals());

    // Verify answers
    Assert.assertEquals(2, proofs.size());
    Assert.assertEquals(1, answers.size());
    Assert.assertEquals(1, tries.size());

    Clause answer1 = buildClause("smokes(angelika)", Lists.newArrayList("friend(angelika, jonas)",
        "person(jonas)", "person(angelika)", "person(jonas)"));
    Clause answer2 = buildClause("smokes(angelika)", Lists.newArrayList("person(angelika)"));

    Assert.assertTrue(checkAnswers(answers, Sets.newHashSet(answer1, answer2)));
    Assert.assertTrue(checkProofs(tries, Sets.newHashSet(answer1, answer2)));

    // Verify BDD answer
    // 0.342::smokes(angelika).
    BigDecimal probability = new ProbabilityEstimator(proofs).probability(query, 3);

    Assert.assertEquals(0, BigDecimal.valueOf(0.342).compareTo(probability));
  }

  @Test
  public void test2() {

    // Query kb
    // smokes(joris)?
    Solver solver = new Solver(kb(), true);
    Literal query = new Literal("smokes", new Const("joris"));
    Set<Clause> proofs = solver.proofs(query);
    Set<Clause> answers = Sets.newHashSet(solver.solve(query));
    Map<Literal, Trie<Literal>> tries = solver.tries(query);

    // Verify subgoals
    Assert.assertEquals(22, solver.nbSubgoals());

    // Verify answers
    Assert.assertEquals(5, proofs.size());
    Assert.assertEquals(1, answers.size());
    Assert.assertEquals(1, tries.size());

    Clause answer1 = buildClause("smokes(joris)", Lists.newArrayList("friend(joris, dimitar)",
        "person(dimitar)", "person(joris)", "person(dimitar)"));
    Clause answer2 = buildClause("smokes(joris)", Lists.newArrayList("person(joris)"));
    Clause answer3 = buildClause("smokes(joris)", Lists.newArrayList("friend(joris, angelika)",
        "person(angelika)", "person(joris)", "person(angelika)"));
    Clause answer4 = buildClause("smokes(joris)", Lists.newArrayList("friend(joris, jonas)",
        "person(jonas)", "person(joris)", "person(jonas)"));
    Clause answer5 = buildClause("smokes(joris)",
        Lists.newArrayList("friend(joris, angelika)", "person(angelika)", "person(joris)",
            "friend(angelika, jonas)", "person(jonas)", "person(angelika)", "person(jonas)"));

    Assert.assertTrue(
        checkAnswers(answers, Sets.newHashSet(answer1, answer2, answer3, answer4, answer5)));
    Assert.assertTrue(
        checkProofs(tries, Sets.newHashSet(answer1, answer2, answer3, answer4, answer5)));

    // Verify BDD answer
    // 0.42301296::smokes(joris).
    ProbabilityEstimator estimator = new ProbabilityEstimator(proofs);
    BigDecimal probability = estimator.probability(query, 8);

    Assert.assertEquals(0, BigDecimal.valueOf(0.42556811).compareTo(probability));
  }

  private InMemoryKnowledgeBase kb() {

    // Create kb
    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();

    // Init kb with facts
    kb.azzert(parseClause("person(angelika)."));
    kb.azzert(parseClause("person(joris)."));
    kb.azzert(parseClause("person(jonas)."));
    kb.azzert(parseClause("person(dimitar)."));
    kb.azzert(parseClause("friend(joris, jonas)."));
    kb.azzert(parseClause("friend(joris, angelika)."));
    kb.azzert(parseClause("friend(joris, dimitar)."));
    kb.azzert(parseClause("friend(angelika, jonas)."));

    // Init kb with rules
    kb.azzert(parseClause("0.3::stress(X) :- person(X)."));
    kb.azzert(parseClause("0.2::influences(X,Y) :- person(X), person(Y)."));
    kb.azzert(parseClause("smokes(X) :- stress(X)."));
    kb.azzert(parseClause("smokes(X) :- friend(X,Y), influences(Y,X), smokes(Y)."));
    kb.azzert(parseClause("0.4::asthma(X) :- smokes(X)."));

    return kb;
  }
}
