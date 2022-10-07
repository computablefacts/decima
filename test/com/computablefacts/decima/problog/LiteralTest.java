package com.computablefacts.decima.problog;

import static com.computablefacts.decima.problog.AbstractTerm.newConst;
import static com.computablefacts.decima.problog.AbstractTerm.newVar;
import static com.computablefacts.decima.problog.Parser.parseClause;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;


public class LiteralTest {

  @Test
  public void testLiteral() {

    Predicate predicate = new Predicate("edge", 2);
    Literal literal = new Literal("edge", newConst("a"), newConst("b"));

    Assert.assertTrue(literal.isGrounded());
    Assert.assertEquals(predicate, literal.predicate());

    Assert.assertEquals(2, literal.terms().size());
    Assert.assertTrue(literal.hasTerm(newConst("a")));
    Assert.assertTrue(literal.hasTerm(newConst("b")));
  }

  @Test
  public void testNegatedLiteral() {

    Predicate predicate = new Predicate("~edge", 2);
    Literal literal = new Literal("~edge", newConst("a"), newConst("b"));

    Assert.assertTrue(literal.isGrounded());
    Assert.assertEquals(predicate, literal.predicate());

    Assert.assertEquals(2, literal.terms().size());
    Assert.assertTrue(literal.hasTerm(newConst("a")));
    Assert.assertTrue(literal.hasTerm(newConst("b")));
  }

  @Test
  public void testIsSemiGroundedWithConstAndWildcard() {

    Literal literal = new Literal("edge", newConst("a"), newVar(true));

    Assert.assertFalse(literal.isGrounded());
    Assert.assertTrue(literal.isSemiGrounded());
  }

  @Test
  public void testIsSemiGroundedWithConstOnly() {

    Literal literal = new Literal("edge", newConst("a"), newConst("b"));

    Assert.assertTrue(literal.isGrounded());
    Assert.assertTrue(literal.isSemiGrounded());
  }

  @Test
  public void testIsSemiGroundedWithWildcardOnly() {

    Literal literal = new Literal("edge", newVar(true), newVar(true));

    Assert.assertFalse(literal.isGrounded());
    Assert.assertTrue(literal.isSemiGrounded());
  }

  @Test
  public void testIsNotSemiGrounded() {

    Literal literal = new Literal("edge", newConst("a"), newVar());

    Assert.assertFalse(literal.isGrounded());
    Assert.assertFalse(literal.isSemiGrounded());
  }

  @Test
  public void testBuiltinLiteral() {

    Predicate predicate = new Predicate("fn_isOk", 2);
    Literal literal = new Literal("fn_isOk", newVar(), newVar());

    Assert.assertFalse(literal.isGrounded());
    Assert.assertEquals(predicate, literal.predicate());

    Assert.assertEquals(2, literal.terms().size());
    Assert.assertTrue(literal.terms().stream().noneMatch(t -> t instanceof Const));
    Assert.assertTrue(literal.terms().stream().allMatch(t -> t instanceof Var));
  }

  @Test(expected = IllegalStateException.class)
  public void testNegatedBuiltinLiteral() {
    Literal literal = new Literal("~fn_isOk", newVar(), newVar());
  }

  @Test
  public void testFnEqBuiltin() {

    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();
    Literal literal = new Literal("fn_eq", newVar(), newConst(2), newConst(2));
    Literal newLiteral = literal.execute(kb.definitions()).next();

    Assert.assertEquals(new Literal("fn_eq", newConst(true), newConst(2), newConst(2)), newLiteral);
  }

  @Test
  public void testGroundedFnIsBuiltin() {

    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();
    Literal literal = new Literal("fn_is", newConst(2), newConst(2));
    Literal newLiteral = literal.execute(kb.definitions()).next();

    Assert.assertEquals(new Literal("fn_is", newConst(2), newConst(2)), newLiteral);
  }

  @Test
  public void testNotGroundedFnIsBuiltin() {

    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();
    Literal literal = new Literal("fn_is", newVar(), newConst(2));
    Literal newLiteral = literal.execute(kb.definitions()).next();

    Assert.assertEquals(new Literal("fn_is", newConst(2), newConst(2)), newLiteral);
  }

  @Test
  public void testInvalidFnIsBuiltin() {

    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();
    Literal literal = new Literal("fn_is", newConst(3), newConst(2));

    Assert.assertNull(literal.execute(kb.definitions()));
  }

  @Test
  public void testFnIsTrueOfTrueBuiltin() {

    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();
    Literal literal = new Literal("fn_is_true", newConst(true));
    Literal newLiteral = literal.execute(kb.definitions()).next();

    Assert.assertEquals(new Literal("fn_is_true", newConst(true)), newLiteral);
  }

  @Test
  public void testFnIsTrueOfFalseBuiltin() {

    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();
    Literal literal = new Literal("fn_is_true", newConst(false));

    Assert.assertNull(literal.execute(kb.definitions()));
  }

  @Test
  public void testFnIsFalseOfFalseBuiltin() {

    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();
    Literal literal = new Literal("fn_is_false", newConst(false));
    Literal newLiteral = literal.execute(kb.definitions()).next();

    Assert.assertEquals(new Literal("fn_is_false", newConst(false)), newLiteral);
  }

  @Test
  public void testFnIsFalseOfTrueBuiltin() {

    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();
    Literal literal = new Literal("fn_is_false", newConst(true));

    Assert.assertNull(literal.execute(kb.definitions()));
  }

  @Test
  public void testMergeFunctionsWithoutSubstitution() {

    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();
    Clause clause = parseClause("is_ok(X) :- fn_eq(X, fn_add(1, 1), 2).");
    Literal literal = clause.body().get(0);
    Literal newLiteral = literal.execute(kb.definitions()).next();

    Assert.assertEquals(1, newLiteral.terms().size());
    Assert.assertEquals(newConst(true), newLiteral.terms().get(0));
  }

  @Test
  public void testMergeFunctionsWithSubstitution() {

    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();
    Clause clause = parseClause("is_ok(X, Y) :- fn_eq(X, fn_add(Y, 1), 2).");
    Literal literal = clause.body().get(0);

    Map<Var, AbstractTerm> subst = new HashMap<>(); // substitute Y with 1
    subst.put((Var) clause.head().terms().get(1), newConst("1"));

    Literal newLiteral = literal.subst(subst).execute(kb.definitions()).next();

    Assert.assertEquals(2, newLiteral.terms().size());
    Assert.assertEquals(newConst(true), newLiteral.terms().get(0));
  }

  @Test
  public void testRelevantLiterals() {

    Literal literal0 = new Literal("~edge", newConst("a"), newConst("b"));
    Literal literal1 = new Literal("edge", newConst("a"), newConst("b"));
    Literal literal2 = new Literal("edge", newConst("a"), newVar());
    Literal literal3 = new Literal("edge", newVar(), newConst("c"));

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

    Literal literal0 = new Literal("~edge", newConst("a"), newConst("b"));
    Literal literal1 = new Literal("edge", newConst("a"), newConst("b"));
    Literal literal2 = new Literal("edge", newConst("a"), newVar());
    Literal literal3 = new Literal("edge", newVar(), newConst("c"));
    Literal literal4 = new Literal("edge", newVar(), newConst("b"));

    Assert.assertEquals("1:~edge/2:ca:cb", literal0.id());
    Assert.assertEquals("1:edge/2:ca:cb", literal1.id());

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

    Literal literal0 = new Literal("~edge", newConst("a"), newConst("b"));
    Literal literal1 = new Literal("edge", newConst("a"), newConst("b"));
    Literal literal2 = new Literal("edge", newConst("a"), newVar());
    Literal literal3 = new Literal("edge", newVar(), newConst("c"));
    Literal literal4 = new Literal("edge", newVar(), newConst("b"));

    Assert.assertEquals("~edge/2:ca:cb", literal0.tag());
    Assert.assertEquals("edge/2:ca:cb", literal1.tag());
    Assert.assertEquals("edge/2:ca:v", literal2.tag());
    Assert.assertEquals("edge/2:v:cc", literal3.tag());
    Assert.assertEquals("edge/2:v:cb", literal4.tag());

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

    Literal literal = new Literal("edge", newConst("a\nb"));

    Assert.assertEquals("edge(\"b64_(YQpi)\")", literal.toString());
  }

  @Test
  public void testToStringEncodesCrLf() {

    Literal literal = new Literal("edge", newConst("a\r\nb"));

    Assert.assertEquals("edge(\"b64_(YQ0KYg==)\")", literal.toString());
  }
}
