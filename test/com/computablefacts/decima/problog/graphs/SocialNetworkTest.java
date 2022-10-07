package com.computablefacts.decima.problog.graphs;

import static com.computablefacts.decima.problog.AbstractTerm.newConst;
import static com.computablefacts.decima.problog.Parser.parseClause;
import static com.computablefacts.decima.problog.TestUtils.buildClause;
import static com.computablefacts.decima.problog.TestUtils.checkAnswers;
import static com.computablefacts.decima.problog.TestUtils.checkProofs;

import com.computablefacts.asterix.WildcardMatcher;
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
 * See http://csci431.artifice.cc/notes/problog.html
 */
public class SocialNetworkTest {

  @Test
  public void test1() {

    // Query kb
    // smokes(angelika)?
    Solver solver = new Solver(kb(), true);
    Literal query = new Literal("smokes", newConst("angelika"));
    Set<Clause> proofs = solver.proofs(query);
    Set<Clause> answers = Sets.newHashSet(solver.solve(query));
    Map<Literal, Trie<Literal>> tries = solver.tries(query);

    // Verify subgoals
    Assert.assertEquals(11, solver.nbSubgoals());

    // Verify answers
    Assert.assertEquals(2, proofs.size());
    Assert.assertEquals(1, answers.size());
    Assert.assertEquals(1, tries.size());

    Clause answer1 = buildClause("smokes(angelika)",
        Lists.newArrayList("friend(angelika, jonas)", "person(jonas)", "person(angelika)", "person(jonas)"));
    Clause answer2 = buildClause("smokes(angelika)", Lists.newArrayList("person(angelika)"));

    Assert.assertTrue(checkAnswers(answers, Sets.newHashSet(answer1, answer2)));
    Assert.assertTrue(checkProofs(tries, Sets.newHashSet(answer1, answer2)));

    // Verify BDD answer
    // 0.342::smokes(angelika).
    BigDecimal probability = new ProbabilityEstimator(proofs).probability(query, 3);

    Assert.assertEquals(0, BigDecimal.valueOf(0.342).compareTo(probability));
  }

  @Test
  public void testExtractClausesInProofs1() {

    // Query kb
    // smokes(angelika)?
    Solver solver = new Solver(kb(), true);
    Literal query = new Literal("smokes", newConst("angelika"));
    List<String> table = solver.tableOfProofs(query);

    Assert.assertEquals(13, table.size());
    Assert.assertTrue(WildcardMatcher.match(Joiner.on("\n").join(table),
        "[fact] depth=0, friend(\"angelika\", \"jonas\")\n" + "[fact] depth=1, 0.2::proba_???????(\"true\")\n"
            + "[fact] depth=1, 0.3::proba_???????(\"true\")\n" + "[fact] depth=1, person(\"angelika\")\n"
            + "[fact] depth=1, person(\"jonas\")\n" + "[fact] depth=2, 0.3::proba_???????(\"true\")\n"
            + "[fact] depth=2, person(\"jonas\")\n"
            + "[rule] depth=0, smokes(\"angelika\") :- friend(\"angelika\", \"jonas\"), influences(\"jonas\", \"angelika\"), smokes(\"jonas\")\n"
            + "[rule] depth=0, smokes(\"angelika\") :- stress(\"angelika\")\n"
            + "[rule] depth=1, influences(\"jonas\", \"angelika\") :- person(\"jonas\"), person(\"angelika\"), 0.2::proba_???????(\"true\")\n"
            + "[rule] depth=1, smokes(\"jonas\") :- stress(\"jonas\")\n"
            + "[rule] depth=1, stress(\"angelika\") :- person(\"angelika\"), 0.3::proba_???????(\"true\")\n"
            + "[rule] depth=2, stress(\"jonas\") :- person(\"jonas\"), 0.3::proba_???????(\"true\")"));
  }

  @Test
  public void test2() {

    // Query kb
    // smokes(joris)?
    Solver solver = new Solver(kb(), true);
    Literal query = new Literal("smokes", newConst("joris"));
    Set<Clause> proofs = solver.proofs(query);
    Set<Clause> answers = Sets.newHashSet(solver.solve(query));
    Map<Literal, Trie<Literal>> tries = solver.tries(query);

    // Verify subgoals
    Assert.assertEquals(22, solver.nbSubgoals());

    // Verify answers
    Assert.assertEquals(5, proofs.size());
    Assert.assertEquals(1, answers.size());
    Assert.assertEquals(1, tries.size());

    Clause answer1 = buildClause("smokes(joris)",
        Lists.newArrayList("friend(joris, dimitar)", "person(dimitar)", "person(joris)", "person(dimitar)"));
    Clause answer2 = buildClause("smokes(joris)", Lists.newArrayList("person(joris)"));
    Clause answer3 = buildClause("smokes(joris)",
        Lists.newArrayList("friend(joris, angelika)", "person(angelika)", "person(joris)", "person(angelika)"));
    Clause answer4 = buildClause("smokes(joris)",
        Lists.newArrayList("friend(joris, jonas)", "person(jonas)", "person(joris)", "person(jonas)"));
    Clause answer5 = buildClause("smokes(joris)",
        Lists.newArrayList("friend(joris, angelika)", "person(angelika)", "person(joris)", "friend(angelika, jonas)",
            "person(jonas)", "person(angelika)", "person(jonas)"));

    Assert.assertTrue(checkAnswers(answers, Sets.newHashSet(answer1, answer2, answer3, answer4, answer5)));
    Assert.assertTrue(checkProofs(tries, Sets.newHashSet(answer1, answer2, answer3, answer4, answer5)));

    // Verify BDD answer
    // 0.42301296::smokes(joris).
    ProbabilityEstimator estimator = new ProbabilityEstimator(proofs);
    BigDecimal probability = estimator.probability(query, 8);

    Assert.assertEquals(0, BigDecimal.valueOf(0.42556811).compareTo(probability));
  }

  @Test
  public void testExtractClausesInProofs2() {

    // Query kb
    // smokes(joris)?
    Solver solver = new Solver(kb(), true);
    Literal query = new Literal("smokes", newConst("joris"));
    List<String> table = solver.tableOfProofs(query);

    Assert.assertEquals(33, table.size());
    Assert.assertTrue(WildcardMatcher.match(Joiner.on("\n").join(table),
        "[fact] depth=0, friend(\"joris\", \"angelika\")\n" + "[fact] depth=0, friend(\"joris\", \"dimitar\")\n"
            + "[fact] depth=0, friend(\"joris\", \"jonas\")\n" + "[fact] depth=1, 0.2::proba_???????(\"true\")\n"
            + "[fact] depth=1, 0.3::proba_???????(\"true\")\n" + "[fact] depth=1, friend(\"angelika\", \"jonas\")\n"
            + "[fact] depth=1, person(\"angelika\")\n" + "[fact] depth=1, person(\"dimitar\")\n"
            + "[fact] depth=1, person(\"jonas\")\n" + "[fact] depth=1, person(\"joris\")\n"
            + "[fact] depth=2, 0.2::proba_???????(\"true\")\n" + "[fact] depth=2, 0.3::proba_???????(\"true\")\n"
            + "[fact] depth=2, person(\"angelika\")\n" + "[fact] depth=2, person(\"dimitar\")\n"
            + "[fact] depth=2, person(\"jonas\")\n" + "[fact] depth=3, 0.3::proba_???????(\"true\")\n"
            + "[fact] depth=3, person(\"jonas\")\n"
            + "[rule] depth=0, smokes(\"joris\") :- friend(\"joris\", \"angelika\"), influences(\"angelika\", \"joris\"), smokes(\"angelika\")\n"
            + "[rule] depth=0, smokes(\"joris\") :- friend(\"joris\", \"dimitar\"), influences(\"dimitar\", \"joris\"), smokes(\"dimitar\")\n"
            + "[rule] depth=0, smokes(\"joris\") :- friend(\"joris\", \"jonas\"), influences(\"jonas\", \"joris\"), smokes(\"jonas\")\n"
            + "[rule] depth=0, smokes(\"joris\") :- stress(\"joris\")\n"
            + "[rule] depth=1, influences(\"angelika\", \"joris\") :- person(\"angelika\"), person(\"joris\"), 0.2::proba_???????(\"true\")\n"
            + "[rule] depth=1, influences(\"dimitar\", \"joris\") :- person(\"dimitar\"), person(\"joris\"), 0.2::proba_???????(\"true\")\n"
            + "[rule] depth=1, influences(\"jonas\", \"joris\") :- person(\"jonas\"), person(\"joris\"), 0.2::proba_???????(\"true\")\n"
            + "[rule] depth=1, smokes(\"angelika\") :- friend(\"angelika\", \"jonas\"), influences(\"jonas\", \"angelika\"), smokes(\"jonas\")\n"
            + "[rule] depth=1, smokes(\"angelika\") :- stress(\"angelika\")\n"
            + "[rule] depth=1, smokes(\"dimitar\") :- stress(\"dimitar\")\n"
            + "[rule] depth=1, stress(\"joris\") :- person(\"joris\"), 0.3::proba_???????(\"true\")\n"
            + "[rule] depth=2, influences(\"jonas\", \"angelika\") :- person(\"jonas\"), person(\"angelika\"), 0.2::proba_???????(\"true\")\n"
            + "[rule] depth=2, smokes(\"jonas\") :- stress(\"jonas\")\n"
            + "[rule] depth=2, stress(\"angelika\") :- person(\"angelika\"), 0.3::proba_???????(\"true\")\n"
            + "[rule] depth=2, stress(\"dimitar\") :- person(\"dimitar\"), 0.3::proba_???????(\"true\")\n"
            + "[rule] depth=3, stress(\"jonas\") :- person(\"jonas\"), 0.3::proba_???????(\"true\")"));
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
