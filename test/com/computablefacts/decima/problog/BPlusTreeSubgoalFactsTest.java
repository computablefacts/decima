package com.computablefacts.decima.problog;

import static com.computablefacts.decima.problog.AbstractTerm.newConst;
import static com.computablefacts.decima.problog.Parser.parseClause;

import com.computablefacts.asterix.RandomString;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;

public class BPlusTreeSubgoalFactsTest {

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
    AtomicInteger id = new AtomicInteger(0);
    String tblName = "solver_subgoals_" + new RandomString().nextString();
    Solver solver = new Solver(kb, literal -> new Subgoal(literal,
        new BPlusTreeSubgoalFacts(System.getProperty("java.io.tmpdir"), tblName, id.getAndIncrement()), true));
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
    Multiset<Literal> facts = HashMultiset.create();
    AtomicInteger id = new AtomicInteger(0);
    String tblName = "solver_subgoals_" + new RandomString().nextString();
    Solver solver = new Solver(kb, literal -> new Subgoal(literal,
        new BPlusTreeSubgoalFacts(System.getProperty("java.io.tmpdir"), tblName, id.getAndIncrement(), facts::add),
        true));
    Literal query = new Literal("s2", newConst(1));
    List<Clause> proofs = Lists.newArrayList(solver.proofs(query));

    // Verify subgoals' facts
    Assert.assertEquals(10, facts.size());

    Assert.assertTrue(facts.contains(new Literal(BigDecimal.valueOf(0.5), "b", newConst(1))));
    Assert.assertTrue(facts.contains(new Literal(BigDecimal.valueOf(0.5), "b", newConst(2))));
    Assert.assertTrue(facts.contains(new Literal(BigDecimal.valueOf(0.5), "b", newConst(3))));
    Assert.assertTrue(facts.contains(new Literal(BigDecimal.valueOf(0.5), "f", newConst(1), newConst(3))));
    Assert.assertTrue(facts.contains(new Literal(BigDecimal.valueOf(0.5), "f", newConst(1), newConst(2))));
    Assert.assertTrue(facts.contains(new Literal(BigDecimal.valueOf(0.5), "f", newConst(2), newConst(3))));
    Assert.assertTrue(facts.contains(new Literal(BigDecimal.valueOf(0.5), "f", newConst(2), newConst(1))));
    Assert.assertTrue(facts.contains(new Literal("s2", newConst(3))));
    Assert.assertTrue(facts.contains(new Literal("s2", newConst(2))));
    Assert.assertTrue(facts.contains(new Literal("s2", newConst(1))));

    // Verify BDD answer
    // 0.734375::s2(1).
    ProbabilityEstimator estimator = new ProbabilityEstimator(Sets.newHashSet(proofs));
    BigDecimal probability = estimator.probability(query, 6);

    Assert.assertEquals(0, BigDecimal.valueOf(0.734375).compareTo(probability));
  }
}
