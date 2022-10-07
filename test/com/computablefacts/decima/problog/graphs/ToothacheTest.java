package com.computablefacts.decima.problog.graphs;

import static com.computablefacts.decima.problog.AbstractTerm.newConst;
import static com.computablefacts.decima.problog.Parser.parseClause;
import static com.computablefacts.decima.problog.TestUtils.buildClause;
import static com.computablefacts.decima.problog.TestUtils.checkAnswers;
import static com.computablefacts.decima.problog.TestUtils.checkProofs;

import com.computablefacts.asterix.WildcardMatcher;
import com.computablefacts.asterix.trie.Trie;
import com.computablefacts.decima.problog.Clause;
import com.computablefacts.decima.problog.InMemoryKnowledgeBase;
import com.computablefacts.decima.problog.Literal;
import com.computablefacts.decima.problog.ProbabilityEstimator;
import com.computablefacts.decima.problog.Solver;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

public class ToothacheTest {

  @Test
  public void testToothache() {

    // Create kb
    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();

    // Init kb with facts
    kb.azzert(parseClause("0.10::cavity(a)."));
    kb.azzert(parseClause("0.05::gum_disease(a)."));

    // Init kb with rules
    kb.azzert(parseClause("1.00::toothache(X) :- cavity(X), gum_disease(X)."));
    kb.azzert(parseClause("0.60::toothache(X) :- cavity(X), ~gum_disease(X)."));
    kb.azzert(parseClause("0.30::toothache(X) :- ~cavity(X), gum_disease(X)."));
    kb.azzert(parseClause("0.05::toothache(X) :- ~cavity(X), ~gum_disease(X)."));

    // Query kb
    // path(1, 6)?
    Solver solver = new Solver(kb, true);
    Literal query = new Literal("toothache", newConst("a"));
    Set<Clause> proofs = solver.proofs(query);
    Set<Clause> answers = Sets.newHashSet(solver.solve(query));
    Map<Literal, Trie<Literal>> tries = solver.tries(query);

    // Verify subgoals
    Assert.assertEquals(8, solver.nbSubgoals());

    // Verify answers
    Assert.assertEquals(4, proofs.size());
    Assert.assertEquals(1, answers.size());
    Assert.assertEquals(1, tries.size());

    Clause answer1 = buildClause("toothache(a)", Lists.newArrayList("0.9::~cavity(a)", "0.95::~gum_disease(a)"));
    Clause answer2 = buildClause("toothache(a)", Lists.newArrayList("0.1::cavity(a)", "0.05::gum_disease(a)"));
    Clause answer3 = buildClause("toothache(a)", Lists.newArrayList("0.9::~cavity(a)", "0.05::gum_disease(a)"));
    Clause answer4 = buildClause("toothache(a)", Lists.newArrayList("0.1::cavity(a)", "0.95::~gum_disease(a)"));

    Assert.assertTrue(checkAnswers(answers, Sets.newHashSet(answer1, answer2, answer3, answer4)));
    Assert.assertTrue(checkProofs(tries, Sets.newHashSet(answer1, answer2, answer3, answer4)));

    // Verify BDD answer
    // 0.11825::toothache(a).
    ProbabilityEstimator estimator = new ProbabilityEstimator(proofs);
    BigDecimal probability = estimator.probability(query, 5);

    Assert.assertEquals(0, BigDecimal.valueOf(0.11082).compareTo(probability));
  }

  @Test
  public void testExtractClausesInProofs() {

    // Create kb
    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();

    // Init kb with facts
    kb.azzert(parseClause("0.10::cavity(a)."));
    kb.azzert(parseClause("0.05::gum_disease(a)."));

    // Init kb with rules
    kb.azzert(parseClause("1.00::toothache(X) :- cavity(X), gum_disease(X)."));
    kb.azzert(parseClause("0.60::toothache(X) :- cavity(X), ~gum_disease(X)."));
    kb.azzert(parseClause("0.30::toothache(X) :- ~cavity(X), gum_disease(X)."));
    kb.azzert(parseClause("0.05::toothache(X) :- ~cavity(X), ~gum_disease(X)."));

    // Query kb
    // path(1, 6)?
    Solver solver = new Solver(kb, true);
    Literal query = new Literal("toothache", newConst("a"));
    List<String> table = solver.tableOfProofs(query);

    Assert.assertEquals(11, table.size());
    Assert.assertTrue(WildcardMatcher.match(Joiner.on("\n").join(table),
        "[fact] depth=0, 0.05::gum_disease(\"a\")\n" + "[fact] depth=0, 0.05::proba_???????(\"true\")\n"
            + "[fact] depth=0, 0.1::cavity(\"a\")\n" + "[fact] depth=0, 0.3::proba_???????(\"true\")\n"
            + "[fact] depth=0, 0.6::proba_???????(\"true\")\n" + "[fact] depth=0, 0.95::~gum_disease(\"a\")\n"
            + "[fact] depth=0, 0.9::~cavity(\"a\")\n"
            + "[rule] depth=0, toothache(\"a\") :- 0.05::gum_disease(\"a\"), 0.9::~cavity(\"a\"), 0.3::proba_???????(\"true\")\n"
            + "[rule] depth=0, toothache(\"a\") :- 0.1::cavity(\"a\"), 0.05::gum_disease(\"a\")\n"
            + "[rule] depth=0, toothache(\"a\") :- 0.1::cavity(\"a\"), 0.95::~gum_disease(\"a\"), 0.6::proba_???????(\"true\")\n"
            + "[rule] depth=0, toothache(\"a\") :- 0.9::~cavity(\"a\"), 0.95::~gum_disease(\"a\"), 0.05::proba_???????(\"true\")"));
  }
}
