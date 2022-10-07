package com.computablefacts.decima.problog;

import static com.computablefacts.decima.problog.AbstractTerm.newConst;
import static com.computablefacts.decima.problog.AbstractTerm.newVar;
import static com.computablefacts.decima.problog.Parser.parseClause;
import static com.computablefacts.decima.problog.TestUtils.buildClause;
import static com.computablefacts.decima.problog.TestUtils.checkAnswers;
import static com.computablefacts.decima.problog.TestUtils.checkProofs;

import com.computablefacts.asterix.trie.Trie;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

public class ProbabilityEstimatorTest {

  @Test
  public void testComputeProbabilityWithoutProofs() {
    ProbabilityEstimator estimator = new ProbabilityEstimator(new HashSet<>());
    Assert.assertEquals(BigDecimal.ZERO, estimator.probability(new Literal("fake", newConst(1))));
  }

  /**
   * See https://github.com/ML-KULeuven/problog/blob/master/test/swap.pl
   */
  @Test
  public void testLiteralsSwapping1() {

    // Create kb
    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();

    // Init kb with facts
    kb.azzert(parseClause("0.5::f(1,2)."));
    kb.azzert(parseClause("0.5::f(2,1)."));
    kb.azzert(parseClause("0.5::f(1,3)."));
    kb.azzert(parseClause("0.5::f(2,3)."));
    kb.azzert(parseClause("0.5::b(1)."));
    kb.azzert(parseClause("0.5::b(2)."));
    kb.azzert(parseClause("0.5::b(3)."));

    // Init kb with rules
    kb.azzert(parseClause("s1(X) :- b(X)."));
    kb.azzert(parseClause("s1(X) :- f(X,Y),s1(Y)."));
    kb.azzert(parseClause("s2(X) :- f(X,Y),s2(Y)."));
    kb.azzert(parseClause("s2(X) :- b(X)."));

    // Query kb
    // s1(1)?
    Solver solver = new Solver(kb, true);
    Literal query = new Literal("s1", newConst(1));
    List<Clause> proofs = Lists.newArrayList(solver.proofs(query));

    // Verify BDD answer
    // 0.734375::s1(1).
    ProbabilityEstimator estimator = new ProbabilityEstimator(Sets.newHashSet(proofs));
    BigDecimal probability = estimator.probability(query, 6);

    Assert.assertEquals(0, BigDecimal.valueOf(0.734375).compareTo(probability));
  }

  /**
   * See https://github.com/ML-KULeuven/problog/blob/master/test/swap.pl
   */
  @Test
  public void testLiteralsSwapping2() {

    // Create kb
    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();

    // Init kb with facts
    kb.azzert(parseClause("0.5::f(1,2)."));
    kb.azzert(parseClause("0.5::f(2,1)."));
    kb.azzert(parseClause("0.5::f(1,3)."));
    kb.azzert(parseClause("0.5::f(2,3)."));
    kb.azzert(parseClause("0.5::b(1)."));
    kb.azzert(parseClause("0.5::b(2)."));
    kb.azzert(parseClause("0.5::b(3)."));

    // Init kb with rules
    kb.azzert(parseClause("s1(X) :- b(X)."));
    kb.azzert(parseClause("s1(X) :- f(X,Y),s1(Y)."));
    kb.azzert(parseClause("s2(X) :- f(X,Y),s2(Y)."));
    kb.azzert(parseClause("s2(X) :- b(X)."));

    // Query kb
    // s2(1)?
    Solver solver = new Solver(kb, true);
    Literal query = new Literal("s2", newConst(1));
    List<Clause> proofs = Lists.newArrayList(solver.proofs(query));

    // Verify BDD answer
    // 0.734375::s2(1).
    ProbabilityEstimator estimator = new ProbabilityEstimator(Sets.newHashSet(proofs));
    BigDecimal probability = estimator.probability(query, 6);

    Assert.assertEquals(0, BigDecimal.valueOf(0.734375).compareTo(probability));
  }

  /**
   * Non-ground query
   * <p>
   * See https://github.com/ML-KULeuven/problog/blob/master/test/non_ground_query.pl
   */
  @Test
  public void testNonGroundQuery() {

    // Create kb
    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();

    // Init kb with facts
    kb.azzert(parseClause("0.1::b(1)."));
    kb.azzert(parseClause("0.2::b(2)."));
    kb.azzert(parseClause("0.3::e(1)."));
    kb.azzert(parseClause("0.4::e(3)."));
    kb.azzert(parseClause("d(1)."));
    kb.azzert(parseClause("d(2)."));
    kb.azzert(parseClause("d(3)."));

    // Init kb with rules
    kb.azzert(parseClause("a(X) :- b(2), c(X,Y)."));
    kb.azzert(parseClause("c(X,Y) :- c(X,Z), c(Z,Y)."));
    kb.azzert(parseClause("c(X,Y) :- d(X), d(Y)."));

    // Query kb
    // a(X)?
    Solver solver = new Solver(kb, true);
    Literal query = new Literal("a", newVar());
    Set<Clause> proofs = solver.proofs(query);

    // Verify BDD answer
    // 0.2::a(1).
    // 0.2::a(2).
    // 0.2::a(3).
    ProbabilityEstimator estimator = new ProbabilityEstimator(proofs);
    Map<Clause, BigDecimal> probabilities = estimator.probabilities();

    Clause a1 = new Clause(new Literal("a", newConst(1)));
    Clause a2 = new Clause(new Literal("a", newConst(2)));
    Clause a3 = new Clause(new Literal("a", newConst(3)));

    Assert.assertEquals(0, BigDecimal.valueOf(0.2).compareTo(probabilities.get(a1)));
    Assert.assertEquals(0, BigDecimal.valueOf(0.2).compareTo(probabilities.get(a2)));
    Assert.assertEquals(0, BigDecimal.valueOf(0.2).compareTo(probabilities.get(a3)));
  }

  /**
   * Ground, non-ground query
   * <p>
   * See https://github.com/ML-KULeuven/problog/blob/master/test/ground_nonground_bug_v4.pl
   */
  @Test
  public void testGroundNonGroundQuery4() {

    // Create kb
    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();

    // Init kb with facts
    kb.azzert(parseClause("0.1::p(1)."));

    // Init kb with rules
    kb.azzert(parseClause("p(X) :- p(1), fn_is(X, unk)."));
    kb.azzert(parseClause("fill(X) :- fn_is(X, unk)."));
    kb.azzert(parseClause("fill(X) :- p(X), fill(X)."));
    kb.azzert(parseClause("q(X) :- fill(X)."));

    // Query kb
    // q(X)?
    Solver solver = new Solver(kb, true);
    Literal query = new Literal("q", newVar());
    Set<Clause> proofs = solver.proofs(query);

    // Verify BDD answer
    // 0.1::q(unk).
    ProbabilityEstimator estimator = new ProbabilityEstimator(proofs);
    Map<Clause, BigDecimal> probabilities = estimator.probabilities();

    Clause answer = new Clause(new Literal("q", newConst("unk")));

    Assert.assertEquals(0, BigDecimal.valueOf(0.1).compareTo(probabilities.get(answer)));
  }

  /**
   * Negative query
   * <p>
   * See https://github.com/ML-KULeuven/problog/blob/master/test/negative_query.pl
   */
  @Test
  public void testNegativeQuery() {

    // Create kb
    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();

    // Init kb with facts
    kb.azzert(parseClause("0.3::p(1)."));

    // Query kb
    Solver solver = new Solver(kb, true);
    Literal query = new Literal("~p", newConst(1));
    Set<Clause> proofs = solver.proofs(query);
    Set<Clause> answers = Sets.newHashSet(solver.solve(query));
    Map<Literal, Trie<Literal>> tries = solver.tries(query);

    // Verify answers
    Assert.assertEquals(1, proofs.size());
    Assert.assertEquals(1, answers.size());
    Assert.assertEquals(1, tries.size());

    Clause answer = parseClause("0.7::~p(1).");

    Assert.assertTrue(checkAnswers(answers, Sets.newHashSet(answer)));
    Assert.assertTrue(checkProofs(tries, Sets.newHashSet(answer)));

    // Verify BDD answer
    // 0.7::~p(1).
    ProbabilityEstimator estimator = new ProbabilityEstimator(Sets.newHashSet(proofs));
    BigDecimal probability = estimator.probability(query);

    Assert.assertEquals(0, BigDecimal.valueOf(0.7).compareTo(probability));
  }

  /**
   * Tossing coins
   * <p>
   * Description: two coins - one biased and one not.
   * <p>
   * Query: what is the probability of throwing some heads.
   * <p>
   * See https://github.com/ML-KULeuven/problog/blob/master/test/00_trivial_or.pl
   */
  @Test
  public void testTrivialOr() {

    // Create kb
    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();

    // Init kb with facts
    kb.azzert(parseClause("0.5::heads1(a)."));
    kb.azzert(parseClause("0.6::heads2(a)."));

    // Init kb with rules
    kb.azzert(parseClause("someHeads(X) :- heads1(X)."));
    kb.azzert(parseClause("someHeads(X) :- heads2(X)."));

    // Query kb
    // someHeads(X)?
    Solver solver = new Solver(kb, true);
    Literal query = new Literal("someHeads", newVar());
    Set<Clause> proofs = solver.proofs(query);
    Set<Clause> answers = Sets.newHashSet(solver.solve(query));
    Map<Literal, Trie<Literal>> tries = solver.tries(query);

    // Verify answers
    Assert.assertEquals(2, proofs.size());
    Assert.assertEquals(1, answers.size());
    Assert.assertEquals(1, tries.size());

    Clause answer1 = buildClause("someHeads(a)", Lists.newArrayList("0.5::heads1(a)"));
    Clause answer2 = buildClause("someHeads(a)", Lists.newArrayList("0.6::heads2(a)"));

    Assert.assertTrue(checkAnswers(answers, Sets.newHashSet(answer1, answer2)));
    Assert.assertTrue(checkProofs(tries, Sets.newHashSet(answer1, answer2)));

    // Verify BDD answer
    // 0.8::someHeads(a).
    ProbabilityEstimator estimator = new ProbabilityEstimator(Sets.newHashSet(proofs));
    BigDecimal probability = estimator.probability(new Literal("someHeads", newConst("a")));

    Assert.assertEquals(0, BigDecimal.valueOf(0.8).compareTo(probability));
  }

  /**
   * Tossing coins
   * <p>
   * Description: two coins - one biased and one not.
   * <p>
   * Query: what is the probability of throwing two heads.
   * <p>
   * See https://github.com/ML-KULeuven/problog/blob/master/test/00_trivial_and.pl
   */
  @Test
  public void testTrivialAnd() {

    // Create kb
    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();

    // Init kb with facts
    kb.azzert(parseClause("0.5::heads1(a)."));
    kb.azzert(parseClause("0.6::heads2(a)."));

    // Init kb with rules
    kb.azzert(parseClause("twoHeads(X) :- heads1(X), heads2(X)."));

    // Query kb
    // twoHeads(X)?
    Solver solver = new Solver(kb, true);
    Literal query = new Literal("twoHeads", newVar());
    List<Clause> proofs = Lists.newArrayList(solver.proofs(query));
    Set<Clause> answers = Sets.newHashSet(solver.solve(query));
    Map<Literal, Trie<Literal>> tries = solver.tries(query);

    // Verify answers
    Assert.assertEquals(1, proofs.size());
    Assert.assertEquals(1, answers.size());
    Assert.assertEquals(1, tries.size());

    Clause answer = buildClause("twoHeads(a)", Lists.newArrayList("0.5::heads1(a)", "0.6::heads2(a)"));

    Assert.assertTrue(checkAnswers(answers, Sets.newHashSet(answer)));
    Assert.assertTrue(checkProofs(tries, Sets.newHashSet(answer)));

    // Verify BDD answer
    // 0.3::twoHeads(a).
    ProbabilityEstimator estimator = new ProbabilityEstimator(Sets.newHashSet(proofs));
    BigDecimal probability = estimator.probability(new Literal("twoHeads", newConst("a")));

    Assert.assertEquals(0, BigDecimal.valueOf(0.3).compareTo(probability));
  }

  /**
   * Duplicate fact
   * <p>
   * Description: Interpret as two separate facts.
   * <p>
   * See https://github.com/ML-KULeuven/problog/blob/master/test/00_trivial_duplicate.pl
   */
  @Test
  public void testTrivialDuplicate() {

    // Create kb
    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();

    // Init kb with facts
    kb.azzert(parseClause("0.3::p(1)."));
    kb.azzert(parseClause("0.2::p(2)."));
    kb.azzert(parseClause("0.6::p(1)."));

    // Query kb
    // p(1)?
    // p(2)?
    Solver solver = new Solver(kb, true);
    Literal query1 = new Literal("p", newConst("1"));
    Set<Clause> proofs1 = solver.proofs(query1);
    Set<Clause> answers1 = Sets.newHashSet(solver.solve(query1));
    Map<Literal, Trie<Literal>> tries1 = solver.tries(query1);

    Literal query2 = new Literal("p", newConst("2"));
    Set<Clause> proofs2 = solver.proofs(query2);
    Set<Clause> answers2 = Sets.newHashSet(solver.solve(query2));
    Map<Literal, Trie<Literal>> tries2 = solver.tries(query2);

    // Verify answers
    Assert.assertEquals(2, proofs1.size());
    Assert.assertEquals(2, tries1.size());
    Assert.assertEquals(2, answers1.size());
    Assert.assertEquals(1, proofs2.size());
    Assert.assertEquals(1, tries2.size());
    Assert.assertEquals(1, answers2.size());

    Clause answer1 = buildClause("0.3::p(1)", Lists.newArrayList());
    Clause answer2 = buildClause("0.6::p(1)", Lists.newArrayList());

    Assert.assertTrue(checkAnswers(answers1, Sets.newHashSet(answer1, answer2)));
    Assert.assertTrue(checkProofs(tries1, Sets.newHashSet(answer1, answer2)));

    Clause answer3 = buildClause("0.2::p(2)", Lists.newArrayList());

    Assert.assertTrue(checkAnswers(answers2, Sets.newHashSet(answer3)));
    Assert.assertTrue(checkProofs(tries2, Sets.newHashSet(answer3)));

    // Verify BDD answer
    // 0.72::p(1).
    // 0.2::p(2).
    ProbabilityEstimator estimator1 = new ProbabilityEstimator(Sets.newHashSet(proofs1));
    BigDecimal probability1 = estimator1.probability(query1);

    Assert.assertEquals(0, BigDecimal.valueOf(0.72).compareTo(probability1));

    ProbabilityEstimator estimator2 = new ProbabilityEstimator(Sets.newHashSet(proofs2));
    BigDecimal probability2 = estimator2.probability(query2);

    Assert.assertEquals(0, BigDecimal.valueOf(0.2).compareTo(probability2));
  }

  /**
   * Probabilistic negation
   * <p>
   * Description: Compute probability of a negated fact.
   * <p>
   * See https://github.com/ML-KULeuven/problog/blob/master/test/00_trivial_not.pl
   */
  @Test
  public void testTrivialNot() {

    // Create kb
    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();

    // Init kb with facts
    kb.azzert(parseClause("0.4::p(1)."));

    // Query kb
    // ~p(1)?
    Solver solver = new Solver(kb, true);
    Literal query = new Literal("~p", newConst("1"));
    Set<Clause> proofs = solver.proofs(query);
    Set<Clause> answers = Sets.newHashSet(solver.solve(query));
    Map<Literal, Trie<Literal>> tries = solver.tries(query);

    // Verify answers
    Assert.assertEquals(1, proofs.size());
    Assert.assertEquals(1, answers.size());
    Assert.assertEquals(1, tries.size());

    Clause answer = buildClause("0.6::~p(1)", Lists.newArrayList());

    Assert.assertTrue(checkAnswers(answers, Sets.newHashSet(answer)));
    Assert.assertTrue(checkProofs(tries, Sets.newHashSet(answer)));

    // Verify BDD answer
    // 0.6::~p(1).
    ProbabilityEstimator estimator = new ProbabilityEstimator(Sets.newHashSet(proofs));
    BigDecimal probability = estimator.probability(query);

    Assert.assertEquals(0, BigDecimal.valueOf(0.6).compareTo(probability));
  }

  /**
   * Probabilistic negation of a rule
   * <p>
   * Description: Compute probability of a negated rule.
   * <p>
   * See https://github.com/ML-KULeuven/problog/blob/master/test/00_trivial_not_and.pl
   */
  @Test
  public void testTrivialNotAnd() {

    // Create kb
    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();

    // Init kb with facts
    kb.azzert(parseClause("0.5::t(1)."));
    kb.azzert(parseClause("0.3::t(2)."));

    // Init kb with rules
    kb.azzert(parseClause("q(X) :- t(X), fn_add(U, X, 1), fn_int(V, U), t(V)."));
    kb.azzert(parseClause("p(X) :- ~q(X)."));

    // Query kb
    // p(1)?
    Solver solver = new Solver(kb, true);
    Literal query = new Literal("p", newConst(1));
    Set<Clause> proofs = solver.proofs(query);
    Set<Clause> answers = Sets.newHashSet(solver.solve(query));
    Map<Literal, Trie<Literal>> tries = solver.tries(query);

    // Verify answers
    Assert.assertEquals(2, proofs.size());
    Assert.assertEquals(1, tries.size());
    Assert.assertEquals(1, answers.size());

    Clause answer1 = buildClause("p(1)", Lists.newArrayList("0.5::~t(1)"));
    Clause answer2 = buildClause("p(1)", Lists.newArrayList("0.7::~t(2)"));

    Assert.assertTrue(checkAnswers(answers, Sets.newHashSet(answer1, answer2)));
    Assert.assertTrue(checkProofs(tries, Sets.newHashSet(answer1, answer2)));

    // Verify BDD answer
    // 0.85::p(1).
    ProbabilityEstimator estimator = new ProbabilityEstimator(Sets.newHashSet(proofs));
    BigDecimal probability = estimator.probability(query);

    Assert.assertEquals(0, BigDecimal.valueOf(0.85).compareTo(probability));
  }

  /**
   * See https://github.com/ML-KULeuven/problog/blob/master/test/tc_3.pl
   */
  @Test
  public void testRuleWithProbabilityInHead() {

    // Create kb
    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();

    // Init kb with facts
    kb.azzert(parseClause("athlet(1)."));
    kb.azzert(parseClause("athlet(2)."));
    kb.azzert(parseClause("student(2)."));
    kb.azzert(parseClause("student(3)."));

    // Init kb with rules
    kb.azzert(parseClause("0.5::stressed(X) :- student(X)."));
    kb.azzert(parseClause("0.2::stressed(X) :- athlet(X)."));

    // Query kb
    // stressed(1)?
    // stressed(2)?
    // stressed(3)?
    Solver solver = new Solver(kb, true);

    Literal query1 = new Literal("stressed", newConst(1));
    Set<Clause> proofs1 = solver.proofs(query1);
    Set<Clause> answers1 = Sets.newHashSet(solver.solve(query1));
    Map<Literal, Trie<Literal>> tries1 = solver.tries(query1);

    Literal query2 = new Literal("stressed", newConst(2));
    Set<Clause> proofs2 = solver.proofs(query2);
    Set<Clause> answers2 = Sets.newHashSet(solver.solve(query2));
    Map<Literal, Trie<Literal>> tries2 = solver.tries(query2);

    Literal query3 = new Literal("stressed", newConst(3));
    Set<Clause> proofs3 = solver.proofs(query3);
    Set<Clause> answers3 = Sets.newHashSet(solver.solve(query3));
    Map<Literal, Trie<Literal>> tries3 = solver.tries(query3);

    // Verify answers
    // stressed("1") :- athlet("1"), 0.2::proba_0sr9pjn("true")
    // stressed("2") :- student("2"), 0.5::proba_8jyexcv("true")
    // stressed("2") :- athlet("2"), 0.2::proba_0sr9pjn("true")
    // stressed("3") :- student("3"), 0.5::proba_8jyexcv("true")
    Assert.assertEquals(1, proofs1.size());
    Assert.assertEquals(1, answers1.size());
    Assert.assertEquals(1, tries1.size());
    Assert.assertEquals(2, proofs2.size());
    Assert.assertEquals(1, answers2.size());
    Assert.assertEquals(1, tries2.size());
    Assert.assertEquals(1, proofs3.size());
    Assert.assertEquals(1, answers3.size());
    Assert.assertEquals(1, tries3.size());

    Clause answer1 = buildClause("stressed(1)", Lists.newArrayList("athlet(1)"));

    Assert.assertTrue(checkAnswers(answers1, Sets.newHashSet(answer1)));
    Assert.assertTrue(checkProofs(tries1, Sets.newHashSet(answer1)));

    Clause answer2 = buildClause("stressed(2)", Lists.newArrayList("student(2)"));
    Clause answer3 = buildClause("stressed(2)", Lists.newArrayList("athlet(2)"));

    Assert.assertTrue(checkAnswers(answers2, Sets.newHashSet(answer2, answer3)));
    Assert.assertTrue(checkProofs(tries2, Sets.newHashSet(answer2, answer3)));

    Clause answer4 = buildClause("stressed(3)", Lists.newArrayList("student(3)"));

    Assert.assertTrue(checkAnswers(answers3, Sets.newHashSet(answer4)));
    Assert.assertTrue(checkProofs(tries3, Sets.newHashSet(answer4)));

    // Verify BDD answer
    // 0.2::stressed(1).
    // 0.6::stressed(2).
    // 0.5::stressed(3).
    ProbabilityEstimator estimator1 = new ProbabilityEstimator(Sets.newHashSet(proofs1));
    BigDecimal probability1 = estimator1.probability(query1);

    Assert.assertEquals(0, BigDecimal.valueOf(0.2).compareTo(probability1));

    ProbabilityEstimator estimator2 = new ProbabilityEstimator(Sets.newHashSet(proofs2));
    BigDecimal probability2 = estimator2.probability(query2);

    Assert.assertEquals(0, BigDecimal.valueOf(0.6).compareTo(probability2));

    ProbabilityEstimator estimator3 = new ProbabilityEstimator(Sets.newHashSet(proofs3));
    BigDecimal probability3 = estimator3.probability(query3);

    Assert.assertEquals(0, BigDecimal.valueOf(0.5).compareTo(probability3));
  }
}
