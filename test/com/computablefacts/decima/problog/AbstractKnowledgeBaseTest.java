package com.computablefacts.decima.problog;

import static com.computablefacts.decima.problog.TestUtils.parseClause;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.computablefacts.decima.robdd.Pair;
import com.computablefacts.nona.Function;
import com.computablefacts.nona.helpers.Codecs;
import com.computablefacts.nona.types.BoxedType;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
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

    Assert.assertEquals(Sets.newHashSet(), Sets.newHashSet(kb.rules()));
    Assert.assertEquals(Sets.newHashSet(fact1, fact2), Sets.newHashSet(kb.facts()));
  }

  @Test
  public void testAssertRule() {

    Clause rule1 = parseClause("0.2::path(A, B) :- edge(A, B).");
    Clause rule2 = parseClause("0.2::path(A, B) :- path(A, X), edge(X, B).");

    InMemoryKnowledgeBase kb = kb();
    kb.azzert(rule1);
    kb.azzert(rule2);

    Assert.assertEquals(2, kb.nbFacts());
    Assert.assertEquals(2, kb.nbRules());

    Set<Clause> facts = Sets.newHashSet(kb.facts());
    Set<Clause> rules = Sets.newHashSet(kb.rules());

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

    Assert.assertEquals(4, kb.nbFacts());
    Assert.assertEquals(2, kb.nbRules());
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

  @Test
  public void testFnAssertJson() {

    Clause clause = Parser.parseClause(
        "json_path(\"jhWTAETz\", \"data\", \"9\", \"rawOutput\", \"[{\\u0022Modified\\u0022:\\u00222020-07-07T12:24:00\\u0022\\u002c\\u0022Published\\u0022:1594088100000\\u002c\\u0022access.authentication\\u0022:\\u0022NONE\\u0022\\u002c\\u0022access.complexity\\u0022:\\u0022LOW\\u0022\\u002c\\u0022access.vector\\u0022:\\u0022NETWORK\\u0022\\u002c\\u0022assigner\\u0022:\\u0022cve@mitre.org\\u0022\\u002c\\u0022cvss\\u0022:7.5\\u002c\\u0022cvss-time\\u0022:null\\u002c\\u0022cvss-vector\\u0022:null\\u002c\\u0022cwe\\u0022:\\u0022NVD-CWE-noinfo\\u0022\\u002c\\u0022id\\u0022:\\u0022CVE-2020-15505\\u0022\\u002c\\u0022impact.availability\\u0022:\\u0022PARTIAL\\u0022\\u002c\\u0022impact.confidentiality\\u0022:\\u0022PARTIAL\\u0022\\u002c\\u0022impact.integrity\\u0022:\\u0022PARTIAL\\u0022\\u002c\\u0022last-modified\\u0022:\\u00222020-09-18T16:15:00\\u0022\\u002c\\u0022references\\u0022:[\\u0022https:\\/\\/www.mobileiron.com\\/en\\/blog\\/mobileiron-security-updates-available\\u0022]\\u002c\\u0022summary\\u0022:\\u0022A remote code execution vulnerability in MobileIron Core & Connector versions 10.3.0.3 and earlier\\u002c 10.4.0.0\\u002c 10.4.0.1\\u002c 10.4.0.2\\u002c 10.4.0.3\\u002c 10.5.1.0\\u002c 10.5.2.0 and 10.6.0.0; and Sentry versions 9.7.2 and earlier\\u002c and 9.8.0; and Monitor and Reporting Database \\u0028RDB\\u0029 version 2.0.0.1 and earlier that allows remote attackers to execute arbitrary code via unspecified vectors.\\u0022\\u002c\\u0022vulnerable_configuration\\u0022:[\\u0022cpe:2.3:a:mobileiron:cloud:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:cloud:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:core:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:core:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:enterprise_connector:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:enterprise_connector:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:reporting_database:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:reporting_database:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:sentry:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:sentry:9.8:*:*:*:*:*:*:*\\u0022]\\u002c\\u0022vulnerable_configuration_cpe_2_2\\u0022:[]\\u002c\\u0022vulnerable_product\\u0022:[\\u0022cpe:2.3:a:mobileiron:cloud:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:cloud:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:core:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:core:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:enterprise_connector:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:enterprise_connector:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:reporting_database:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:reporting_database:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:sentry:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:sentry:9.8:*:*:*:*:*:*:*\\u0022]}\\u002c{\\u0022Modified\\u0022:\\u00222020-07-07T12:24:00\\u0022\\u002c\\u0022Published\\u0022:1594088100000\\u002c\\u0022access.authentication\\u0022:\\u0022NONE\\u0022\\u002c\\u0022access.complexity\\u0022:\\u0022LOW\\u0022\\u002c\\u0022access.vector\\u0022:\\u0022NETWORK\\u0022\\u002c\\u0022assigner\\u0022:\\u0022cve@mitre.org\\u0022\\u002c\\u0022cvss\\u0022:7.5\\u002c\\u0022cvss-time\\u0022:null\\u002c\\u0022cvss-vector\\u0022:null\\u002c\\u0022cwe\\u0022:\\u0022CWE-287\\u0022\\u002c\\u0022id\\u0022:\\u0022CVE-2020-15506\\u0022\\u002c\\u0022impact.availability\\u0022:\\u0022PARTIAL\\u0022\\u002c\\u0022impact.confidentiality\\u0022:\\u0022PARTIAL\\u0022\\u002c\\u0022impact.integrity\\u0022:\\u0022PARTIAL\\u0022\\u002c\\u0022last-modified\\u0022:\\u00222020-09-18T17:15:00\\u0022\\u002c\\u0022references\\u0022:[\\u0022https:\\/\\/www.mobileiron.com\\/en\\/blog\\/mobileiron-security-updates-available\\u0022]\\u002c\\u0022summary\\u0022:\\u0022An authentication bypass vulnerability in MobileIron Core & Connector versions 10.3.0.3 and earlier\\u002c 10.4.0.0\\u002c 10.4.0.1\\u002c 10.4.0.2\\u002c 10.4.0.3\\u002c 10.5.1.0\\u002c 10.5.2.0 and 10.6.0.0 that allows remote attackers to bypass authentication mechanisms via unspecified vectors.\\u0022\\u002c\\u0022vulnerable_configuration\\u0022:[\\u0022cpe:2.3:a:mobileiron:cloud:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:cloud:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:core:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:core:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:enterprise_connector:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:enterprise_connector:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:reporting_database:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:reporting_database:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:sentry:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:sentry:9.8:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:sentry:10.6:*:*:*:*:*:*:*\\u0022]\\u002c\\u0022vulnerable_configuration_cpe_2_2\\u0022:[]\\u002c\\u0022vulnerable_product\\u0022:[\\u0022cpe:2.3:a:mobileiron:cloud:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:cloud:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:core:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:core:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:enterprise_connector:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:enterprise_connector:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:reporting_database:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:reporting_database:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:sentry:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:sentry:9.8:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:sentry:10.6:*:*:*:*:*:*:*\\u0022]}\\u002c{\\u0022Modified\\u0022:\\u00222020-02-21T15:13:00\\u0022\\u002c\\u0022Published\\u0022:1581635700000\\u002c\\u0022access.authentication\\u0022:\\u0022NONE\\u0022\\u002c\\u0022access.complexity\\u0022:\\u0022LOW\\u0022\\u002c\\u0022access.vector\\u0022:\\u0022NETWORK\\u0022\\u002c\\u0022assigner\\u0022:\\u0022cve@mitre.org\\u0022\\u002c\\u0022cvss\\u0022:10.0\\u002c\\u0022cvss-time\\u0022:\\u00222020-02-21T15:13:00\\u0022\\u002c\\u0022cvss-vector\\u0022:\\u0022AV:N\\/AC:L\\/Au:N\\/C:C\\/I:C\\/A:C\\u0022\\u002c\\u0022cwe\\u0022:\\u0022CWE-326\\u0022\\u002c\\u0022id\\u0022:\\u0022CVE-2013-7287\\u0022\\u002c\\u0022impact.availability\\u0022:\\u0022COMPLETE\\u0022\\u002c\\u0022impact.confidentiality\\u0022:\\u0022COMPLETE\\u0022\\u002c\\u0022impact.integrity\\u0022:\\u0022COMPLETE\\u0022\\u002c\\u0022last-modified\\u0022:null\\u002c\\u0022references\\u0022:[\\u0022http:\\/\\/seclists.org\\/fulldisclosure\\/2014\\/Apr\\/21\\u0022\\u002c\\u0022https:\\/\\/www.securityfocus.com\\/archive\\/1\\/531713\\u0022]\\u002c\\u0022summary\\u0022:\\u0022MobileIron VSP < 5.9.1 and Sentry < 5.0 has an insecure encryption scheme.\\u0022\\u002c\\u0022vulnerable_configuration\\u0022:[\\u0022cpe:2.3:a:mobileiron:sentry:*:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:virtual_smartphone_platform:*:*:*:*:*:*:*:*\\u0022]\\u002c\\u0022vulnerable_configuration_cpe_2_2\\u0022:[]\\u002c\\u0022vulnerable_product\\u0022:[\\u0022cpe:2.3:a:mobileiron:sentry:*:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:virtual_smartphone_platform:*:*:*:*:*:*:*:*\\u0022]}]\").");
    Clause rule = Parser.parseClause(
        "assert(X) :- json_path(X, _, _, _, RawOutput), fn_assert_json(IsOk, fn_concat(X, \"-cRz86jrY\"), fn_to_json(RawOutput)), fn_is_true(IsOk).");

    AbstractKnowledgeBase kb = kb();
    kb.azzert(clause);
    kb.azzert(rule);

    Solver solver = new Solver(kb);
    @com.google.errorprone.annotations.Var
    Set<Clause> clauses = Sets.newHashSet(solver.solve(Parser.parseQuery("assert(\"jhWTAETz\")?")));

    Assert.assertEquals(1, clauses.size());
    Assert.assertEquals(1, kb.nbRules());
    Assert.assertEquals(105, kb.nbFacts());
    Assert.assertEquals(3, kb.nbFacts(new Literal("json", new Var(), new Var(), new Var())));
    Assert.assertEquals(51,
        kb.nbFacts(new Literal("json_path", new Var(), new Var(), new Var(), new Var())));
    Assert.assertEquals(51, kb.nbFacts(new Literal("json_path",
        Lists.newArrayList(new Var(), new Var(), new Var(), new Var(), new Var()))));

    // Here, the KB has been augmented with the facts generated by the assert(X) rule
    clauses = Sets.newHashSet(
        solver.solve(Parser.parseQuery("json_path(\"jhWTAETz-cRz86jrY\", _, \"id\", _)?")));

    Assert.assertEquals(3, clauses.size());
    Assert.assertTrue(clauses.contains(Parser
        .parseClause("json_path(\"jhWTAETz-cRz86jrY\", \"0\", \"id\", \"CVE-2020-15505\").")));
    Assert.assertTrue(clauses.contains(Parser
        .parseClause("json_path(\"jhWTAETz-cRz86jrY\", \"1\", \"id\", \"CVE-2020-15506\").")));
    Assert.assertTrue(clauses.contains(
        Parser.parseClause("json_path(\"jhWTAETz-cRz86jrY\", \"2\", \"id\", \"CVE-2013-7287\").")));
  }

  @Test
  public void testFnAssertCsv() {

    Clause clause = Parser.parseClause(
        "json_path(\"aIMuk3ze\", \"data\", \"3\", \"rawOutput\", \"FUZZ\\u002curl\\u002credirectlocation\\u002cposition\\u002cstatus_code\\u002ccontent_length\\u002ccontent_words\\u002ccontent_lines\\u002cresultfile\\nadmin/\\u002chttps://www.example.com:443/admin/\\u002c\\u002c438\\u002c200\\u002c7266\\u002c2275\\u002c152\\u002c\\n\").");
    Clause rule = Parser.parseClause(
        "assert(X) :- json_path(X, _, _, _, RawOutput), fn_assert_csv(IsOk, fn_concat(X, \"-cRz86jrY\"), fn_to_csv(RawOutput)), fn_is_true(IsOk).");

    AbstractKnowledgeBase kb = kb();
    kb.azzert(clause);
    kb.azzert(rule);

    Solver solver = new Solver(kb);
    @com.google.errorprone.annotations.Var
    Set<Clause> clauses = Sets.newHashSet(solver.solve(Parser.parseQuery("assert(\"aIMuk3ze\")?")));

    Assert.assertEquals(1, clauses.size());
    Assert.assertEquals(1, kb.nbRules());
    Assert.assertEquals(11, kb.nbFacts());
    Assert.assertEquals(1, kb.nbFacts(new Literal("json", new Var(), new Var(), new Var())));
    Assert.assertEquals(9,
        kb.nbFacts(new Literal("json_path", new Var(), new Var(), new Var(), new Var())));
    Assert.assertEquals(1, kb.nbFacts(new Literal("json_path",
        Lists.newArrayList(new Var(), new Var(), new Var(), new Var(), new Var()))));

    // Here, the KB has been augmented with the facts generated by the assert(X) rule
    clauses = Sets.newHashSet(
        solver.solve(Parser.parseQuery("json_path(\"aIMuk3ze-cRz86jrY\", _, \"FUZZ\", _)?")));

    Assert.assertEquals(1, clauses.size());
    Assert.assertTrue(clauses.contains(
        Parser.parseClause("json_path(\"aIMuk3ze-cRz86jrY\", \"0\", \"FUZZ\", \"admin/\").")));
  }

  @Test
  public void testExistInKb() {

    Clause clause = Parser.parseClause(
        "json_path(\"jhWTAETz\", \"data\", \"9\", \"rawOutput\", \"[{\\u0022Modified\\u0022:\\u00222020-07-07T12:24:00\\u0022\\u002c\\u0022Published\\u0022:1594088100000\\u002c\\u0022access.authentication\\u0022:\\u0022NONE\\u0022\\u002c\\u0022access.complexity\\u0022:\\u0022LOW\\u0022\\u002c\\u0022access.vector\\u0022:\\u0022NETWORK\\u0022\\u002c\\u0022assigner\\u0022:\\u0022cve@mitre.org\\u0022\\u002c\\u0022cvss\\u0022:7.5\\u002c\\u0022cvss-time\\u0022:null\\u002c\\u0022cvss-vector\\u0022:null\\u002c\\u0022cwe\\u0022:\\u0022NVD-CWE-noinfo\\u0022\\u002c\\u0022id\\u0022:\\u0022CVE-2020-15505\\u0022\\u002c\\u0022impact.availability\\u0022:\\u0022PARTIAL\\u0022\\u002c\\u0022impact.confidentiality\\u0022:\\u0022PARTIAL\\u0022\\u002c\\u0022impact.integrity\\u0022:\\u0022PARTIAL\\u0022\\u002c\\u0022last-modified\\u0022:\\u00222020-09-18T16:15:00\\u0022\\u002c\\u0022references\\u0022:[\\u0022https:\\/\\/www.mobileiron.com\\/en\\/blog\\/mobileiron-security-updates-available\\u0022]\\u002c\\u0022summary\\u0022:\\u0022A remote code execution vulnerability in MobileIron Core & Connector versions 10.3.0.3 and earlier\\u002c 10.4.0.0\\u002c 10.4.0.1\\u002c 10.4.0.2\\u002c 10.4.0.3\\u002c 10.5.1.0\\u002c 10.5.2.0 and 10.6.0.0; and Sentry versions 9.7.2 and earlier\\u002c and 9.8.0; and Monitor and Reporting Database \\u0028RDB\\u0029 version 2.0.0.1 and earlier that allows remote attackers to execute arbitrary code via unspecified vectors.\\u0022\\u002c\\u0022vulnerable_configuration\\u0022:[\\u0022cpe:2.3:a:mobileiron:cloud:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:cloud:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:core:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:core:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:enterprise_connector:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:enterprise_connector:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:reporting_database:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:reporting_database:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:sentry:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:sentry:9.8:*:*:*:*:*:*:*\\u0022]\\u002c\\u0022vulnerable_configuration_cpe_2_2\\u0022:[]\\u002c\\u0022vulnerable_product\\u0022:[\\u0022cpe:2.3:a:mobileiron:cloud:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:cloud:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:core:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:core:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:enterprise_connector:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:enterprise_connector:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:reporting_database:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:reporting_database:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:sentry:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:sentry:9.8:*:*:*:*:*:*:*\\u0022]}\\u002c{\\u0022Modified\\u0022:\\u00222020-07-07T12:24:00\\u0022\\u002c\\u0022Published\\u0022:1594088100000\\u002c\\u0022access.authentication\\u0022:\\u0022NONE\\u0022\\u002c\\u0022access.complexity\\u0022:\\u0022LOW\\u0022\\u002c\\u0022access.vector\\u0022:\\u0022NETWORK\\u0022\\u002c\\u0022assigner\\u0022:\\u0022cve@mitre.org\\u0022\\u002c\\u0022cvss\\u0022:7.5\\u002c\\u0022cvss-time\\u0022:null\\u002c\\u0022cvss-vector\\u0022:null\\u002c\\u0022cwe\\u0022:\\u0022CWE-287\\u0022\\u002c\\u0022id\\u0022:\\u0022CVE-2020-15506\\u0022\\u002c\\u0022impact.availability\\u0022:\\u0022PARTIAL\\u0022\\u002c\\u0022impact.confidentiality\\u0022:\\u0022PARTIAL\\u0022\\u002c\\u0022impact.integrity\\u0022:\\u0022PARTIAL\\u0022\\u002c\\u0022last-modified\\u0022:\\u00222020-09-18T17:15:00\\u0022\\u002c\\u0022references\\u0022:[\\u0022https:\\/\\/www.mobileiron.com\\/en\\/blog\\/mobileiron-security-updates-available\\u0022]\\u002c\\u0022summary\\u0022:\\u0022An authentication bypass vulnerability in MobileIron Core & Connector versions 10.3.0.3 and earlier\\u002c 10.4.0.0\\u002c 10.4.0.1\\u002c 10.4.0.2\\u002c 10.4.0.3\\u002c 10.5.1.0\\u002c 10.5.2.0 and 10.6.0.0 that allows remote attackers to bypass authentication mechanisms via unspecified vectors.\\u0022\\u002c\\u0022vulnerable_configuration\\u0022:[\\u0022cpe:2.3:a:mobileiron:cloud:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:cloud:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:core:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:core:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:enterprise_connector:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:enterprise_connector:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:reporting_database:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:reporting_database:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:sentry:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:sentry:9.8:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:sentry:10.6:*:*:*:*:*:*:*\\u0022]\\u002c\\u0022vulnerable_configuration_cpe_2_2\\u0022:[]\\u002c\\u0022vulnerable_product\\u0022:[\\u0022cpe:2.3:a:mobileiron:cloud:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:cloud:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:core:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:core:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:enterprise_connector:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:enterprise_connector:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:reporting_database:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:reporting_database:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:sentry:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:sentry:9.8:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:sentry:10.6:*:*:*:*:*:*:*\\u0022]}\\u002c{\\u0022Modified\\u0022:\\u00222020-02-21T15:13:00\\u0022\\u002c\\u0022Published\\u0022:1581635700000\\u002c\\u0022access.authentication\\u0022:\\u0022NONE\\u0022\\u002c\\u0022access.complexity\\u0022:\\u0022LOW\\u0022\\u002c\\u0022access.vector\\u0022:\\u0022NETWORK\\u0022\\u002c\\u0022assigner\\u0022:\\u0022cve@mitre.org\\u0022\\u002c\\u0022cvss\\u0022:10.0\\u002c\\u0022cvss-time\\u0022:\\u00222020-02-21T15:13:00\\u0022\\u002c\\u0022cvss-vector\\u0022:\\u0022AV:N\\/AC:L\\/Au:N\\/C:C\\/I:C\\/A:C\\u0022\\u002c\\u0022cwe\\u0022:\\u0022CWE-326\\u0022\\u002c\\u0022id\\u0022:\\u0022CVE-2013-7287\\u0022\\u002c\\u0022impact.availability\\u0022:\\u0022COMPLETE\\u0022\\u002c\\u0022impact.confidentiality\\u0022:\\u0022COMPLETE\\u0022\\u002c\\u0022impact.integrity\\u0022:\\u0022COMPLETE\\u0022\\u002c\\u0022last-modified\\u0022:null\\u002c\\u0022references\\u0022:[\\u0022http:\\/\\/seclists.org\\/fulldisclosure\\/2014\\/Apr\\/21\\u0022\\u002c\\u0022https:\\/\\/www.securityfocus.com\\/archive\\/1\\/531713\\u0022]\\u002c\\u0022summary\\u0022:\\u0022MobileIron VSP < 5.9.1 and Sentry < 5.0 has an insecure encryption scheme.\\u0022\\u002c\\u0022vulnerable_configuration\\u0022:[\\u0022cpe:2.3:a:mobileiron:sentry:*:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:virtual_smartphone_platform:*:*:*:*:*:*:*:*\\u0022]\\u002c\\u0022vulnerable_configuration_cpe_2_2\\u0022:[]\\u002c\\u0022vulnerable_product\\u0022:[\\u0022cpe:2.3:a:mobileiron:sentry:*:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:virtual_smartphone_platform:*:*:*:*:*:*:*:*\\u0022]}]\").");
    Clause rule1 = Parser.parseClause(
        "assert(X) :- json_path(X, _, _, _, RawOutput), fn_assert_json(IsOk, fn_concat(X, \"-cRz86jrY\"), fn_to_json(RawOutput)), fn_is_true(IsOk).");
    Clause rule2 = Parser.parseClause(
        "exist_in_kb(X) :- fn_exist_in_kb(IsOk, \"json_path\", \"_\", \"_\", \"id\", X), fn_is_true(IsOk).");

    AbstractKnowledgeBase kb = kb();
    kb.azzert(clause);
    kb.azzert(rule1);
    kb.azzert(rule2);

    Literal query1 = Parser.parseQuery("exist_in_kb(\"CVE-2020-15505\")?");
    Literal query2 = Parser.parseQuery("exist_in_kb(\"CVE-2020-15506\")?");
    Literal query3 = Parser.parseQuery("exist_in_kb(\"CVE-2013-7287\")?");

    // First test : queries must fail while assert(X) has not been called
    @com.google.errorprone.annotations.Var
    Solver solver = new Solver(kb);

    Assert.assertEquals(0, Sets.newHashSet(solver.solve(query1)).size());
    Assert.assertEquals(0, Sets.newHashSet(solver.solve(query2)).size());
    Assert.assertEquals(0, Sets.newHashSet(solver.solve(query3)).size());

    @com.google.errorprone.annotations.Var
    Set<Clause> clauses = Sets.newHashSet(solver.solve(Parser.parseQuery("assert(\"jhWTAETz\")?")));

    Assert.assertEquals(1, clauses.size());
    Assert.assertEquals(2, kb.nbRules());
    Assert.assertEquals(105, kb.nbFacts());
    Assert.assertEquals(3, kb.nbFacts(new Literal("json", new Var(), new Var(), new Var())));
    Assert.assertEquals(51,
        kb.nbFacts(new Literal("json_path", new Var(), new Var(), new Var(), new Var())));
    Assert.assertEquals(51, kb.nbFacts(new Literal("json_path",
        Lists.newArrayList(new Var(), new Var(), new Var(), new Var(), new Var()))));

    // Second test : queries must fail while the solver's subgoals are cached
    Assert.assertEquals(0, Sets.newHashSet(solver.solve(query1)).size());
    Assert.assertEquals(0, Sets.newHashSet(solver.solve(query2)).size());
    Assert.assertEquals(0, Sets.newHashSet(solver.solve(query3)).size());

    // Third test : queries must succeed after the solver's subgoals are removed
    solver = new Solver(kb);

    Assert.assertEquals(1, Sets.newHashSet(solver.solve(query1)).size());
    Assert.assertEquals(1, Sets.newHashSet(solver.solve(query2)).size());
    Assert.assertEquals(1, Sets.newHashSet(solver.solve(query3)).size());
  }

  @Test
  public void testMockMaterializeFactsQueryWithoutFixedTerms() {

    // Dataset CRM1 -> 2 clients
    String rule1 =
        "clients(FirstName, LastName, Email) :- fn_mock_materialize_facts_query(\"http://localhost:3000/crm1\", \"first_name\", FirstName, \"last_name\", LastName, \"email\", Email).";

    // Dataset CRM2 -> 3 clients + 1 duplicate of CRM1
    String rule2 =
        "clients(FirstName, LastName, Email) :- fn_mock_materialize_facts_query(\"http://localhost:3000/crm2\", \"first_name\", FirstName, \"last_name\", LastName, \"email\", Email).";

    AbstractKnowledgeBase kb = addMockMaterializeFactsQueryDefinition1(kb());
    kb.azzert(Parser.parseClause(rule1));
    kb.azzert(Parser.parseClause(rule2));

    Solver solver = new Solver(kb);
    Set<Clause> clauses =
        Sets.newHashSet(solver.solve(Parser.parseQuery("clients(FirstName, LastName, Email)?")));

    Assert.assertEquals(5, clauses.size());
    Assert.assertTrue(clauses.contains(
        Parser.parseClause("clients(\"Robert\", \"Brown\", \"bobbrown432@yahoo.com\").")));
    Assert.assertTrue(clauses
        .contains(Parser.parseClause("clients(\"Lucy\", \"Ballmer\", \"lucyb56@gmail.com\").")));
    Assert.assertTrue(clauses.contains(
        Parser.parseClause("clients(\"Roger\", \"Bacon\", \"rogerbacon12@yahoo.com\").")));
    Assert.assertTrue(clauses
        .contains(Parser.parseClause("clients(\"Robert\", \"Schwartz\", \"rob23@gmail.com\").")));
    Assert.assertTrue(clauses
        .contains(Parser.parseClause("clients(\"Anna\", \"Smith\", \"annasmith23@gmail.com\").")));
  }

  @Test
  public void testMockMaterializeFactsQueryWithFixedTerms() {

    // Dataset CRM1 -> 2 clients
    String rule1 =
        "clients(FirstName, LastName, Email) :- fn_mock_materialize_facts_query(\"http://localhost:3000/crm1\", \"first_name\", FirstName, \"last_name\", LastName, \"email\", Email).";

    // Dataset CRM2 -> 3 clients + 1 duplicate of CRM1
    String rule2 =
        "clients(FirstName, LastName, Email) :- fn_mock_materialize_facts_query(\"http://localhost:3000/crm2\", \"first_name\", FirstName, \"last_name\", LastName, \"email\", Email).";

    AbstractKnowledgeBase kb = addMockMaterializeFactsQueryDefinition1(kb());
    kb.azzert(Parser.parseClause(rule1));
    kb.azzert(Parser.parseClause(rule2));

    Solver solver = new Solver(kb);
    Set<Clause> clauses =
        Sets.newHashSet(solver.solve(Parser.parseQuery("clients(\"Robert\", LastName, Email)?")));

    Assert.assertEquals(2, clauses.size());
    Assert.assertTrue(clauses.contains(
        Parser.parseClause("clients(\"Robert\", \"Brown\", \"bobbrown432@yahoo.com\").")));
    Assert.assertTrue(clauses
        .contains(Parser.parseClause("clients(\"Robert\", \"Schwartz\", \"rob23@gmail.com\").")));
  }

  @Test
  public void testMockMaterializeFactsQueryWithWildcardFilter() {

    String rule1 =
        "fichier(PATH, MD5) :- fn_mock_materialize_facts_query(\"https://localhost/facts/dab/fichier\", \"metadata.path\", _, PATH, \"metadata.md5_after\", _, MD5).";
    String rule2 =
        "mes_fichiers_favoris(PATH, MD5) :- fn_mock_materialize_facts_query(\"https://localhost/facts/dab/fichier\", \"metadata.path\", _, PATH, \"metadata.md5_after\", \"824a*\", MD5).";

    AbstractKnowledgeBase kb = addMockMaterializeFactsQueryDefinition2(kb());
    kb.azzert(Parser.parseClause(rule1));
    kb.azzert(Parser.parseClause(rule2));

    Solver solver = new Solver(kb);
    Set<Clause> clauses =
        Sets.newHashSet(solver.solve(Parser.parseQuery("mes_fichiers_favoris(PATH, MD5)?")));

    Assert.assertEquals(4, clauses.size());
    Assert.assertTrue(clauses.contains(Parser.parseClause(
        "mes_fichiers_favoris(\"/var/sftp/file1.pdf\", \"824a6d489b13f87d9006fe6842dd424b\").")));
    Assert.assertTrue(clauses.contains(Parser.parseClause(
        "mes_fichiers_favoris(\"/var/sftp/file2.pdf\", \"824afe9a2309abcf033bc74b7fe42a84\").")));
    Assert.assertTrue(clauses.contains(Parser.parseClause(
        "mes_fichiers_favoris(\"/var/sftp/file2.pdf\", \"824a6d489b13f87d9006fe6842dd424b\").")));
    Assert.assertTrue(clauses.contains(Parser.parseClause(
        "mes_fichiers_favoris(\"/var/sftp/file3.pdf\", \"824a6d489b13f87d9006fe6842dd424b\").")));
  }

  @Test
  public void testMockMaterializeFactsQueryWithCarriageReturnAndLineFeed() {

    String rule1 =
        "fichier_dab(PATH, TEXT) :- fn_mock_materialize_facts_query(\"https://localhost/facts/dab/fichier\", \"metadata.path\", _, PATH, \"content.text\", _, TEXT).";
    String rule2 =
        "fichier_vam(PATH, TEXT) :- fn_mock_materialize_facts_query(\"https://localhost/facts/vam/fichier\", \"metadata.path\", _, PATH, \"content.text\", _, TEXT).";
    String rule3 =
        "fichier_duplique(PATH, TEXT) :- fichier_dab(PATH, TEXT), fichier_vam(PATH, TEXT).";

    AbstractKnowledgeBase kb = addMockMaterializeFactsQueryDefinition3(kb());
    kb.azzert(Parser.parseClause(rule1));
    kb.azzert(Parser.parseClause(rule2));
    kb.azzert(Parser.parseClause(rule3));

    Solver solver = new Solver(kb);
    Literal query = Parser.parseQuery("fichier_duplique(PATH, TEXT)?");
    Iterator<Clause> iterator = solver.solve(query);
    Set<Clause> clauses = Sets.newHashSet(iterator);

    System.out.println("clauses : " + clauses);
    Assert.assertEquals(1, clauses.size());
  }

  private InMemoryKnowledgeBase kb() {
    return new InMemoryKnowledgeBase();
  }

  private AbstractKnowledgeBase addMockMaterializeFactsQueryDefinition1(AbstractKnowledgeBase kb) {
    kb.definitions().put("FN_MOCK_MATERIALIZE_FACTS_QUERY",
        new Function("MOCK_MATERIALIZE_FACTS_QUERY") {

          @Override
          protected boolean isCacheable() {
            return false;
          }

          @Override
          public BoxedType<?> evaluate(List<BoxedType<?>> parameters) {

            Preconditions.checkArgument(parameters.size() >= 3,
                "MOCK_MATERIALIZE_FACTS_QUERY takes at least three parameters.");
            Preconditions.checkArgument(parameters.get(0).isString(), "%s should be a string",
                parameters.get(0));

            Map<String, Object> params = new HashMap<>();

            for (int i = 1; i < parameters.size(); i = i + 2) {

              String name = parameters.get(i).asString();
              String value = "_".equals(parameters.get(i + 1).asString()) ? null
                  : parameters.get(i + 1).asString();

              params.put(name, value);
            }

            // Mock two distinct API calls
            Map<String, Object> json;
            String uri = parameters.get(0).asString();

            if (uri.equals("http://localhost:3000/crm1")) {
              json = Codecs.asObject(
                  "{\"namespace\":\"crm1\",\"class\":\"clients\",\"facts\":[{\"id\":1,\"first_name\":\"Robert\",\"last_name\":\"Schwartz\",\"email\":\"rob23@gmail.com\"},{\"id\":2,\"first_name\":\"Lucy\",\"last_name\":\"Ballmer\",\"email\":\"lucyb56@gmail.com\"}]}");
            } else if (uri.equals("http://localhost:3000/crm2")) {
              json = Codecs.asObject(
                  "{\"namespace\":\"crm2\",\"class\":\"clients\",\"facts\":[{\"id\":1,\"first_name\":\"Robert\",\"last_name\":\"Schwartz\",\"email\":\"rob23@gmail.com\"},{\"id\":3,\"first_name\":\"Anna\",\"last_name\":\"Smith\",\"email\":\"annasmith23@gmail.com\"},{\"id\":4,\"first_name\":\"Robert\",\"last_name\":\"Brown\",\"email\":\"bobbrown432@yahoo.com\"},{\"id\":5,\"first_name\":\"Roger\",\"last_name\":\"Bacon\",\"email\":\"rogerbacon12@yahoo.com\"}]}");
            } else {
              return BoxedType.empty();
            }

            // Mock server-side filtering
            json.put("facts",
                ((List<Map<String, Object>>) json.get("facts")).stream().filter(fact -> {
                  for (Map.Entry<String, Object> param : params.entrySet()) {
                    if (param.getValue() != null && fact.containsKey(param.getKey())
                        && !param.getValue().equals(fact.get(param.getKey()))) {
                      return false;
                    }
                  }
                  return true;
                }).collect(Collectors.toList()));

            // Transform API result
            List<Literal> facts =
                ((List<Map<String, Object>>) json.get("facts")).stream().map(fact -> {

                  List<AbstractTerm> terms = new ArrayList<>();
                  terms.add(new Const(parameters.get(0)));

                  for (int i = 1; i < parameters.size(); i = i + 2) {
                    String name = parameters.get(i).asString();
                    terms.add(new Const(name));
                    terms.add(new Const(fact.get(name)));
                  }
                  return new Literal("fn_" + name().toLowerCase(), terms);
                }).collect(Collectors.toList());

            if (uri.equals("http://localhost:3000/crm1")) {
              return BoxedType.create(facts.iterator()); // For tests purposes !
            }
            return BoxedType.create(facts);
          }
        });
    return kb;
  }

  private AbstractKnowledgeBase addMockMaterializeFactsQueryDefinition2(AbstractKnowledgeBase kb) {
    kb.definitions().put("FN_MOCK_MATERIALIZE_FACTS_QUERY",
        new Function("MOCK_MATERIALIZE_FACTS_QUERY") {

          @Override
          protected boolean isCacheable() {
            return false;
          }

          @Override
          public BoxedType<?> evaluate(List<BoxedType<?>> parameters) {

            List<Literal> literals = new ArrayList<>();
            literals.add(Parser.parseClause(
                "fn_mock_materialize_facts_query(\"https://localhost/facts/dab/fichier\", \"metadata.path\", \"*\", \"/var/sftp/file1.pdf\", \"metadata.md5_after\", \"824a*\", \"824a6d489b13f87d9006fe6842dd424b\").")
                .head());
            literals.add(Parser.parseClause(
                "fn_mock_materialize_facts_query(\"https://localhost/facts/dab/fichier\", \"metadata.path\", \"*\", \"/var/sftp/file2.pdf\", \"metadata.md5_after\", \"824a*\", \"824afe9a2309abcf033bc74b7fe42a84\").")
                .head());
            literals.add(Parser.parseClause(
                "fn_mock_materialize_facts_query(\"https://localhost/facts/dab/fichier\", \"metadata.path\", \"*\", \"/var/sftp/file2.pdf\", \"metadata.md5_after\", \"824a*\", \"824a6d489b13f87d9006fe6842dd424b\").")
                .head());
            literals.add(Parser.parseClause(
                "fn_mock_materialize_facts_query(\"https://localhost/facts/dab/fichier\", \"metadata.path\", \"*\", \"/var/sftp/file3.pdf\", \"metadata.md5_after\", \"824a*\", \"824a6d489b13f87d9006fe6842dd424b\").")
                .head());

            return BoxedType.create(literals);
          }
        });
    return kb;
  }

  private AbstractKnowledgeBase addMockMaterializeFactsQueryDefinition3(AbstractKnowledgeBase kb) {
    kb.definitions().put("FN_MOCK_MATERIALIZE_FACTS_QUERY",
        new Function("MOCK_MATERIALIZE_FACTS_QUERY") {

          @Override
          protected boolean isCacheable() {
            return false;
          }

          @Override
          public BoxedType<?> evaluate(List<BoxedType<?>> parameters) {

            List<Literal> literals = new ArrayList<>();

            System.out.println("parameters : " + parameters);

            String path = parameters.get(3).asString();
            String text = parameters.get(6).asString();

            if (parameters.get(0).asString().equals("https://localhost/facts/dab/fichier")) {
              if (("_".equals(path) || "/var/sftp/file1.pdf".equals(path)) && ("_".equals(text)
                  || "The quick brown fox jumps over the lazy dog".equals(text))) {
                literals.add(new Literal("fn_mock_materialize_facts_query",
                    Lists.newArrayList(new Const("https://localhost/facts/dab/fichier"),
                        new Const("metadata.path"), new Const("*"),
                        new Const("/var/sftp/file1.pdf"), new Const("content.text"), new Const("*"),
                        new Const("The quick brown fox jumps over the lazy dog"))));
              }
              if (("_".equals(path) || "/var/sftp/file2.pdf".equals(path)) && ("_".equals(text)
                  || "The quick brown fox\njumps over\r\nthe lazy dog".equals(text))) {
                literals.add(new Literal("fn_mock_materialize_facts_query",
                    Lists.newArrayList(new Const("https://localhost/facts/dab/fichier"),
                        new Const("metadata.path"), new Const("*"),
                        new Const("/var/sftp/file2.pdf"), new Const("content.text"), new Const("*"),
                        new Const("The quick brown fox\njumps over\r\nthe lazy dog"))));
              }
            } else {
              if (("_".equals(path) || "/var/sftp/file2.pdf".equals(path)) && ("_".equals(text)
                  || "The quick brown fox\njumps over\r\nthe lazy dog".equals(text))) {
                Literal literal = new Literal("fn_mock_materialize_facts_query",
                    Lists.newArrayList(new Const("https://localhost/facts/vam/fichier"),
                        new Const("metadata.path"), new Const("*"),
                        new Const("/var/sftp/file2.pdf"), new Const("content.text"), new Const("*"),
                        new Const("The quick brown fox\njumps over\r\nthe lazy dog")));
                System.out.println("literal : " + literal + ".");
                literals.add(Parser.parseClause(literal + ".").head());
              }
              if (("_".equals(path) || "/var/sftp/file3.pdf".equals(path)) && ("_".equals(text)
                  || "The quick brown fox jumps over the lazy dog".equals(text))) {
                Literal literal = new Literal("fn_mock_materialize_facts_query",
                    Lists.newArrayList(new Const("https://localhost/facts/vam/fichier"),
                        new Const("metadata.path"), new Const("*"),
                        new Const("/var/sftp/file3.pdf"), new Const("content.text"), new Const("*"),
                        new Const("The quick brown fox jumps over the lazy dog")));
                literals.add(Parser.parseClause(literal + ".").head());
              }
            }

            System.out.println("literals : " + literals);

            return BoxedType.create(literals);
          }
        });
    return kb;
  }
}
