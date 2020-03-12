package com.computablefacts.decima.problog;

import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Test;

public class ParserTest {

  @Test(expected = IllegalStateException.class)
  public void testMissingFinalDotAfterFact() {
    Clause clause = Parser.parseClause("edge(a, b)");
  }

  // @Test(expected = IllegalStateException.class)
  // public void testMissingFinalDotAfterRule() {
  // Clause clause = Parser.parseClause("edge(X, Y) :- node(X), node(Y)");
  // }

  @Test(expected = IllegalStateException.class)
  public void testFactWithVariable() {
    Clause clause = Parser.parseClause("edge(a, U).");
  }

  @Test
  public void testParseFact() {

    Clause fact0 = new Clause(new Literal("edge", new Const("a"), new Const(0), new Const(1.1)));
    Clause fact1 = Parser.parseClause("edge(\"a\", 0, 1.1).");
    Clause fact2 = Parser.parseClause("edge(a, \"0\", \"1.1\").");

    Assert.assertEquals(fact0, fact1);
    Assert.assertEquals(fact0, fact2);
  }

  @Test
  public void testParseNegatedFact() {

    Clause fact0 = new Clause(new Literal("~edge", new Const("a"), new Const("b")));
    Clause fact1 = Parser.parseClause("~edge(a, b).");
    Clause fact2 = Parser.parseClause("\\+ edge(a, b).");

    Assert.assertEquals(fact0, fact1);
    Assert.assertEquals(fact0, fact2);
  }

  @Test
  public void testParseRule() {

    Var x = new Var();
    Var y = new Var();

    Literal edgeXY = new Literal("edge", x, y);
    Literal nodeX = new Literal("node", x);
    Literal nodeY = new Literal("node", y);

    Clause rule0 = new Clause(edgeXY, nodeX, nodeY);
    Clause rule1 = Parser.parseClause("edge(X, Y) :- node(X), node(Y).");

    Assert.assertTrue(rule0.isRelevant(rule1));
  }

  @Test
  public void testParseRuleWithNegatedLiteralsInBody() {

    Var x = new Var();
    Var y = new Var();

    Literal edgeXY = new Literal("not_edge", x, y);
    Literal nodeX = new Literal("~node", x);
    Literal nodeY = new Literal("node", y);

    Clause rule0 = new Clause(edgeXY, nodeX, nodeY);
    Clause rule1 = Parser.parseClause("not_edge(X, Y) :- ~node(X), node(Y).");

    Assert.assertTrue(rule0.isRelevant(rule1));
  }

  @Test
  public void testParseEqBuiltin() {

    Clause rule1 = Parser.parseClause("edge(X, Y) :- node(X), node(Y), X=Y.");
    Clause rule2 =
        Parser.parseClause("edge(X, Y) :- node(X), node(Y), fn_eq(U, X, Y), fn_is_true(U).");

    Assert.assertTrue(rule1.isRelevant(rule2));
  }

  @Test
  public void testParseNotEqBuiltin() {

    Clause rule1 = Parser.parseClause("edge(X, Y) :- node(X), node(Y), X!=Y.");
    Clause rule2 = Parser.parseClause("edge(X, Y) :- node(X), node(Y), X<>Y.");
    Clause rule3 =
        Parser.parseClause("edge(X, Y) :- node(X), node(Y), fn_eq(U, X, Y), fn_is_false(U).");

    Assert.assertTrue(rule1.isRelevant(rule2));
    Assert.assertTrue(rule1.isRelevant(rule3));
    Assert.assertTrue(rule2.isRelevant(rule3));
  }

  @Test
  public void testParseLtBuiltin() {

    Clause rule1 = Parser.parseClause("edge(X, Y) :- node(X), node(Y), X<Y.");
    Clause rule2 =
        Parser.parseClause("edge(X, Y) :- node(X), node(Y), fn_lt(U, X, Y), fn_is_true(U).");

    Assert.assertTrue(rule1.isRelevant(rule2));
  }

  @Test
  public void testParseLteBuiltin() {

    Clause rule1 = Parser.parseClause("edge(X, Y) :- node(X), node(Y), X<=Y.");
    Clause rule2 =
        Parser.parseClause("edge(X, Y) :- node(X), node(Y), fn_lte(U, X, Y), fn_is_true(U).");

    Assert.assertTrue(rule1.isRelevant(rule2));
  }

  @Test
  public void testParseGtBuiltin() {

    Clause rule1 = Parser.parseClause("edge(X, Y) :- node(X), node(Y), X>Y.");
    Clause rule2 =
        Parser.parseClause("edge(X, Y) :- node(X), node(Y), fn_gt(U, X, Y), fn_is_true(U).");

    Assert.assertTrue(rule1.isRelevant(rule2));
  }

  @Test
  public void testParseGteBuiltin() {

    Clause rule1 = Parser.parseClause("edge(X, Y) :- node(X), node(Y), X>=Y.");
    Clause rule2 =
        Parser.parseClause("edge(X, Y) :- node(X), node(Y), fn_gte(U, X, Y), fn_is_true(U).");

    Assert.assertTrue(rule1.isRelevant(rule2));
  }

  @Test
  public void testParseProbabilityOnFact() {

    Clause fact = Parser.parseClause("0.3::edge(a, b).");

    Assert.assertTrue(fact.isFact());
    Assert.assertFalse(fact.isRule());

    Assert.assertEquals("edge", fact.head().predicate().name());
    Assert.assertEquals(2, fact.head().predicate().arity());

    Assert.assertEquals(BigDecimal.valueOf(0.3), fact.head().probability());
  }

  @Test
  public void testParseProbabilityOnRule() {

    Clause rule = Parser.parseClause("0.3::edge(X, Y) :- node(X), node(Y).");

    Assert.assertTrue(rule.isRule());
    Assert.assertFalse(rule.isFact());

    Assert.assertEquals("edge", rule.head().predicate().name());
    Assert.assertEquals(2, rule.head().predicate().arity());

    Assert.assertEquals(BigDecimal.valueOf(0.3), rule.head().probability());
  }

  @Test
  public void testParseFunction() {

    Clause rule = Parser
        .parseClause("under_and_above(X, Y) :- fn_if(O, fn_and(fn_lt(X, 0), fn_gt(Y, 0)), 1, 0).");

    Assert.assertTrue(rule.isRule());
    Assert.assertFalse(rule.isFact());
  }
}
