package com.computablefacts.decima.problog;

import static com.computablefacts.decima.problog.TestUtils.parseClause;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.computablefacts.decima.robdd.Pair;
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

  private InMemoryKnowledgeBase kb() {
    return new InMemoryKnowledgeBase();
  }
}
