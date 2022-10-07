package com.computablefacts.decima.problog;

import static com.computablefacts.decima.problog.AbstractTerm.newConst;
import static com.computablefacts.decima.problog.AbstractTerm.newVar;
import static com.computablefacts.decima.problog.Parser.parseClause;
import static com.computablefacts.decima.problog.TestUtils.checkAnswers;
import static com.computablefacts.decima.problog.TestUtils.checkProofs;

import com.computablefacts.asterix.trie.Trie;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

public class SolverTest {

  @Test
  public void testSimpleQuery() {

    // Create kb
    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();

    // Init kb with facts
    kb.azzert(parseClause("girl(alice)."));
    kb.azzert(parseClause("boy(alex)."));

    // Init kb with rules
    kb.azzert(parseClause("child(X) :- boy(X)."));
    kb.azzert(parseClause("child(Y) :- girl(Y)."));

    // Query kb
    // child(Z)?
    Solver solver = new Solver(kb, true);
    Literal query = new Literal("child", newVar());
    Set<Clause> proofs = solver.proofs(query);
    Set<Clause> answers = Sets.newHashSet(solver.solve(query));
    Map<Literal, Trie<Literal>> tries = solver.tries(query);

    // Verify answers
    Assert.assertEquals(2, proofs.size());
    Assert.assertEquals(2, answers.size());
    Assert.assertEquals(2, tries.size());

    Clause answer1 = parseClause("child(alice) :- girl(alice).");
    Clause answer2 = parseClause("child(alex) :- boy(alex).");

    Assert.assertTrue(checkAnswers(answers, Sets.newHashSet(answer1, answer2)));
    Assert.assertTrue(checkProofs(tries, Sets.newHashSet(answer1, answer2)));
  }

  @Test
  public void testComplexQuery() {

    // Create kb
    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();

    // Init kb with facts
    kb.azzert(parseClause("boy(bill)."));
    kb.azzert(parseClause("mother(alice, bill)."));

    // Init kb with rules
    kb.azzert(parseClause("child(X,Y) :- mother(Y,X)."));
    kb.azzert(parseClause("child(X,Y) :- father(Y,X)."));
    kb.azzert(parseClause("son(X,Y) :- child(X,Y),boy(X)."));

    // Query kb
    // son(Z, alice)?
    Solver solver = new Solver(kb, true);
    Literal query = new Literal("son", newVar(), newConst("alice"));
    Set<Clause> proofs = solver.proofs(query);
    Set<Clause> answers = Sets.newHashSet(solver.solve(query));
    Map<Literal, Trie<Literal>> tries = solver.tries(query);

    // Verify answers
    Assert.assertEquals(1, proofs.size());
    Assert.assertEquals(1, answers.size());
    Assert.assertEquals(1, tries.size());

    Clause answer = parseClause("son(bill, alice) :- mother(alice, bill), boy(bill).");

    Assert.assertTrue(checkAnswers(answers, Sets.newHashSet(answer)));
    Assert.assertTrue(checkProofs(tries, Sets.newHashSet(answer)));
  }

  @Test
  public void testNegation() {

    // Create kb
    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();

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
    Solver solver = new Solver(kb, true);
    Literal query = new Literal("human", newVar());
    Set<Clause> proofs = solver.proofs(query);
    Set<Clause> answers = Sets.newHashSet(solver.solve(query));
    Map<Literal, Trie<Literal>> tries = solver.tries(query);

    // Verify answers
    Assert.assertEquals(2, proofs.size());
    Assert.assertEquals(2, answers.size());
    Assert.assertEquals(2, tries.size());

    Clause answer1 = parseClause("human(alice) :- girl(alice), ~boy(alice).");
    Clause answer2 = parseClause("human(alex) :- boy(alex), ~girl(alex).");

    Assert.assertTrue(checkAnswers(answers, Sets.newHashSet(answer1, answer2)));
    Assert.assertTrue(checkProofs(tries, Sets.newHashSet(answer1, answer2)));
  }

  @Test
  public void testRecursion() {

    // Create kb
    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();

    // Init kb with facts
    kb.azzert(parseClause("edge(a,b)."));
    kb.azzert(parseClause("edge(b,c)."));
    kb.azzert(parseClause("edge(a,d)."));

    // Init kb with rules
    kb.azzert(parseClause("path(X, Y) :- edge(X, Y)."));
    kb.azzert(parseClause("path(X, Y) :- path(X, Z), edge(Z, Y)."));

    // Query kb
    // path(a, V)?
    Solver solver = new Solver(kb, true);
    Literal query = new Literal("path", newConst("a"), newVar());
    Set<Clause> proofs = solver.proofs(query);
    Set<Clause> answers = Sets.newHashSet(solver.solve(query));
    Map<Literal, Trie<Literal>> tries = solver.tries(query);

    // Verify answers
    Assert.assertEquals(3, proofs.size());
    Assert.assertEquals(3, answers.size());
    Assert.assertEquals(3, tries.size());

    Clause answer1 = parseClause("path(a, b) :- edge(a, b).");
    Clause answer2 = parseClause("path(a, c) :- edge(a, b), edge(b, c).");
    Clause answer3 = parseClause("path(a, d) :- edge(a, d).");

    Assert.assertTrue(checkAnswers(answers, Sets.newHashSet(answer1, answer2, answer3)));
    Assert.assertTrue(checkProofs(tries, Sets.newHashSet(answer1, answer2, answer3)));
  }

  @Test
  public void testSimplePrimitive() {

    // Create kb
    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();

    // Init kb with facts
    kb.azzert(parseClause("one(1)."));
    kb.azzert(parseClause("two(2)."));

    // Init kb with rules
    kb.azzert(parseClause("three(Z) :- one(X), two(Y), fn_add(W, X, Y), fn_int(Z, W)."));
    kb.azzert(parseClause("four(Z) :- three(X), fn_add(W, X, 1), fn_int(Z, W)."));

    // Query kb
    // three(Z)?
    Solver solver = new Solver(kb, true);
    Literal query1 = new Literal("three", newVar());
    Set<Clause> proofs1 = solver.proofs(query1);
    Set<Clause> answers1 = Sets.newHashSet(solver.solve(query1));
    Map<Literal, Trie<Literal>> tries1 = solver.tries(query1);

    // Verify answers
    Assert.assertEquals(1, proofs1.size());
    Assert.assertEquals(1, answers1.size());
    Assert.assertEquals(1, tries1.size());

    Clause answer1 = parseClause("three(3) :- one(1), two(2), fn_add(3, 1, 2), fn_int(3, 3).");

    Assert.assertTrue(checkAnswers(answers1, Sets.newHashSet(answer1)));
    Assert.assertTrue(checkProofs(tries1, Sets.newHashSet(answer1)));

    // Query kb
    // four(Z)?
    Literal query2 = new Literal("four", newVar());
    Set<Clause> proofs2 = solver.proofs(query2);
    Set<Clause> answers2 = Sets.newHashSet(solver.solve(query2));
    Map<Literal, Trie<Literal>> tries2 = solver.tries(query2);

    // Verify answers
    Assert.assertEquals(1, proofs2.size());
    Assert.assertEquals(1, answers2.size());
    Assert.assertEquals(1, tries1.size());

    Clause answer2 = parseClause(
        "four(4) :- one(1), two(2), fn_add(3, 1, 2), fn_int(3, 3), fn_add(4, 3, 1), fn_int(4, 4).");

    Assert.assertTrue(checkAnswers(answers2, Sets.newHashSet(answer2)));
    Assert.assertTrue(checkProofs(tries2, Sets.newHashSet(answer2)));
  }

  @Test
  public void testIsTrue() {

    // Create kb
    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();

    // Init kb with facts
    kb.azzert(parseClause("bagItems(\"red_bag\", 1)."));
    kb.azzert(parseClause("bagItems(\"green_bag\", 2)."));
    kb.azzert(parseClause("bagItems(\"blue_bag\", 3)."));

    // Init kb with rules
    kb.azzert(parseClause("hasMoreItems(X, Y) :- bagItems(X, A), bagItems(Y, B), fn_gt(U, A, B), fn_is_true(U)."));

    // Query kb
    // hasMoreItems(X, Y)?
    Solver solver = new Solver(kb, true);
    Literal query = new Literal("hasMoreItems", newVar(), newVar());
    Set<Clause> proofs = solver.proofs(query);
    Set<Clause> answers = Sets.newHashSet(solver.solve(query));
    Map<Literal, Trie<Literal>> tries = solver.tries(query);

    // Verify answers
    Assert.assertEquals(3, proofs.size());
    Assert.assertEquals(3, answers.size());
    Assert.assertEquals(3, tries.size());

    Clause answer1 = parseClause(
        "hasMoreItems(green_bag, red_bag) :- bagItems(green_bag, 2), bagItems(red_bag, 1), fn_gt(true, 2, 1), fn_is_true(true).");
    Clause answer2 = parseClause(
        "hasMoreItems(blue_bag, red_bag) :- bagItems(blue_bag, 3), bagItems(red_bag, 1), fn_gt(true, 3, 1), fn_is_true(true).");
    Clause answer3 = parseClause(
        "hasMoreItems(blue_bag, green_bag) :- bagItems(blue_bag, 3), bagItems(green_bag, 2), fn_gt(true, 3, 2), fn_is_true(true).");

    Assert.assertTrue(checkAnswers(answers, Sets.newHashSet(answer1, answer2, answer3)));
    Assert.assertTrue(checkProofs(tries, Sets.newHashSet(answer1, answer2, answer3)));
  }

  @Test
  public void testIsFalse() {

    // Create kb
    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();

    // Init kb with facts
    kb.azzert(parseClause("bagItems(\"red_bag\", 1)."));
    kb.azzert(parseClause("bagItems(\"green_bag\", 2)."));
    kb.azzert(parseClause("bagItems(\"blue_bag\", 2)."));

    // Init kb with rules
    kb.azzert(parseClause(
        "hasDifferentNumberOfItems(X, Y) :- bagItems(X, A), bagItems(Y, B), fn_eq(U, A, B), fn_is_false(U)."));

    // Query kb
    // hasDifferentNumberOfItems(X, Y)?
    Solver solver = new Solver(kb, true);
    Literal query = new Literal("hasDifferentNumberOfItems", newVar(), newVar());
    Set<Clause> proofs = solver.proofs(query);
    Set<Clause> answers = Sets.newHashSet(solver.solve(query));
    Map<Literal, Trie<Literal>> tries = solver.tries(query);

    // Verify answers
    Assert.assertEquals(4, proofs.size());
    Assert.assertEquals(4, answers.size());
    Assert.assertEquals(4, tries.size());

    Clause answer1 = parseClause(
        "hasDifferentNumberOfItems(red_bag, green_bag) :- bagItems(red_bag, 1), bagItems(green_bag, 2), fn_eq(false, 1, 2), fn_is_false(false).");
    Clause answer2 = parseClause(
        "hasDifferentNumberOfItems(red_bag, blue_bag) :- bagItems(red_bag, 1),bagItems(blue_bag, 2), fn_eq(false, 1, 2), fn_is_false(false).");
    Clause answer3 = parseClause(
        "hasDifferentNumberOfItems(green_bag, red_bag) :- bagItems(green_bag, 2), bagItems(red_bag, 1), fn_eq(false, 2, 1), fn_is_false(false).");
    Clause answer4 = parseClause(
        "hasDifferentNumberOfItems(blue_bag, red_bag) :- bagItems(blue_bag, 2), bagItems(red_bag, 1), fn_eq(false, 2, 1), fn_is_false(false).");

    Assert.assertTrue(checkAnswers(answers, Sets.newHashSet(answer1, answer2, answer3, answer4)));
    Assert.assertTrue(checkProofs(tries, Sets.newHashSet(answer1, answer2, answer3, answer4)));
  }

  @Test
  public void testSampleOfSizeMinus1() {

    // Create kb
    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();

    // Init kb with facts
    kb.azzert(parseClause("bagItems(\"red_bag\", 1)."));
    kb.azzert(parseClause("bagItems(\"green_bag\", 2)."));
    kb.azzert(parseClause("bagItems(\"blue_bag\", 2)."));

    // Init kb with rules
    kb.azzert(parseClause(
        "hasDifferentNumberOfItems(X, Y) :- bagItems(X, A), bagItems(Y, B), fn_eq(U, A, B), fn_is_false(U)."));

    // Query kb
    // hasDifferentNumberOfItems(X, Y)?
    Solver solver = new Solver(kb, true);
    Literal query = new Literal("hasDifferentNumberOfItems", newVar(), newVar());
    Set<Clause> proofs = solver.proofs(query);
    Set<Clause> answers = Sets.newHashSet(solver.solve(query, -1));
    Map<Literal, Trie<Literal>> tries = solver.tries(query);

    // Verify answers
    Assert.assertEquals(4, answers.size());
    Assert.assertEquals(4, tries.size());
    Assert.assertEquals(4, proofs.size());

    Clause answer1 = parseClause(
        "hasDifferentNumberOfItems(\"green_bag\", \"red_bag\") :- bagItems(\"green_bag\", \"2\"), bagItems(\"red_bag\", \"1\"), fn_eq(\"false\", \"2\", \"1\"), fn_is_false(\"false\").");
    Clause answer2 = parseClause(
        "hasDifferentNumberOfItems(\"red_bag\", \"blue_bag\") :- bagItems(\"red_bag\", \"1\"), bagItems(\"blue_bag\", \"2\"), fn_eq(\"false\", \"1\", \"2\"), fn_is_false(\"false\").");
    Clause answer3 = parseClause(
        "hasDifferentNumberOfItems(\"red_bag\", \"green_bag\") :- bagItems(\"red_bag\", \"1\"), bagItems(\"green_bag\", \"2\"), fn_eq(\"false\", \"1\", \"2\"), fn_is_false(\"false\").");
    Clause answer4 = parseClause(
        "hasDifferentNumberOfItems(\"blue_bag\", \"red_bag\") :- bagItems(\"blue_bag\", \"2\"), bagItems(\"red_bag\", \"1\"), fn_eq(\"false\", \"2\", \"1\"), fn_is_false(\"false\").");

    Assert.assertTrue(checkAnswers(answers, Sets.newHashSet(answer1, answer2, answer3, answer4)));
    Assert.assertTrue(checkProofs(tries, Sets.newHashSet(answer1, answer2, answer3, answer4)));
  }

  @Test
  public void testSampleOfSize1() {

    // Create kb
    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();

    // Init kb with facts
    kb.azzert(parseClause("bagItems(\"red_bag\", 1)."));
    kb.azzert(parseClause("bagItems(\"green_bag\", 2)."));
    kb.azzert(parseClause("bagItems(\"blue_bag\", 2)."));

    // Init kb with rules
    kb.azzert(parseClause(
        "hasDifferentNumberOfItems(X, Y) :- bagItems(X, A), bagItems(Y, B), fn_eq(U, A, B), fn_is_false(U)."));

    // Query kb
    // hasDifferentNumberOfItems(X, Y)?
    Solver solver = new Solver(kb, true);
    Literal query = new Literal("hasDifferentNumberOfItems", newVar(), newVar());
    Set<Clause> answers = Sets.newHashSet(solver.solve(query, 1));
    Set<Clause> proofs = solver.proofs(query);
    Map<Literal, Trie<Literal>> tries = solver.tries(query);

    // Verify answers
    Assert.assertEquals(1, proofs.size());
    Assert.assertEquals(1, answers.size());
    Assert.assertEquals(1, tries.size());

    Clause answer1 = parseClause(
        "hasDifferentNumberOfItems(\"green_bag\", \"red_bag\") :- bagItems(\"green_bag\", \"2\"), bagItems(\"red_bag\", \"1\"), fn_eq(\"false\", \"2\", \"1\"), fn_is_false(\"false\").");
    Clause answer2 = parseClause(
        "hasDifferentNumberOfItems(\"red_bag\", \"blue_bag\") :- bagItems(\"red_bag\", \"1\"), bagItems(\"blue_bag\", \"2\"), fn_eq(\"false\", \"1\", \"2\"), fn_is_false(\"false\").");
    Clause answer3 = parseClause(
        "hasDifferentNumberOfItems(\"red_bag\", \"green_bag\") :- bagItems(\"red_bag\", \"1\"), bagItems(\"green_bag\", \"2\"), fn_eq(\"false\", \"1\", \"2\"), fn_is_false(\"false\").");
    Clause answer4 = parseClause(
        "hasDifferentNumberOfItems(\"blue_bag\", \"red_bag\") :- bagItems(\"blue_bag\", \"2\"), bagItems(\"red_bag\", \"1\"), fn_eq(\"false\", \"2\", \"1\"), fn_is_false(\"false\").");

    @com.google.errorprone.annotations.Var int count = checkAnswers(proofs, Sets.newHashSet(answer1)) ? 1 : 0;
    count += checkAnswers(proofs, Sets.newHashSet(answer2)) ? 1 : 0;
    count += checkAnswers(proofs, Sets.newHashSet(answer3)) ? 1 : 0;
    count += checkAnswers(proofs, Sets.newHashSet(answer4)) ? 1 : 0;

    Assert.assertEquals(1, count);
  }

  @Test
  public void testSampleOfSize2() {

    // Create kb
    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();

    // Init kb with facts
    kb.azzert(parseClause("bagItems(\"red_bag\", 1)."));
    kb.azzert(parseClause("bagItems(\"green_bag\", 2)."));
    kb.azzert(parseClause("bagItems(\"blue_bag\", 2)."));

    // Init kb with rules
    kb.azzert(parseClause(
        "hasDifferentNumberOfItems(X, Y) :- bagItems(X, A), bagItems(Y, B), fn_eq(U, A, B), fn_is_false(U)."));

    // Query kb
    // hasDifferentNumberOfItems(X, Y)?
    Solver solver = new Solver(kb, true);
    Literal query = new Literal("hasDifferentNumberOfItems", newVar(), newVar());
    Set<Clause> answers = Sets.newHashSet(solver.solve(query, 2));
    Set<Clause> proofs = solver.proofs(query);
    Map<Literal, Trie<Literal>> tries = solver.tries(query);

    // Verify answers
    Assert.assertEquals(2, proofs.size());
    Assert.assertEquals(2, answers.size());
    Assert.assertEquals(2, tries.size());

    Clause answer1 = parseClause(
        "hasDifferentNumberOfItems(\"green_bag\", \"red_bag\") :- bagItems(\"green_bag\", \"2\"), bagItems(\"red_bag\", \"1\"), fn_eq(\"false\", \"2\", \"1\"), fn_is_false(\"false\").");
    Clause answer2 = parseClause(
        "hasDifferentNumberOfItems(\"red_bag\", \"blue_bag\") :- bagItems(\"red_bag\", \"1\"), bagItems(\"blue_bag\", \"2\"), fn_eq(\"false\", \"1\", \"2\"), fn_is_false(\"false\").");
    Clause answer3 = parseClause(
        "hasDifferentNumberOfItems(\"red_bag\", \"green_bag\") :- bagItems(\"red_bag\", \"1\"), bagItems(\"green_bag\", \"2\"), fn_eq(\"false\", \"1\", \"2\"), fn_is_false(\"false\").");
    Clause answer4 = parseClause(
        "hasDifferentNumberOfItems(\"blue_bag\", \"red_bag\") :- bagItems(\"blue_bag\", \"2\"), bagItems(\"red_bag\", \"1\"), fn_eq(\"false\", \"2\", \"1\"), fn_is_false(\"false\").");

    @com.google.errorprone.annotations.Var int count = checkAnswers(proofs, Sets.newHashSet(answer1)) ? 1 : 0;
    count += checkAnswers(proofs, Sets.newHashSet(answer2)) ? 1 : 0;
    count += checkAnswers(proofs, Sets.newHashSet(answer3)) ? 1 : 0;
    count += checkAnswers(proofs, Sets.newHashSet(answer4)) ? 1 : 0;

    Assert.assertEquals(2, count);
  }

  @Test
  public void testSampleOfSize3() {

    // Create kb
    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();

    // Init kb with facts
    kb.azzert(parseClause("bagItems(\"red_bag\", 1)."));
    kb.azzert(parseClause("bagItems(\"green_bag\", 2)."));
    kb.azzert(parseClause("bagItems(\"blue_bag\", 2)."));

    // Init kb with rules
    kb.azzert(parseClause(
        "hasDifferentNumberOfItems(X, Y) :- bagItems(X, A), bagItems(Y, B), fn_eq(U, A, B), fn_is_false(U)."));

    // Query kb
    // hasDifferentNumberOfItems(X, Y)?
    Solver solver = new Solver(kb, true);
    Literal query = new Literal("hasDifferentNumberOfItems", newVar(), newVar());
    Set<Clause> answers = Sets.newHashSet(solver.solve(query, 3));
    Set<Clause> proofs = solver.proofs(query);
    Map<Literal, Trie<Literal>> tries = solver.tries(query);

    // Verify answers
    Assert.assertEquals(3, proofs.size());
    Assert.assertEquals(3, answers.size());
    Assert.assertEquals(3, tries.size());

    Clause answer1 = parseClause(
        "hasDifferentNumberOfItems(\"green_bag\", \"red_bag\") :- bagItems(\"green_bag\", \"2\"), bagItems(\"red_bag\", \"1\"), fn_eq(\"false\", \"2\", \"1\"), fn_is_false(\"false\").");
    Clause answer2 = parseClause(
        "hasDifferentNumberOfItems(\"red_bag\", \"blue_bag\") :- bagItems(\"red_bag\", \"1\"), bagItems(\"blue_bag\", \"2\"), fn_eq(\"false\", \"1\", \"2\"), fn_is_false(\"false\").");
    Clause answer3 = parseClause(
        "hasDifferentNumberOfItems(\"red_bag\", \"green_bag\") :- bagItems(\"red_bag\", \"1\"), bagItems(\"green_bag\", \"2\"), fn_eq(\"false\", \"1\", \"2\"), fn_is_false(\"false\").");
    Clause answer4 = parseClause(
        "hasDifferentNumberOfItems(\"blue_bag\", \"red_bag\") :- bagItems(\"blue_bag\", \"2\"), bagItems(\"red_bag\", \"1\"), fn_eq(\"false\", \"2\", \"1\"), fn_is_false(\"false\").");

    @com.google.errorprone.annotations.Var int count = checkAnswers(proofs, Sets.newHashSet(answer1)) ? 1 : 0;
    count += checkAnswers(proofs, Sets.newHashSet(answer2)) ? 1 : 0;
    count += checkAnswers(proofs, Sets.newHashSet(answer3)) ? 1 : 0;
    count += checkAnswers(proofs, Sets.newHashSet(answer4)) ? 1 : 0;

    Assert.assertEquals(3, count);
  }

  @Test
  public void testSampleOfSize4() {

    // Create kb
    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();

    // Init kb with facts
    kb.azzert(parseClause("bagItems(\"red_bag\", 1)."));
    kb.azzert(parseClause("bagItems(\"green_bag\", 2)."));
    kb.azzert(parseClause("bagItems(\"blue_bag\", 2)."));

    // Init kb with rules
    kb.azzert(parseClause(
        "hasDifferentNumberOfItems(X, Y) :- bagItems(X, A), bagItems(Y, B), fn_eq(U, A, B), fn_is_false(U)."));

    // Query kb
    // hasDifferentNumberOfItems(X, Y)?
    Solver solver = new Solver(kb, true);
    Literal query = new Literal("hasDifferentNumberOfItems", newVar(), newVar());
    Set<Clause> answers = Sets.newHashSet(solver.solve(query, 4));
    Set<Clause> proofs = solver.proofs(query);
    Map<Literal, Trie<Literal>> tries = solver.tries(query);

    // Verify answers
    Assert.assertEquals(4, proofs.size());
    Assert.assertEquals(4, answers.size());
    Assert.assertEquals(4, tries.size());

    Clause answer1 = parseClause(
        "hasDifferentNumberOfItems(\"green_bag\", \"red_bag\") :- bagItems(\"green_bag\", \"2\"), bagItems(\"red_bag\", \"1\"), fn_eq(\"false\", \"2\", \"1\"), fn_is_false(\"false\").");
    Clause answer2 = parseClause(
        "hasDifferentNumberOfItems(\"red_bag\", \"blue_bag\") :- bagItems(\"red_bag\", \"1\"), bagItems(\"blue_bag\", \"2\"), fn_eq(\"false\", \"1\", \"2\"), fn_is_false(\"false\").");
    Clause answer3 = parseClause(
        "hasDifferentNumberOfItems(\"red_bag\", \"green_bag\") :- bagItems(\"red_bag\", \"1\"), bagItems(\"green_bag\", \"2\"), fn_eq(\"false\", \"1\", \"2\"), fn_is_false(\"false\").");
    Clause answer4 = parseClause(
        "hasDifferentNumberOfItems(\"blue_bag\", \"red_bag\") :- bagItems(\"blue_bag\", \"2\"), bagItems(\"red_bag\", \"1\"), fn_eq(\"false\", \"2\", \"1\"), fn_is_false(\"false\").");

    @com.google.errorprone.annotations.Var int count = checkAnswers(proofs, Sets.newHashSet(answer1)) ? 1 : 0;
    count += checkAnswers(proofs, Sets.newHashSet(answer2)) ? 1 : 0;
    count += checkAnswers(proofs, Sets.newHashSet(answer3)) ? 1 : 0;
    count += checkAnswers(proofs, Sets.newHashSet(answer4)) ? 1 : 0;

    Assert.assertEquals(4, count);
    Assert.assertTrue(checkProofs(tries, Sets.newHashSet(answer1, answer2, answer3, answer4)));
  }

  @Test
  public void testSampleOfSize5() {

    // Create kb
    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();

    // Init kb with facts
    kb.azzert(parseClause("bagItems(\"red_bag\", 1)."));
    kb.azzert(parseClause("bagItems(\"green_bag\", 2)."));
    kb.azzert(parseClause("bagItems(\"blue_bag\", 2)."));

    // Init kb with rules
    kb.azzert(parseClause(
        "hasDifferentNumberOfItems(X, Y) :- bagItems(X, A), bagItems(Y, B), fn_eq(U, A, B), fn_is_false(U)."));

    // Query kb
    // hasDifferentNumberOfItems(X, Y)?
    Solver solver = new Solver(kb, true);
    Literal query = new Literal("hasDifferentNumberOfItems", newVar(), newVar());
    Set<Clause> answers = Sets.newHashSet(solver.solve(query, 5));
    Set<Clause> proofs = solver.proofs(query);
    Map<Literal, Trie<Literal>> tries = solver.tries(query);

    // Verify answers
    Assert.assertEquals(4, proofs.size());
    Assert.assertEquals(4, answers.size());
    Assert.assertEquals(4, tries.size());

    Clause answer1 = parseClause(
        "hasDifferentNumberOfItems(\"green_bag\", \"red_bag\") :- bagItems(\"green_bag\", \"2\"), bagItems(\"red_bag\", \"1\"), fn_eq(\"false\", \"2\", \"1\"), fn_is_false(\"false\").");
    Clause answer2 = parseClause(
        "hasDifferentNumberOfItems(\"red_bag\", \"blue_bag\") :- bagItems(\"red_bag\", \"1\"), bagItems(\"blue_bag\", \"2\"), fn_eq(\"false\", \"1\", \"2\"), fn_is_false(\"false\").");
    Clause answer3 = parseClause(
        "hasDifferentNumberOfItems(\"red_bag\", \"green_bag\") :- bagItems(\"red_bag\", \"1\"), bagItems(\"green_bag\", \"2\"), fn_eq(\"false\", \"1\", \"2\"), fn_is_false(\"false\").");
    Clause answer4 = parseClause(
        "hasDifferentNumberOfItems(\"blue_bag\", \"red_bag\") :- bagItems(\"blue_bag\", \"2\"), bagItems(\"red_bag\", \"1\"), fn_eq(\"false\", \"2\", \"1\"), fn_is_false(\"false\").");

    @com.google.errorprone.annotations.Var int count = checkAnswers(proofs, Sets.newHashSet(answer1)) ? 1 : 0;
    count += checkAnswers(proofs, Sets.newHashSet(answer2)) ? 1 : 0;
    count += checkAnswers(proofs, Sets.newHashSet(answer3)) ? 1 : 0;
    count += checkAnswers(proofs, Sets.newHashSet(answer4)) ? 1 : 0;

    Assert.assertEquals(4, count);
    Assert.assertTrue(checkProofs(tries, Sets.newHashSet(answer1, answer2, answer3, answer4)));
  }

  @Test
  public void testDoubleNegation() {

    // Create kb
    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();

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
    kb.azzert(parseClause("match(X, Y) :- isBoyNotGirl(X), ~isGirlNotBoy(X), isGirlNotBoy(Y), ~isBoyNotGirl(Y)."));

    // Query kb
    // match(X, Y)?
    Solver solver = new Solver(kb, true);
    Literal query = new Literal("match", newVar(), newVar());
    Set<Clause> proofs = solver.proofs(query);
    Set<Clause> answers = Sets.newHashSet(solver.solve(query));
    Map<Literal, Trie<Literal>> tries = solver.tries(query);

    // Verify answers
    Assert.assertEquals(1, proofs.size());
    Assert.assertEquals(1, answers.size());
    Assert.assertEquals(1, tries.size());

    Clause answer = parseClause(
        "match(b, c) :- boy(b), ~girl(b), ~isGirl(b), ~isGirlNotBoy(b), girl(c), ~boy(c), ~isBoy(c), ~isBoyNotGirl(c).");

    Assert.assertTrue(checkAnswers(answers, Sets.newHashSet(answer)));
    Assert.assertTrue(checkProofs(tries, Sets.newHashSet(answer)));
  }

  /**
   * Non-ground
   * <p>
   * Description: negation on non-ground probabilistic facts are forbidden.
   * <p>
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
    Solver solver = new Solver(kb, true);
    Literal query = new Literal("a", newVar(), newVar());
    Set<Clause> proofs = solver.proofs(query);
  }

  /**
   * Variable unification in query
   * <p>
   * See https://github.com/ML-KULeuven/problog/blob/master/test/query_same.pl
   */
  @Test
  public void testVariableUnificationInQuery() {

    // Create kb
    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();

    // Init kb with facts
    kb.azzert(parseClause("a(2, 3)."));
    kb.azzert(parseClause("a(1, 1)."));

    // Init kb with rules
    kb.azzert(parseClause("p(X) :- a(X, X)."));

    // Query kb
    // a(X, X)?
    // p(X)?
    Solver solver = new Solver(kb, true);
    Var x = newVar();
    Literal query1 = new Literal("a", x, x);
    Set<Clause> answers1 = Sets.newHashSet(solver.solve(query1));
    Set<Clause> proofs1 = solver.proofs(query1);
    Map<Literal, Trie<Literal>> tries1 = solver.tries(query1);

    Literal query2 = new Literal("p", x);
    Set<Clause> answers2 = Sets.newHashSet(solver.solve(query2));
    Set<Clause> proofs2 = solver.proofs(query2);
    Map<Literal, Trie<Literal>> tries2 = solver.tries(query2);

    // Verify answers
    // a(1,1).
    // p(1) :- a(1, 1).
    Assert.assertEquals(1, proofs1.size());
    Assert.assertEquals(1, tries1.size());
    Assert.assertEquals(1, proofs2.size());
    Assert.assertEquals(1, tries2.size());

    Clause fact = parseClause("a(1, 1).");
    Clause answer = parseClause("p(1) :- a(1, 1).");

    Assert.assertTrue(checkAnswers(answers1, Sets.newHashSet(fact)));
    Assert.assertTrue(checkProofs(tries1, Sets.newHashSet(fact)));

    Assert.assertTrue(checkAnswers(answers2, Sets.newHashSet(answer)));
    Assert.assertTrue(checkProofs(tries2, Sets.newHashSet(answer)));
  }
}
