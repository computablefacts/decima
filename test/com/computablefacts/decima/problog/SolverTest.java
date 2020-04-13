package com.computablefacts.decima.problog;

import static com.computablefacts.decima.problog.TestUtils.isValid;
import static com.computablefacts.decima.problog.TestUtils.kb;
import static com.computablefacts.decima.problog.TestUtils.parseClause;
import static com.computablefacts.decima.problog.TestUtils.solver;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Iterables;

public class SolverTest {

  @Test
  public void testSimpleQuery() {

    // Create kb
    InMemoryKnowledgeBase kb = kb();

    // Init kb with facts
    kb.azzert(parseClause("girl(alice)."));
    kb.azzert(parseClause("boy(alex)."));

    // Init kb with rules
    kb.azzert(parseClause("child(X) :- boy(X)."));
    kb.azzert(parseClause("child(Y) :- girl(Y)."));

    // Query kb
    // child(Z)?
    Solver solver = solver(kb);
    Literal query = new Literal("child", new Var());
    Set<Clause> proofs = solver.proofs(query);
    Set<Clause> solutions = solver.solve(query);

    // Verify answers
    Assert.assertEquals(2, proofs.size());
    Assert.assertTrue(isValid(proofs, "child(alice) :- girl(alice)."));
    Assert.assertTrue(isValid(proofs, "child(alex) :- boy(alex)."));

    Assert.assertEquals(2, solutions.size());
    Assert.assertTrue(isValid(solutions, "child(alice)."));
    Assert.assertTrue(isValid(solutions, "child(alex)."));
  }

  @Test
  public void testComplexQuery() {

    // Create kb
    InMemoryKnowledgeBase kb = kb();

    // Init kb with facts
    kb.azzert(parseClause("boy(bill)."));
    kb.azzert(parseClause("mother(alice, bill)."));

    // Init kb with rules
    kb.azzert(parseClause("child(X,Y) :- mother(Y,X)."));
    kb.azzert(parseClause("child(X,Y) :- father(Y,X)."));
    kb.azzert(parseClause("son(X,Y) :- child(X,Y),boy(X)."));

    // Query kb
    // son(Z, alice)?
    Solver solver = solver(kb);
    Literal query = new Literal("son", new Var(), new Const("alice"));
    Set<Clause> proofs = solver.proofs(query);
    Set<Clause> solutions = solver.solve(query);

    // Verify answers
    Assert.assertEquals(1, proofs.size());
    Assert.assertTrue(isValid(proofs, "son(bill, alice) :- mother(alice, bill), boy(bill)."));

    Assert.assertEquals(1, solutions.size());
    Assert.assertTrue(isValid(solutions, "son(bill, alice)."));
  }

  @Test
  public void testNegation() {

    // Create kb
    InMemoryKnowledgeBase kb = kb();

    // Init kb with facts
    kb.azzert(parseClause("girl(alice)."));
    kb.azzert(parseClause("boy(alex)."));
    kb.azzert(parseClause("girl(nikka)."));
    kb.azzert(parseClause("boy(nikka)."));

    // Init kb with rules
    kb.azzert(parseClause("human(X) :- girl(X), ~boy(X)."));
    kb.azzert(parseClause("human(X) :- boy(X), ~girl(X)."));

    // Query kb
    // human(Z)?
    Solver solver = solver(kb);
    Literal query = new Literal("human", new Var());
    Set<Clause> proofs = solver.proofs(query);
    Set<Clause> solutions = solver.solve(query);

    // Verify answers
    Assert.assertEquals(2, proofs.size());
    Assert.assertTrue(isValid(proofs, "human(alice) :- girl(alice), ~boy(alice)."));
    Assert.assertTrue(isValid(proofs, "human(alex) :- boy(alex), ~girl(alex)."));

    Assert.assertEquals(2, solutions.size());
    Assert.assertTrue(isValid(solutions, "human(alice)."));
    Assert.assertTrue(isValid(solutions, "human(alex)."));
  }

  @Test
  public void testRecursion() {

    // Create kb
    InMemoryKnowledgeBase kb = kb();

    // Init kb with facts
    kb.azzert(parseClause("edge(a,b)."));
    kb.azzert(parseClause("edge(b,c)."));
    kb.azzert(parseClause("edge(a,d)."));

    // Init kb with rules
    kb.azzert(parseClause("path(X, Y) :- edge(X, Y)."));
    kb.azzert(parseClause("path(X, Y) :- path(X, Z), edge(Z, Y)."));

    // Query kb
    // path(a, V)?
    Solver solver = solver(kb);
    Literal query = new Literal("path", new Const("a"), new Var());
    Set<Clause> proofs = solver.proofs(query);
    Set<Clause> solutions = solver.solve(query);

    // Verify answers
    Assert.assertEquals(3, proofs.size());
    Assert.assertTrue(isValid(proofs, "path(a, b) :- edge(a, b)."));
    Assert.assertTrue(isValid(proofs, "path(a, c) :- edge(a, b), edge(b, c)."));
    Assert.assertTrue(isValid(proofs, "path(a, d) :- edge(a, d)."));

    Assert.assertEquals(3, solutions.size());
    Assert.assertTrue(isValid(solutions, "path(a, b)."));
    Assert.assertTrue(isValid(solutions, "path(a, c)."));
    Assert.assertTrue(isValid(solutions, "path(a, d)."));
  }

  @Test
  public void testSimplePrimitive() {

    // Create kb
    InMemoryKnowledgeBase kb = kb();

    // Init kb with facts
    kb.azzert(parseClause("one(1)."));
    kb.azzert(parseClause("two(2)."));

    // Init kb with rules
    kb.azzert(parseClause("three(Z) :- one(X), two(Y), fn_add(W, X, Y), fn_int(Z, W)."));
    kb.azzert(parseClause("four(Z) :- three(X), fn_add(W, X, 1), fn_int(Z, W)."));

    // Query kb
    // three(Z)?
    Solver solver = solver(kb);
    Literal query1 = new Literal("three", new Var());
    Set<Clause> proofs1 = solver.proofs(query1);
    Set<Clause> solutions1 = solver.solve(query1);

    // Verify answers
    Assert.assertEquals(1, proofs1.size());
    Assert
        .assertTrue(isValid(proofs1, "three(3) :- one(1), two(2), fn_add(3, 1, 2), fn_int(3, 3)."));

    Assert.assertEquals(1, solutions1.size());
    Assert.assertTrue(isValid(solutions1, "three(3)."));

    // Query kb
    // four(Z)?
    Literal query2 = new Literal("four", new Var());
    Set<Clause> proofs2 = solver.proofs(query2);
    Set<Clause> solutions2 = solver.solve(query2);

    // Verify answers
    Assert.assertEquals(1, proofs2.size());
    Assert.assertTrue(isValid(proofs2,
        "four(4) :- one(1), two(2), fn_add(3, 1, 2), fn_int(3, 3), fn_add(4, 3, 1), fn_int(4, 4)."));

    Assert.assertEquals(1, solutions2.size());
    Assert.assertTrue(isValid(solutions2, "four(4)."));
  }

  @Test
  public void testIsTrue() {

    // Create kb
    InMemoryKnowledgeBase kb = kb();

    // Init kb with facts
    kb.azzert(parseClause("bagItems(\"red_bag\", 1)."));
    kb.azzert(parseClause("bagItems(\"green_bag\", 2)."));
    kb.azzert(parseClause("bagItems(\"blue_bag\", 3)."));

    // Init kb with rules
    kb.azzert(parseClause(
        "hasMoreItems(X, Y) :- bagItems(X, A), bagItems(Y, B), fn_gt(U, A, B), fn_is_true(U)."));

    // Query kb
    // hasMoreItems(X, Y)?
    Solver solver = solver(kb);
    Literal query = new Literal("hasMoreItems", new Var(), new Var());
    Set<Clause> proofs = solver.proofs(query);
    Set<Clause> solutions = solver.solve(query);

    // Verify answers
    Assert.assertEquals(3, proofs.size());
    Assert.assertTrue(isValid(proofs,
        "hasMoreItems(green_bag, red_bag) :- bagItems(green_bag, 2), bagItems(red_bag, 1), fn_gt(true, 2, 1), fn_is_true(true)."));
    Assert.assertTrue(isValid(proofs,
        "hasMoreItems(blue_bag, red_bag) :- bagItems(blue_bag, 3), bagItems(red_bag, 1), fn_gt(true, 3, 1), fn_is_true(true)."));
    Assert.assertTrue(isValid(proofs,
        "hasMoreItems(blue_bag, green_bag) :- bagItems(blue_bag, 3), bagItems(green_bag, 2), fn_gt(true, 3, 2), fn_is_true(true)."));

    Assert.assertEquals(3, solutions.size());
    Assert.assertTrue(isValid(solutions, "hasMoreItems(\"green_bag\", \"red_bag\")."));
    Assert.assertTrue(isValid(solutions, "hasMoreItems(\"blue_bag\", \"red_bag\")."));
    Assert.assertTrue(isValid(solutions, "hasMoreItems(\"blue_bag\", \"green_bag\")."));
  }

  @Test
  public void testIsFalse() {

    // Create kb
    InMemoryKnowledgeBase kb = kb();

    // Init kb with facts
    kb.azzert(parseClause("bagItems(\"red_bag\", 1)."));
    kb.azzert(parseClause("bagItems(\"green_bag\", 2)."));
    kb.azzert(parseClause("bagItems(\"blue_bag\", 2)."));

    // Init kb with rules
    kb.azzert(parseClause(
        "hasDifferentNumberOfItems(X, Y) :- bagItems(X, A), bagItems(Y, B), fn_eq(U, A, B), fn_is_false(U)."));

    // Query kb
    // hasDifferentNumberOfItems(X, Y)?
    Solver solver = solver(kb);
    Literal query = new Literal("hasDifferentNumberOfItems", new Var(), new Var());
    Set<Clause> proofs = solver.proofs(query);
    Set<Clause> solutions = solver.solve(query);

    // Verify answers
    Assert.assertEquals(4, proofs.size());
    Assert.assertTrue(isValid(proofs,
        "hasDifferentNumberOfItems(red_bag, green_bag) :- bagItems(red_bag, 1), bagItems(green_bag, 2), fn_eq(false, 1, 2), fn_is_false(false)."));
    Assert.assertTrue(isValid(proofs,
        "hasDifferentNumberOfItems(red_bag, blue_bag) :- bagItems(red_bag, 1),bagItems(blue_bag, 2), fn_eq(false, 1, 2), fn_is_false(false)."));
    Assert.assertTrue(isValid(proofs,
        "hasDifferentNumberOfItems(green_bag, red_bag) :- bagItems(green_bag, 2), bagItems(red_bag, 1), fn_eq(false, 2, 1), fn_is_false(false)."));
    Assert.assertTrue(isValid(proofs,
        "hasDifferentNumberOfItems(blue_bag, red_bag) :- bagItems(blue_bag, 2), bagItems(red_bag, 1), fn_eq(false, 2, 1), fn_is_false(false)."));

    Assert.assertEquals(4, solutions.size());
    Assert.assertTrue(Iterables.get(solutions, 0).isFact());
    Assert.assertTrue(Iterables.get(solutions, 1).isFact());
    Assert.assertTrue(Iterables.get(solutions, 2).isFact());
    Assert.assertTrue(Iterables.get(solutions, 3).isFact());
    Assert.assertTrue(isValid(solutions, "hasDifferentNumberOfItems(\"red_bag\", \"green_bag\")."));
    Assert.assertTrue(isValid(solutions, "hasDifferentNumberOfItems(\"red_bag\", \"blue_bag\")."));
    Assert.assertTrue(isValid(solutions, "hasDifferentNumberOfItems(\"green_bag\", \"red_bag\")."));
    Assert.assertTrue(isValid(solutions, "hasDifferentNumberOfItems(\"blue_bag\", \"red_bag\")."));
  }

  @Test
  public void testDoubleNegation() {

    // Create kb
    InMemoryKnowledgeBase kb = kb();

    // Init kb with facts
    kb.azzert(parseClause("boy(a)."));
    kb.azzert(parseClause("boy(b)."));
    kb.azzert(parseClause("girl(a)."));
    kb.azzert(parseClause("girl(c)."));

    // Init kb with rules
    kb.azzert(parseClause("isBoy(X) :- boy(X), ~girl(X)."));
    kb.azzert(parseClause("isGirl(X) :- girl(X), ~boy(X)."));
    kb.azzert(parseClause("isBoyNotGirl(X) :- isBoy(X), ~isGirl(X)."));
    kb.azzert(parseClause("isGirlNotBoy(X) :- isGirl(X), ~isBoy(X)."));
    kb.azzert(parseClause(
        "match(X, Y) :- isBoyNotGirl(X), ~isGirlNotBoy(X), isGirlNotBoy(Y), ~isBoyNotGirl(Y)."));

    // Query kb
    // match(X, Y)?
    Solver solver = solver(kb);
    Literal query = new Literal("match", new Var(), new Var());
    Set<Clause> proofs = solver.proofs(query);
    Set<Clause> solutions = solver.solve(query);

    // Verify answers
    Assert.assertEquals(1, proofs.size());
    Assert.assertTrue(isValid(proofs,
        "match(b, c) :- boy(b), ~girl(b), ~isGirl(b), ~isGirlNotBoy(b), girl(c), ~boy(c), ~isBoy(c), ~isBoyNotGirl(c)."));

    Assert.assertEquals(1, solutions.size());
    Assert.assertTrue(isValid(solutions, "match(b, c)."));
  }

  /**
   * Non-ground
   *
   * Description: negation on non-ground probabilistic facts are forbidden.
   *
   * See https://github.com/ML-KULeuven/problog/blob/master/test/nonground.pl
   */
  @Test(expected = IllegalStateException.class)
  public void testNonGroundProbabilisticClause() {

    // Create kb
    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();

    // Init kb with facts
    kb.azzert(parseClause("0.4::b(1)."));
    kb.azzert(parseClause("0.4::b(2)."));
    kb.azzert(parseClause("0.4::c(1)."));
    kb.azzert(parseClause("0.4::c(2)."));

    // Init kb with rules
    kb.azzert(parseClause("0.4::a(X,Y) :- \\+b(X), \\+c(Y)."));

    // Query kb
    // a(X, Y)?
    Solver solver = new Solver(kb);
    Literal query = new Literal("a", new Var(), new Var());
    Set<Clause> proofs = solver.proofs(query);
  }

  /**
   * Variable unification in query
   *
   * See https://github.com/ML-KULeuven/problog/blob/master/test/query_same.pl
   */
  @Test
  public void testVariableUnificationInQuery() {

    // Create kb
    InMemoryKnowledgeBase kb = kb();

    // Init kb with facts
    kb.azzert(parseClause("a(2, 3)."));
    kb.azzert(parseClause("a(1, 1)."));

    // Init kb with rules
    kb.azzert(parseClause("p(X) :- a(X, X)."));

    // Query kb
    // a(X, X)?
    // p(X)?
    Solver solver = new Solver(kb);
    Var x = new Var();
    Literal query1 = new Literal("a", x, x);
    List<Clause> proofs1 = new ArrayList<>(solver.proofs(query1));

    Literal query2 = new Literal("p", x);
    List<Clause> proofs2 = new ArrayList<>(solver.proofs(query2));

    // Verify answers
    // a(1,1).
    // p(1) :- a(1, 1).
    Assert.assertEquals(1, proofs1.size());
    Assert.assertEquals(1, proofs2.size());

    Assert.assertEquals(proofs1.get(0).head(), proofs2.get(0).body().get(0));
  }
}
