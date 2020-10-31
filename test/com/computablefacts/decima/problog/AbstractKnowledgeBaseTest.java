package com.computablefacts.decima.problog;

import static com.computablefacts.decima.problog.TestUtils.parseClause;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.computablefacts.decima.robdd.Pair;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

public class AbstractKnowledgeBaseTest {

  @Test(expected = NullPointerException.class)
  public void testAssertNullClause() {
    kb().azzert((Clause) null);
  }

  @Test(expected = NullPointerException.class)
  public void testAssertNullClauses() {
    kb().azzert((Set<Clause>) null);
  }

  @Test(expected = IllegalStateException.class)
  public void testAssertFactWithZeroProbability() {
    kb().azzert(parseClause("0.0:edge(a, b)."));
  }

  @Test(expected = IllegalStateException.class)
  public void testAssertRuleWithZeroProbability() {
    kb().azzert(parseClause("0.0:is_true(X) :- fn_is_true(X)."));
  }

  @Test(expected = IllegalStateException.class)
  public void testAssertRuleWithProbabilityInBody() {
    kb().azzert(parseClause("node(X) :- 0.3::edge(X, b)."));
  }

  @Test
  public void testAssertFact() {

    Clause fact1 = parseClause("0.3::edge(a, b).");
    Clause fact2 = parseClause("0.5::edge(b, c).");

    InMemoryKnowledgeBase kb = kb();
    kb.azzert(fact1);
    kb.azzert(fact2);

    Assert.assertEquals(Sets.newHashSet(), kb.rules());
    Assert.assertEquals(Sets.newHashSet(fact1, fact2), kb.facts());
  }

  @Test
  public void testAssertRule() {

    Clause rule1 = parseClause("0.2::path(A, B) :- edge(A, B).");
    Clause rule2 = parseClause("0.2::path(A, B) :- path(A, X), edge(X, B).");

    InMemoryKnowledgeBase kb = kb();
    kb.azzert(rule1);
    kb.azzert(rule2);

    Assert.assertEquals(2, kb.facts().size());
    Assert.assertEquals(2, kb.rules().size());

    Set<Clause> facts = kb.facts();
    Set<Clause> rules = kb.rules();

    // Check facts
    Clause firstFact = Iterables.get(facts, 0);
    Clause secondFact = Iterables.get(facts, 1);

    Assert.assertTrue(firstFact.head().predicate().name().startsWith("proba_"));
    Assert.assertTrue(secondFact.head().predicate().name().startsWith("proba_"));

    // Check rules
    Clause firstRule = Iterables.get(rules, 0);
    Clause secondRule = Iterables.get(rules, 1);

    Assert.assertEquals(BigDecimal.ONE, firstRule.head().probability());
    Assert.assertEquals(BigDecimal.ONE, secondRule.head().probability());

    Assert.assertTrue(facts.stream().map(Clause::head)
        .anyMatch(f -> f.isRelevant(firstRule.body().get(firstRule.body().size() - 1))));
    Assert.assertTrue(facts.stream().map(Clause::head)
        .anyMatch(f -> f.isRelevant(secondRule.body().get(secondRule.body().size() - 1))));
  }

  @Test
  public void testAssertClauses() {

    Clause fact1 = parseClause("0.3::edge(a, b).");
    Clause fact2 = parseClause("0.5::edge(b, c).");

    Clause rule1 = parseClause("0.2::path(A, B) :- edge(A, B).");
    Clause rule2 = parseClause("0.2::path(A, B) :- path(A, X), edge(X, B).");

    InMemoryKnowledgeBase kb = kb();
    kb.azzert(Sets.newHashSet(fact1, fact2, rule1, rule2));

    Assert.assertEquals(4, kb.facts().size());
    Assert.assertEquals(2, kb.rules().size());
  }

  @Test
  public void testIntersectionSet1SizeMoreThanSet2Size() {

    Set<String> set1 = Sets.newHashSet("a", "b", "c", "d", "e");
    Set<String> set2 = Sets.newHashSet("a", "b", "c");

    Set<String> intersection = kb().intersection(set1, set2);

    Assert.assertEquals(Sets.newHashSet("a", "b", "c"), intersection);
  }

  @Test
  public void testIntersectionSet1SizeLessThanSet2Size() {

    Set<String> set1 = Sets.newHashSet("a", "b", "c");
    Set<String> set2 = Sets.newHashSet("a", "b", "c", "d", "e");

    Set<String> intersection = kb().intersection(set1, set2);

    Assert.assertEquals(Sets.newHashSet("a", "b", "c"), intersection);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testApplyRewriteRuleHeadOnFact() {
    Clause fact = parseClause("0.3::edge(a, b).");
    Pair<Clause, Clause> pair = kb().rewriteRuleHead(fact);
  }

  @Test
  public void testApplyRewriteRuleHeadOnRuleWithProbability() {

    Clause rule = parseClause("0.3::path(A, B) :- path(A, X), edge(X, B).");

    Assert.assertEquals(BigDecimal.valueOf(0.3), rule.head().probability());
    Assert.assertEquals(2, rule.body().size());

    Pair<Clause, Clause> pair = kb().rewriteRuleHead(rule);
    Clause newRule = pair.t;
    Clause newFact = pair.u;

    Assert.assertTrue(newRule.isRule());
    Assert.assertTrue(newFact.isFact());

    Assert.assertEquals(BigDecimal.ONE, newRule.head().probability());
    Assert.assertEquals(3, newRule.body().size());

    Assert.assertEquals(BigDecimal.valueOf(0.3), newFact.head().probability());
    Assert.assertTrue(newFact.head().isRelevant(newRule.body().get(newRule.body().size() - 1)));
    Assert.assertTrue(newFact.head().predicate().name().startsWith("proba_"));
  }

  private InMemoryKnowledgeBase kb() {
    return new InMemoryKnowledgeBase();
  }
}
