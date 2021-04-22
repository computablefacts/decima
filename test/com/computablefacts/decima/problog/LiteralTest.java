package com.computablefacts.decima.problog;

import static com.computablefacts.decima.problog.TestUtils.parseClause;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.computablefacts.nona.helpers.WildcardMatcher;

public class LiteralTest {

  @Test
  public void testLiteral() {

    Predicate predicate = new Predicate("edge", 2);
    Literal literal = new Literal("edge", new Const("a"), new Const("b"));

    Assert.assertTrue(literal.isGrounded());
    Assert.assertEquals(predicate, literal.predicate());

    Assert.assertEquals(2, literal.terms().size());
    Assert.assertTrue(literal.hasTerm(new Const("a")));
    Assert.assertTrue(literal.hasTerm(new Const("b")));
  }

  @Test
  public void testNegatedLiteral() {

    Predicate predicate = new Predicate("~edge", 2);
    Literal literal = new Literal("~edge", new Const("a"), new Const("b"));

    Assert.assertTrue(literal.isGrounded());
    Assert.assertEquals(predicate, literal.predicate());

    Assert.assertEquals(2, literal.terms().size());
    Assert.assertTrue(literal.hasTerm(new Const("a")));
    Assert.assertTrue(literal.hasTerm(new Const("b")));
  }

  @Test
  public void testIsSemiGroundedWithConstAndWildcard() {

    Literal literal = new Literal("edge", new Const("a"), new Var(true));

    Assert.assertFalse(literal.isGrounded());
    Assert.assertTrue(literal.isSemiGrounded());
  }

  @Test
  public void testIsSemiGroundedWithConstOnly() {

    Literal literal = new Literal("edge", new Const("a"), new Const("b"));

    Assert.assertTrue(literal.isGrounded());
    Assert.assertTrue(literal.isSemiGrounded());
  }

  @Test
  public void testIsSemiGroundedWithWildcardOnly() {

    Literal literal = new Literal("edge", new Var(true), new Var(true));

    Assert.assertFalse(literal.isGrounded());
    Assert.assertTrue(literal.isSemiGrounded());
  }

  @Test
  public void testIsNotSemiGrounded() {

    Literal literal = new Literal("edge", new Const("a"), new Var());

    Assert.assertFalse(literal.isGrounded());
    Assert.assertFalse(literal.isSemiGrounded());
  }

  @Test
  public void testBuiltinLiteral() {

    Predicate predicate = new Predicate("fn_isOk", 2);
    Literal literal = new Literal("fn_isOk", new Var(), new Var());

    Assert.assertFalse(literal.isGrounded());
    Assert.assertEquals(predicate, literal.predicate());

    Assert.assertEquals(2, literal.terms().size());
    Assert.assertTrue(literal.terms().stream().noneMatch(t -> t instanceof Const));
    Assert.assertTrue(literal.terms().stream().allMatch(t -> t instanceof Var));
  }

  @Test(expected = IllegalStateException.class)
  public void testNegatedBuiltinLiteral() {
    Literal literal = new Literal("~fn_isOk", new Var(), new Var());
  }

  @Test
  public void testFnEqBuiltin() {

    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();
    Literal literal = new Literal("fn_eq", new Var(), new Const(2), new Const(2));
    Literal newLiteral = literal.execute(kb.definitions()).next();

    Assert.assertEquals(new Literal("fn_eq", new Const(true), new Const(2), new Const(2)),
        newLiteral);
  }

  @Test
  public void testGroundedFnIsBuiltin() {

    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();
    Literal literal = new Literal("fn_is", new Const(2), new Const(2));
    Literal newLiteral = literal.execute(kb.definitions()).next();

    Assert.assertEquals(new Literal("fn_is", new Const(2), new Const(2)), newLiteral);
  }

  @Test
  public void testNotGroundedFnIsBuiltin() {

    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();
    Literal literal = new Literal("fn_is", new Var(), new Const(2));
    Literal newLiteral = literal.execute(kb.definitions()).next();

    Assert.assertEquals(new Literal("fn_is", new Const(2), new Const(2)), newLiteral);
  }

  @Test
  public void testInvalidFnIsBuiltin() {

    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();
    Literal literal = new Literal("fn_is", new Const(3), new Const(2));

    Assert.assertNull(literal.execute(kb.definitions()));
  }

  @Test
  public void testFnIsTrueOfTrueBuiltin() {

    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();
    Literal literal = new Literal("fn_is_true", new Const(true));
    Literal newLiteral = literal.execute(kb.definitions()).next();

    Assert.assertEquals(new Literal("fn_is_true", new Const(true)), newLiteral);
  }

  @Test
  public void testFnIsTrueOfFalseBuiltin() {

    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();
    Literal literal = new Literal("fn_is_true", new Const(false));

    Assert.assertNull(literal.execute(kb.definitions()));
  }

  @Test
  public void testFnIsFalseOfFalseBuiltin() {

    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();
    Literal literal = new Literal("fn_is_false", new Const(false));
    Literal newLiteral = literal.execute(kb.definitions()).next();

    Assert.assertEquals(new Literal("fn_is_false", new Const(false)), newLiteral);
  }

  @Test
  public void testFnIsFalseOfTrueBuiltin() {

    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();
    Literal literal = new Literal("fn_is_false", new Const(true));

    Assert.assertNull(literal.execute(kb.definitions()));
  }

  @Test
  public void testMergeFunctionsWithoutSubstitution() {

    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();
    Clause clause = parseClause("is_ok(X) :- fn_eq(X, fn_add(1, 1), 2).");
    Literal literal = clause.body().get(0);
    Literal newLiteral = literal.execute(kb.definitions()).next();

    Assert.assertEquals(1, newLiteral.terms().size());
    Assert.assertEquals(new Const(true), newLiteral.terms().get(0));
  }

  @Test
  public void testMergeFunctionsWithSubstitution() {

    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();
    Clause clause = parseClause("is_ok(X, Y) :- fn_eq(X, fn_add(Y, 1), 2).");
    Literal literal = clause.body().get(0);

    Map<Var, AbstractTerm> subst = new HashMap<>(); // substitute Y with 1
    subst.put((Var) clause.head().terms().get(1), new Const("1"));

    Literal newLiteral = literal.subst(subst).execute(kb.definitions()).next();

    Assert.assertEquals(2, newLiteral.terms().size());
    Assert.assertEquals(new Const(true), newLiteral.terms().get(0));
  }

  @Test
  public void testRelevantLiterals() {

    Literal literal0 = new Literal("~edge", new Const("a"), new Const("b"));
    Literal literal1 = new Literal("edge", new Const("a"), new Const("b"));
    Literal literal2 = new Literal("edge", new Const("a"), new Var());
    Literal literal3 = new Literal("edge", new Var(), new Const("c"));

    Assert.assertFalse(literal0.isRelevant(literal1));
    Assert.assertFalse(literal1.isRelevant(literal0));

    Assert.assertFalse(literal0.isRelevant(literal2));
    Assert.assertFalse(literal2.isRelevant(literal0));

    Assert.assertFalse(literal0.isRelevant(literal3));
    Assert.assertFalse(literal3.isRelevant(literal0));

    Assert.assertTrue(literal1.isRelevant(literal2));
    Assert.assertTrue(literal2.isRelevant(literal1));

    Assert.assertFalse(literal1.isRelevant(literal3));
    Assert.assertFalse(literal3.isRelevant(literal1));

    Assert.assertTrue(literal2.isRelevant(literal3));
    Assert.assertTrue(literal3.isRelevant(literal2));

    // Ensure all literals are relevant with themselves
    Assert.assertTrue(literal0.isRelevant(literal0));
    Assert.assertTrue(literal1.isRelevant(literal1));
    Assert.assertTrue(literal2.isRelevant(literal2));
    Assert.assertTrue(literal3.isRelevant(literal3));
  }

  @Test
  public void testId() {

    Literal literal0 = new Literal("~edge", new Const("a"), new Const("b"));
    Literal literal1 = new Literal("edge", new Const("a"), new Const("b"));
    Literal literal2 = new Literal("edge", new Const("a"), new Var());
    Literal literal3 = new Literal("edge", new Var(), new Const("c"));
    Literal literal4 = new Literal("edge", new Var(), new Const("b"));

    Assert.assertEquals("17:~edge/21:a1:b", literal0.id());
    Assert.assertEquals("16:edge/21:a1:b", literal1.id());

    Assert.assertTrue(WildcardMatcher.match(literal2.id(), "1?:edge/21:av*"));
    Assert.assertTrue(WildcardMatcher.match(literal3.id(), "1?:edge/2v*1:c"));
    Assert.assertTrue(WildcardMatcher.match(literal4.id(), "1?:edge/2v*:b"));

    Assert.assertNotEquals(literal0.id(), literal1.id());
    Assert.assertNotEquals(literal0.id(), literal2.id());
    Assert.assertNotEquals(literal0.id(), literal3.id());
    Assert.assertNotEquals(literal0.id(), literal4.id());

    Assert.assertNotEquals(literal1.id(), literal2.id());
    Assert.assertNotEquals(literal1.id(), literal3.id());
    Assert.assertNotEquals(literal1.id(), literal4.id());

    Assert.assertNotEquals(literal2.id(), literal3.id());
    Assert.assertNotEquals(literal2.id(), literal4.id());

    Assert.assertNotEquals(literal3.id(), literal4.id());
  }

  @Test
  public void testTag() {

    Literal literal0 = new Literal("~edge", new Const("a"), new Const("b"));
    Literal literal1 = new Literal("edge", new Const("a"), new Const("b"));
    Literal literal2 = new Literal("edge", new Const("a"), new Var());
    Literal literal3 = new Literal("edge", new Var(), new Const("c"));
    Literal literal4 = new Literal("edge", new Var(), new Const("b"));

    Assert.assertEquals("7:~edge/21:a1:b", literal0.tag());
    Assert.assertEquals("6:edge/21:a1:b", literal1.tag());
    Assert.assertEquals("6:edge/21:av1", literal2.tag());
    Assert.assertEquals("6:edge/2v01:c", literal3.tag());
    Assert.assertEquals("6:edge/2v01:b", literal4.tag());

    Assert.assertNotEquals(literal0.tag(), literal1.tag());
    Assert.assertNotEquals(literal0.tag(), literal2.tag());
    Assert.assertNotEquals(literal0.tag(), literal3.tag());
    Assert.assertNotEquals(literal0.tag(), literal4.tag());

    Assert.assertNotEquals(literal1.tag(), literal2.tag());
    Assert.assertNotEquals(literal1.tag(), literal3.tag());
    Assert.assertNotEquals(literal1.tag(), literal4.tag());

    Assert.assertNotEquals(literal2.tag(), literal3.tag());
    Assert.assertNotEquals(literal2.tag(), literal4.tag());

    Assert.assertNotEquals(literal3.tag(), literal4.tag());
  }

  @Test
  public void testToStringEncodesLf() {

    Literal literal = new Literal("edge", new Const("a\nb"));

    Assert.assertEquals("edge(\"a\\nb\")", literal.toString());
  }

  @Test
  public void testToStringEncodesCrLf() {

    Literal literal = new Literal("edge", new Const("a\r\nb"));

    Assert.assertEquals("edge(\"a\\nb\")", literal.toString());
  }
}
