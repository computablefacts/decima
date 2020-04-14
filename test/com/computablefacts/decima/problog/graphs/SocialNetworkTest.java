package com.computablefacts.decima.problog.graphs;

import static com.computablefacts.decima.problog.TestUtils.isValid;
import static com.computablefacts.decima.problog.TestUtils.parseClause;
import static com.computablefacts.decima.problog.TestUtils.solver;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.computablefacts.decima.problog.Clause;
import com.computablefacts.decima.problog.Const;
import com.computablefacts.decima.problog.Estimator;
import com.computablefacts.decima.problog.InMemoryKnowledgeBase;
import com.computablefacts.decima.problog.Literal;
import com.computablefacts.decima.problog.Solver;
import com.computablefacts.decima.problog.TestUtils;

/**
 * See http://csci431.artifice.cc/notes/problog.html
 */
public class SocialNetworkTest {

  @Test
  public void test1() {

    // Query kb
    // smokes(angelika)?
    Solver solver = solver(kb());
    Literal query = new Literal("smokes", new Const("angelika"));
    Set<Clause> proofs = solver.proofs(query);

    // Verify answers
    Assert.assertEquals(2, proofs.size());
    Assert.assertTrue(isValid(proofs,
        "smokes(angelika) :- friend(angelika, jonas), person(jonas), person(angelika), person(jonas)."));
    Assert.assertTrue(isValid(proofs, "smokes(angelika) :- person(angelika)."));

    // Verify BDD answer
    // 0.342::smokes(angelika).
    BigDecimal probability = new Estimator(proofs).probability(query, 3);

    Assert.assertEquals(0, BigDecimal.valueOf(0.342).compareTo(probability));
  }

  @Test
  public void test2() {

    // Query kb
    // smokes(joris)?
    Solver solver = solver(kb());
    Literal query = new Literal("smokes", new Const("joris"));
    Set<Clause> proofs = solver.proofs(query);

    // Verify answers
    Assert.assertEquals(5, proofs.size());
    Assert.assertTrue(isValid(proofs,
        "smokes(joris) :- friend(joris, dimitar), person(dimitar), person(joris), person(dimitar)."));
    Assert.assertTrue(isValid(proofs, "smokes(joris) :- person(joris)."));
    Assert.assertTrue(isValid(proofs,
        "smokes(joris) :- friend(joris, angelika), person(angelika), person(joris), person(angelika)."));
    Assert.assertTrue(isValid(proofs,
        "smokes(joris) :- friend(joris, jonas), person(jonas), person(joris), person(jonas)."));
    Assert.assertTrue(isValid(proofs,
        "smokes(joris) :- friend(joris, angelika), person(angelika), person(joris), friend(angelika, jonas), person(jonas), person(angelika), person(jonas)."));

    // Verify BDD answer
    // 0.42301296::smokes(joris).
    Estimator estimator = new Estimator(proofs);
    BigDecimal probability = estimator.probability(query, 8);

    Assert.assertEquals(0, BigDecimal.valueOf(0.42556811).compareTo(probability));
  }

  private InMemoryKnowledgeBase kb() {

    // Create kb
    InMemoryKnowledgeBase kb = TestUtils.kb();

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
