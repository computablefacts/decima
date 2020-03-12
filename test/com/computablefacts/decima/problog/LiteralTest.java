package com.computablefacts.decima.problog;

import org.junit.Assert;
import org.junit.Test;

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
}
