package com.computablefacts.decima.problog;

import static com.computablefacts.decima.problog.AbstractTerm.newConst;
import static com.computablefacts.decima.problog.AbstractTerm.newVar;
import static com.computablefacts.decima.problog.Parser.parseClause;
import static com.computablefacts.decima.problog.Parser.parseQuery;

import com.computablefacts.asterix.BoxedType;
import com.computablefacts.asterix.codecs.JsonCodec;
import com.computablefacts.decima.robdd.Pair;
import com.computablefacts.nona.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
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

    String json = Parser.wrap(
        "[{\"Modified\":\"2020-07-07T12:24:00\",\"Published\":1594088100000,\"access.authentication\":\"NONE\",\"access.complexity\":\"LOW\",\"access.vector\":\"NETWORK\",\"assigner\":\"cve@mitre.org\",\"cvss\":7.5,\"cvss-time\":null,\"cvss-vector\":null,\"cwe\":\"NVD-CWE-noinfo\",\"id\":\"CVE-2020-15505\",\"impact.availability\":\"PARTIAL\",\"impact.confidentiality\":\"PARTIAL\",\"impact.integrity\":\"PARTIAL\",\"last-modified\":\"2020-09-18T16:15:00\",\"references\":[\"https:\\/\\/www.mobileiron.com\\/en\\/blog\\/mobileiron-security-updates-available\"],\"summary\":\"A remote code execution vulnerability in MobileIron Core & Connector versions 10.3.0.3 and earlier, 10.4.0.0, 10.4.0.1, 10.4.0.2, 10.4.0.3, 10.5.1.0, 10.5.2.0 and 10.6.0.0; and Sentry versions 9.7.2 and earlier, and 9.8.0; and Monitor and Reporting Database (RDB) version 2.0.0.1 and earlier that allows remote attackers to execute arbitrary code via unspecified vectors.\",\"vulnerable_configuration\":[\"cpe:2.3:a:mobileiron:cloud:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:cloud:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:core:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:core:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:enterprise_connector:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:enterprise_connector:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:reporting_database:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:reporting_database:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:9.8:*:*:*:*:*:*:*\"],\"vulnerable_configuration_cpe_2_2\":[],\"vulnerable_product\":[\"cpe:2.3:a:mobileiron:cloud:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:cloud:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:core:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:core:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:enterprise_connector:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:enterprise_connector:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:reporting_database:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:reporting_database:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:9.8:*:*:*:*:*:*:*\"]},{\"Modified\":\"2020-07-07T12:24:00\",\"Published\":1594088100000,\"access.authentication\":\"NONE\",\"access.complexity\":\"LOW\",\"access.vector\":\"NETWORK\",\"assigner\":\"cve@mitre.org\",\"cvss\":7.5,\"cvss-time\":null,\"cvss-vector\":null,\"cwe\":\"CWE-287\",\"id\":\"CVE-2020-15506\",\"impact.availability\":\"PARTIAL\",\"impact.confidentiality\":\"PARTIAL\",\"impact.integrity\":\"PARTIAL\",\"last-modified\":\"2020-09-18T17:15:00\",\"references\":[\"https:\\/\\/www.mobileiron.com\\/en\\/blog\\/mobileiron-security-updates-available\"],\"summary\":\"An authentication bypass vulnerability in MobileIron Core & Connector versions 10.3.0.3 and earlier, 10.4.0.0, 10.4.0.1, 10.4.0.2, 10.4.0.3, 10.5.1.0, 10.5.2.0 and 10.6.0.0 that allows remote attackers to bypass authentication mechanisms via unspecified vectors.\",\"vulnerable_configuration\":[\"cpe:2.3:a:mobileiron:cloud:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:cloud:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:core:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:core:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:enterprise_connector:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:enterprise_connector:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:reporting_database:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:reporting_database:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:9.8:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:10.6:*:*:*:*:*:*:*\"],\"vulnerable_configuration_cpe_2_2\":[],\"vulnerable_product\":[\"cpe:2.3:a:mobileiron:cloud:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:cloud:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:core:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:core:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:enterprise_connector:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:enterprise_connector:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:reporting_database:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:reporting_database:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:9.8:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:10.6:*:*:*:*:*:*:*\"]},{\"Modified\":\"2020-02-21T15:13:00\",\"Published\":1581635700000,\"access.authentication\":\"NONE\",\"access.complexity\":\"LOW\",\"access.vector\":\"NETWORK\",\"assigner\":\"cve@mitre.org\",\"cvss\":10.0,\"cvss-time\":\"2020-02-21T15:13:00\",\"cvss-vector\":\"AV:N\\/AC:L\\/Au:N\\/C:C\\/I:C\\/A:C\",\"cwe\":\"CWE-326\",\"id\":\"CVE-2013-7287\",\"impact.availability\":\"COMPLETE\",\"impact.confidentiality\":\"COMPLETE\",\"impact.integrity\":\"COMPLETE\",\"last-modified\":null,\"references\":[\"http:\\/\\/seclists.org\\/fulldisclosure\\/2014\\/Apr\\/21\",\"https:\\/\\/www.securityfocus.com\\/archive\\/1\\/531713\"],\"summary\":\"MobileIron VSP < 5.9.1 and Sentry < 5.0 has an insecure encryption scheme.\",\"vulnerable_configuration\":[\"cpe:2.3:a:mobileiron:sentry:*:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:virtual_smartphone_platform:*:*:*:*:*:*:*:*\"],\"vulnerable_configuration_cpe_2_2\":[],\"vulnerable_product\":[\"cpe:2.3:a:mobileiron:sentry:*:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:virtual_smartphone_platform:*:*:*:*:*:*:*:*\"]}]");
    Clause clause = parseClause(
        String.format("json_path(\"jhWTAETz\", \"data\", \"9\", \"rawOutput\", \"%s\").", json));
    Clause rule = parseClause(
        "assert(X) :- json_path(X, _, _, _, RawOutput), fn_assert_json(IsOk, fn_concat(X, \"-cRz86jrY\"), fn_to_json(RawOutput)), fn_is_true(IsOk).");

    AbstractKnowledgeBase kb = kb();
    kb.azzert(clause);
    kb.azzert(rule);

    Solver solver = new Solver(kb, false);
    @com.google.errorprone.annotations.Var Set<Clause> clauses = Sets.newHashSet(
        solver.solve(parseQuery("assert(\"jhWTAETz\")?")));

    Assert.assertEquals(2, solver.nbSubgoals());
    Assert.assertEquals(1, clauses.size());
    Assert.assertEquals(1, kb.nbRules());
    Assert.assertEquals(105, kb.nbFacts());
    Assert.assertEquals(3, kb.nbFacts(new Literal("json", newVar(), newVar(), newVar())));
    Assert.assertEquals(51, kb.nbFacts(new Literal("json_path", newVar(), newVar(), newVar(), newVar())));
    Assert.assertEquals(51,
        kb.nbFacts(new Literal("json_path", Lists.newArrayList(newVar(), newVar(), newVar(), newVar(), newVar()))));

    // Here, the KB has been augmented with the facts generated by the assert(X) rule
    clauses = Sets.newHashSet(solver.solve(parseQuery("json_path(\"jhWTAETz-cRz86jrY\", _, \"id\", _)?")));

    Assert.assertEquals(3, clauses.size());
    Assert.assertTrue(
        clauses.contains(parseClause("json_path(\"jhWTAETz-cRz86jrY\", \"0\", \"id\", \"CVE-2020-15505\").")));
    Assert.assertTrue(
        clauses.contains(parseClause("json_path(\"jhWTAETz-cRz86jrY\", \"1\", \"id\", \"CVE-2020-15506\").")));
    Assert.assertTrue(
        clauses.contains(parseClause("json_path(\"jhWTAETz-cRz86jrY\", \"2\", \"id\", \"CVE-2013-7287\").")));
  }

  @Test
  public void testFnAssertCsv() {

    String csv = Parser.wrap(
        "FUZZ,url,redirectlocation,position,status_code,content_length,content_words,content_lines,resultfile\nadmin/,https://www.example.com:443/admin/,,438,200,7266,2275,152,\n");
    Clause clause = parseClause(String.format("json_path(\"aIMuk3ze\", \"data\", \"3\", \"rawOutput\", \"%s\").", csv));
    Clause rule = parseClause(
        "assert(X) :- json_path(X, _, _, _, RawOutput), fn_assert_csv(IsOk, fn_concat(X, \"-cRz86jrY\"), fn_to_csv(RawOutput)), fn_is_true(IsOk).");

    AbstractKnowledgeBase kb = kb();
    kb.azzert(clause);
    kb.azzert(rule);

    Solver solver = new Solver(kb, false);
    @com.google.errorprone.annotations.Var Set<Clause> clauses = Sets.newHashSet(
        solver.solve(parseQuery("assert(\"aIMuk3ze\")?")));

    Assert.assertEquals(2, solver.nbSubgoals());
    Assert.assertEquals(1, clauses.size());
    Assert.assertEquals(1, kb.nbRules());
    Assert.assertEquals(11, kb.nbFacts());
    Assert.assertEquals(1, kb.nbFacts(new Literal("json", newVar(), newVar(), newVar())));
    Assert.assertEquals(9, kb.nbFacts(new Literal("json_path", newVar(), newVar(), newVar(), newVar())));
    Assert.assertEquals(1,
        kb.nbFacts(new Literal("json_path", Lists.newArrayList(newVar(), newVar(), newVar(), newVar(), newVar()))));

    // Here, the KB has been augmented with the facts generated by the assert(X) rule
    clauses = Sets.newHashSet(solver.solve(parseQuery("json_path(\"aIMuk3ze-cRz86jrY\", _, \"FUZZ\", _)?")));

    Assert.assertEquals(1, clauses.size());
    Assert.assertTrue(clauses.contains(parseClause("json_path(\"aIMuk3ze-cRz86jrY\", \"0\", \"FUZZ\", \"admin/\").")));
  }

  @Test
  public void testExistInKb() {

    String json = Parser.wrap(
        "[{\"Modified\":\"2020-07-07T12:24:00\",\"Published\":1594088100000,\"access.authentication\":\"NONE\",\"access.complexity\":\"LOW\",\"access.vector\":\"NETWORK\",\"assigner\":\"cve@mitre.org\",\"cvss\":7.5,\"cvss-time\":null,\"cvss-vector\":null,\"cwe\":\"NVD-CWE-noinfo\",\"id\":\"CVE-2020-15505\",\"impact.availability\":\"PARTIAL\",\"impact.confidentiality\":\"PARTIAL\",\"impact.integrity\":\"PARTIAL\",\"last-modified\":\"2020-09-18T16:15:00\",\"references\":[\"https:\\/\\/www.mobileiron.com\\/en\\/blog\\/mobileiron-security-updates-available\"],\"summary\":\"A remote code execution vulnerability in MobileIron Core & Connector versions 10.3.0.3 and earlier, 10.4.0.0, 10.4.0.1, 10.4.0.2, 10.4.0.3, 10.5.1.0, 10.5.2.0 and 10.6.0.0; and Sentry versions 9.7.2 and earlier, and 9.8.0; and Monitor and Reporting Database \\u0028RDB\\u0029 version 2.0.0.1 and earlier that allows remote attackers to execute arbitrary code via unspecified vectors.\",\"vulnerable_configuration\":[\"cpe:2.3:a:mobileiron:cloud:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:cloud:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:core:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:core:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:enterprise_connector:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:enterprise_connector:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:reporting_database:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:reporting_database:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:9.8:*:*:*:*:*:*:*\"],\"vulnerable_configuration_cpe_2_2\":[],\"vulnerable_product\":[\"cpe:2.3:a:mobileiron:cloud:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:cloud:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:core:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:core:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:enterprise_connector:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:enterprise_connector:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:reporting_database:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:reporting_database:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:9.8:*:*:*:*:*:*:*\"]},{\"Modified\":\"2020-07-07T12:24:00\",\"Published\":1594088100000,\"access.authentication\":\"NONE\",\"access.complexity\":\"LOW\",\"access.vector\":\"NETWORK\",\"assigner\":\"cve@mitre.org\",\"cvss\":7.5,\"cvss-time\":null,\"cvss-vector\":null,\"cwe\":\"CWE-287\",\"id\":\"CVE-2020-15506\",\"impact.availability\":\"PARTIAL\",\"impact.confidentiality\":\"PARTIAL\",\"impact.integrity\":\"PARTIAL\",\"last-modified\":\"2020-09-18T17:15:00\",\"references\":[\"https:\\/\\/www.mobileiron.com\\/en\\/blog\\/mobileiron-security-updates-available\"],\"summary\":\"An authentication bypass vulnerability in MobileIron Core & Connector versions 10.3.0.3 and earlier, 10.4.0.0, 10.4.0.1, 10.4.0.2, 10.4.0.3, 10.5.1.0, 10.5.2.0 and 10.6.0.0 that allows remote attackers to bypass authentication mechanisms via unspecified vectors.\",\"vulnerable_configuration\":[\"cpe:2.3:a:mobileiron:cloud:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:cloud:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:core:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:core:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:enterprise_connector:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:enterprise_connector:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:reporting_database:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:reporting_database:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:9.8:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:10.6:*:*:*:*:*:*:*\"],\"vulnerable_configuration_cpe_2_2\":[],\"vulnerable_product\":[\"cpe:2.3:a:mobileiron:cloud:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:cloud:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:core:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:core:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:enterprise_connector:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:enterprise_connector:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:reporting_database:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:reporting_database:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:9.8:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:10.6:*:*:*:*:*:*:*\"]},{\"Modified\":\"2020-02-21T15:13:00\",\"Published\":1581635700000,\"access.authentication\":\"NONE\",\"access.complexity\":\"LOW\",\"access.vector\":\"NETWORK\",\"assigner\":\"cve@mitre.org\",\"cvss\":10.0,\"cvss-time\":\"2020-02-21T15:13:00\",\"cvss-vector\":\"AV:N\\/AC:L\\/Au:N\\/C:C\\/I:C\\/A:C\",\"cwe\":\"CWE-326\",\"id\":\"CVE-2013-7287\",\"impact.availability\":\"COMPLETE\",\"impact.confidentiality\":\"COMPLETE\",\"impact.integrity\":\"COMPLETE\",\"last-modified\":null,\"references\":[\"http:\\/\\/seclists.org\\/fulldisclosure\\/2014\\/Apr\\/21\",\"https:\\/\\/www.securityfocus.com\\/archive\\/1\\/531713\"],\"summary\":\"MobileIron VSP < 5.9.1 and Sentry < 5.0 has an insecure encryption scheme.\",\"vulnerable_configuration\":[\"cpe:2.3:a:mobileiron:sentry:*:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:virtual_smartphone_platform:*:*:*:*:*:*:*:*\"],\"vulnerable_configuration_cpe_2_2\":[],\"vulnerable_product\":[\"cpe:2.3:a:mobileiron:sentry:*:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:virtual_smartphone_platform:*:*:*:*:*:*:*:*\"]}]");
    Clause clause = parseClause(
        String.format("json_path(\"jhWTAETz\", \"data\", \"9\", \"rawOutput\", \"%s\").", json));
    Clause rule1 = parseClause(
        "assert(X) :- json_path(X, _, _, _, RawOutput), fn_assert_json(IsOk, fn_concat(X, \"-cRz86jrY\"), fn_to_json(RawOutput)), fn_is_true(IsOk).");
    Clause rule2 = parseClause(
        "exist_in_kb(X) :- fn_exist_in_kb(IsOk, \"json_path\", \"_\", \"_\", \"id\", X), fn_is_true(IsOk).");

    AbstractKnowledgeBase kb = kb();
    kb.azzert(clause);
    kb.azzert(rule1);
    kb.azzert(rule2);

    Literal query1 = parseQuery("exist_in_kb(\"CVE-2020-15505\")?");
    Literal query2 = parseQuery("exist_in_kb(\"CVE-2020-15506\")?");
    Literal query3 = parseQuery("exist_in_kb(\"CVE-2013-7287\")?");

    // First test : queries must fail while assert(X) has not been called
    @com.google.errorprone.annotations.Var Solver solver = new Solver(kb, false);

    Assert.assertEquals(0, Sets.newHashSet(solver.solve(query1)).size());
    Assert.assertEquals(0, Sets.newHashSet(solver.solve(query2)).size());
    Assert.assertEquals(0, Sets.newHashSet(solver.solve(query3)).size());

    @com.google.errorprone.annotations.Var Set<Clause> clauses = Sets.newHashSet(
        solver.solve(parseQuery("assert(\"jhWTAETz\")?")));

    Assert.assertEquals(5, solver.nbSubgoals());
    Assert.assertEquals(1, clauses.size());
    Assert.assertEquals(2, kb.nbRules());
    Assert.assertEquals(105, kb.nbFacts());
    Assert.assertEquals(3, kb.nbFacts(new Literal("json", newVar(), newVar(), newVar())));
    Assert.assertEquals(51, kb.nbFacts(new Literal("json_path", newVar(), newVar(), newVar(), newVar())));
    Assert.assertEquals(51,
        kb.nbFacts(new Literal("json_path", Lists.newArrayList(newVar(), newVar(), newVar(), newVar(), newVar()))));

    // Second test : queries must not fail i.e. subgoals should not be cached
    Assert.assertEquals(1, Sets.newHashSet(solver.solve(query1)).size());
    Assert.assertEquals(1, Sets.newHashSet(solver.solve(query2)).size());
    Assert.assertEquals(1, Sets.newHashSet(solver.solve(query3)).size());
  }

  @Test
  public void testMockMaterializeFactsQueryWithoutFixedTerms() {

    // Dataset CRM1 -> 2 clients
    String rule1 = "clients(FirstName, LastName, Email) :- fn_mock_materialize_facts(\"http://localhost:3000/crm1\", \"first_name\", FirstName, \"last_name\", LastName, \"email\", Email).";

    // Dataset CRM2 -> 3 clients + 1 duplicate of CRM1
    String rule2 = "clients(FirstName, LastName, Email) :- fn_mock_materialize_facts(\"http://localhost:3000/crm2\", \"first_name\", FirstName, \"last_name\", LastName, \"email\", Email).";

    AbstractKnowledgeBase kb = addMockMaterializeFactsQueryDefinition1(kb());
    kb.azzert(parseClause(rule1));
    kb.azzert(parseClause(rule2));

    Solver solver = new Solver(kb, false);
    Set<Clause> clauses = Sets.newHashSet(solver.solve(parseQuery("clients(FirstName, LastName, Email)?")));

    Assert.assertEquals(1, solver.nbSubgoals());
    Assert.assertEquals(5, clauses.size());
    Assert.assertTrue(clauses.contains(parseClause("clients(\"Robert\", \"Brown\", \"bobbrown432@yahoo.com\").")));
    Assert.assertTrue(clauses.contains(parseClause("clients(\"Lucy\", \"Ballmer\", \"lucyb56@gmail.com\").")));
    Assert.assertTrue(clauses.contains(parseClause("clients(\"Roger\", \"Bacon\", \"rogerbacon12@yahoo.com\").")));
    Assert.assertTrue(clauses.contains(parseClause("clients(\"Robert\", \"Schwartz\", \"rob23@gmail.com\").")));
    Assert.assertTrue(clauses.contains(parseClause("clients(\"Anna\", \"Smith\", \"annasmith23@gmail.com\").")));
  }

  @Test
  public void testMockMaterializeFactsQueryWithFixedTerms() {

    // Dataset CRM1 -> 2 clients
    String rule1 = "clients(FirstName, LastName, Email) :- fn_mock_materialize_facts(\"http://localhost:3000/crm1\", \"first_name\", FirstName, \"last_name\", LastName, \"email\", Email).";

    // Dataset CRM2 -> 3 clients + 1 duplicate of CRM1
    String rule2 = "clients(FirstName, LastName, Email) :- fn_mock_materialize_facts(\"http://localhost:3000/crm2\", \"first_name\", FirstName, \"last_name\", LastName, \"email\", Email).";

    AbstractKnowledgeBase kb = addMockMaterializeFactsQueryDefinition1(kb());
    kb.azzert(parseClause(rule1));
    kb.azzert(parseClause(rule2));

    Solver solver = new Solver(kb, false);
    Set<Clause> clauses = Sets.newHashSet(solver.solve(parseQuery("clients(\"Robert\", LastName, Email)?")));

    Assert.assertEquals(1, solver.nbSubgoals());
    Assert.assertEquals(2, clauses.size());
    Assert.assertTrue(clauses.contains(parseClause("clients(\"Robert\", \"Brown\", \"bobbrown432@yahoo.com\").")));
    Assert.assertTrue(clauses.contains(parseClause("clients(\"Robert\", \"Schwartz\", \"rob23@gmail.com\").")));
  }

  @Test
  public void testMockMaterializeFactsQueryWithWildcardFilter() {

    String rule = "mes_fichiers_favoris(PATH, MD5) :- fn_mock_materialize_facts(\"https://localhost/facts/dab/fichier\", \"metadata.path\", _, PATH, \"metadata.md5_after\", \"824a*\", MD5).";

    AbstractKnowledgeBase kb = addMockMaterializeFactsQueryDefinition2(kb());
    kb.azzert(parseClause(rule));

    Solver solver = new Solver(kb, false);
    Literal query = parseQuery("mes_fichiers_favoris(PATH, MD5)?");
    Set<Clause> clauses = Sets.newHashSet(solver.solve(query));

    Assert.assertEquals(1, solver.nbSubgoals());
    Assert.assertEquals(4, clauses.size());
    Assert.assertTrue(clauses.contains(
        parseClause("mes_fichiers_favoris(\"/var/sftp/file1.pdf\", \"824a6d489b13f87d9006fe6842dd424b\").")));
    Assert.assertTrue(clauses.contains(
        parseClause("mes_fichiers_favoris(\"/var/sftp/file2.pdf\", \"824afe9a2309abcf033bc74b7fe42a84\").")));
    Assert.assertTrue(clauses.contains(
        parseClause("mes_fichiers_favoris(\"/var/sftp/file2.pdf\", \"824a6d489b13f87d9006fe6842dd424b\").")));
    Assert.assertTrue(clauses.contains(
        parseClause("mes_fichiers_favoris(\"/var/sftp/file3.pdf\", \"824a6d489b13f87d9006fe6842dd424b\").")));
  }

  @Test
  public void testMockMaterializeFactsQueryWithCarriageReturnAndLineFeed() {

    String rule1 = "fichier_dab(PATH, TEXT) :- fn_mock_materialize_facts(\"https://localhost/facts/dab/fichier\", \"metadata.path\", _, PATH, \"content.text\", _, TEXT).";
    String rule2 = "fichier_vam(PATH, TEXT) :- fn_mock_materialize_facts(\"https://localhost/facts/vam/fichier\", \"metadata.path\", _, PATH, \"content.text\", _, TEXT).";
    String rule3 = "fichier_duplique(PATH, TEXT) :- fichier_dab(PATH, TEXT), fichier_vam(PATH, TEXT).";

    AbstractKnowledgeBase kb = addMockMaterializeFactsQueryDefinition3(kb());
    kb.azzert(parseClause(rule1));
    kb.azzert(parseClause(rule2));
    kb.azzert(parseClause(rule3));

    Solver solver = new Solver(kb, false);
    Literal query = parseQuery("fichier_duplique(PATH, TEXT)?");
    Iterator<Clause> iterator = solver.solve(query);
    List<Clause> clauses = Lists.newArrayList(iterator);

    Assert.assertEquals(4, solver.nbSubgoals());
    Assert.assertEquals(1, clauses.size());

    Literal literal = new Literal("fichier_duplique", Lists.newArrayList(newConst("/var/sftp/file2.pdf"),
        newConst("The quick brown fox\njumps over\r\nthe lazy dog")));

    Assert.assertEquals(new Clause(literal), clauses.get(0));
  }

  @Test
  public void testMockMaterializeFactsQueryWithColonsAndEquals() {

    String rule = "mes_fichiers_favoris(PATH, CONTENT) :- fn_mock_materialize_facts(\"https://localhost/facts/dab/fichier\", \"metadata.path\", _, PATH, \"content.text\", _, CONTENT).";

    AbstractKnowledgeBase kb = addMockMaterializeFactsQueryDefinition4(kb());
    kb.azzert(parseClause(rule));

    Solver solver = new Solver(kb, false);
    Literal query = parseQuery("mes_fichiers_favoris(PATH, CONTENT)?");
    Set<Clause> clauses = Sets.newHashSet(solver.solve(query));

    Assert.assertEquals(1, solver.nbSubgoals());
    Assert.assertEquals(4, clauses.size());
    Assert.assertTrue(clauses.contains(parseClause(
        "mes_fichiers_favoris(\"/var/sftp/file1.pdf\", \"b64_(bGhzIDo9IHJocyDigJQgZnVuY3Rpb24gZXRjLiBkZWZpbml0aW9u)\").")));
    Assert.assertTrue(clauses.contains(parseClause(
        "mes_fichiers_favoris(\"/var/sftp/file2.pdf\", \"b64_(eCA9PSB2YWwg4oCUIHRlc3QgZXF1YWxpdHkgb3IgcmVwcmVzZW50IGEgc3ltYm9saWMgZXF1YXRpb24gKCHCpHUwMDNkIGZvciB1bmVxdWFsKQ==)\").")));
    Assert.assertTrue(clauses.contains(parseClause(
        "mes_fichiers_favoris(\"/var/sftp/file2.pdf\", \"b64_(bGhzIDo9IHJocyDigJQgZnVuY3Rpb24gZXRjLiBkZWZpbml0aW9u)\").")));
    Assert.assertTrue(clauses.contains(parseClause(
        "mes_fichiers_favoris(\"/var/sftp/file3.pdf\", \"b64_(bGhzIDo9IHJocyDigJQgZnVuY3Rpb24gZXRjLiBkZWZpbml0aW9u)\").")));
  }

  @Test
  public void testCompactSimpleRule() {

    AbstractKnowledgeBase kb = kb();
    kb.azzert(parseClause("first(X) :- second(X), third(X)."));
    kb.azzert(parseClause("second(X) :- fourth(X)."));
    kb.azzert(parseClause("third(X) :- fifth(X)."));

    List<Clause> rules = kb.compact();

    Assert.assertEquals(3, rules.size());
    Assert.assertTrue(rules.stream().anyMatch(rule -> rule.isRelevant(parseClause("second(X) :- fourth(X)."))));
    Assert.assertTrue(rules.stream().anyMatch(rule -> rule.isRelevant(parseClause("third(X) :- fifth(X)."))));
    Assert.assertTrue(
        rules.stream().anyMatch(rule -> rule.isRelevant(parseClause("first(X) :- fourth(X), fifth(X)."))));
  }

  @Test
  public void testCompactComplexRule() {

    AbstractKnowledgeBase kb = kb();
    kb.azzert(parseClause("first(X) :- second(X), third(X)."));
    kb.azzert(parseClause("second(X) :- fourth(X)."));
    kb.azzert(parseClause("third(X) :- fifth(X)."));
    kb.azzert(parseClause("fourth(X) :- sixth(X)."));

    List<Clause> rules = kb.compact();

    Assert.assertEquals(4, rules.size());
    Assert.assertTrue(rules.stream().anyMatch(rule -> rule.isRelevant(parseClause("fourth(X) :- sixth(X)."))));
    Assert.assertTrue(rules.stream().anyMatch(rule -> rule.isRelevant(parseClause("third(X) :- fifth(X)."))));
    Assert.assertTrue(rules.stream().anyMatch(rule -> rule.isRelevant(parseClause("second(X) :- sixth(X)."))));
    Assert.assertTrue(
        rules.stream().anyMatch(rule -> rule.isRelevant(parseClause("first(X) :- second(X), fifth(X)."))));
  }

  @Test
  public void testCompactSimpleRuleWithTwoBodies() {

    AbstractKnowledgeBase kb = kb();
    kb.azzert(parseClause("first(X) :- second(X), third(X)."));
    kb.azzert(parseClause("second(X) :- fourth(X)."));
    kb.azzert(parseClause("third(X) :- fifth(X)."));
    kb.azzert(parseClause("third(X) :- sixth(X)."));

    List<Clause> rules = kb.compact();

    Assert.assertEquals(5, rules.size());
    Assert.assertTrue(rules.stream().anyMatch(rule -> rule.isRelevant(parseClause("second(X) :- fourth(X)."))));
    Assert.assertTrue(rules.stream().anyMatch(rule -> rule.isRelevant(parseClause("third(X) :- fifth(X)."))));
    Assert.assertTrue(rules.stream().anyMatch(rule -> rule.isRelevant(parseClause("third(X) :- sixth(X)."))));
    Assert.assertTrue(
        rules.stream().anyMatch(rule -> rule.isRelevant(parseClause("first(X) :- fourth(X), fifth(X)."))));
    Assert.assertTrue(
        rules.stream().anyMatch(rule -> rule.isRelevant(parseClause("first(X) :- fourth(X), sixth(X)."))));
  }

  @Test
  public void testStreamAndMaterializeFacts1() {

    AbstractKnowledgeBase kb = addMockCreateJsonDefinition(kb());
    kb.azzert(parseClause(
        "load_json(JsonObject) :- fn_mock_json_materialize_facts(dummy, JsonString), fn_to_json(JsonObject, JsonString)."));
    kb.azzert(parseClause(
        "stream_array_elements(Object) :- load_json(Json), fn_is(Object, \"b64_(eyJjb2xfMSI6MjEsImNvbF8yIjoyMiwiY29sXzMiOjIzfQ==)\"), fn_materialize_facts(Json, Object)."));

    List<Clause> rules = kb.compact();

    Assert.assertEquals(2, rules.size());

    Solver solver = new Solver(kb, false);
    Literal query = parseQuery("stream_array_elements(Object)?");
    List<Clause> clauses = Lists.newArrayList(solver.solve(query));

    Clause clause = parseClause("stream_array_elements(\"b64_(eyJjb2xfMSI6MjEsImNvbF8yIjoyMiwiY29sXzMiOjIzfQ==)\").");

    Assert.assertEquals(1, clauses.size());
    Assert.assertTrue(clauses.contains(clause));
  }

  @Test
  public void testStreamAndMaterializeFacts2() {

    AbstractKnowledgeBase kb = addMockCreateJsonDefinition(kb());
    kb.azzert(parseClause(
        "load_json(JsonObject) :- fn_mock_json_materialize_facts(dummy, JsonString), fn_to_json(JsonObject, JsonString)."));
    kb.azzert(parseClause("stream_array_elements(Object) :- load_json(Json), fn_materialize_facts(Json, Object)."));

    List<Clause> rules = kb.compact();

    Assert.assertEquals(2, rules.size());

    Solver solver = new Solver(kb, false);
    Literal query = parseQuery("stream_array_elements(Object)?");
    List<Clause> clauses = Lists.newArrayList(solver.solve(query));

    Clause clause1 = parseClause("stream_array_elements(\"b64_(eyJjb2xfMSI6MjEsImNvbF8yIjoyMiwiY29sXzMiOjIzfQ==)\").");
    Clause clause2 = parseClause("stream_array_elements(\"b64_(eyJjb2xfMSI6MTEsImNvbF8yIjoxMiwiY29sXzMiOjEzfQ==)\").");

    Assert.assertEquals(2, clauses.size());
    Assert.assertTrue(
        (clauses.get(0).equals(clause1) && clauses.get(1).equals(clause2)) || (clauses.get(0).equals(clause2)
            && clauses.get(1).equals(clause1)));
  }

  private InMemoryKnowledgeBase kb() {
    return new InMemoryKnowledgeBase();
  }

  private AbstractKnowledgeBase addMockMaterializeFactsQueryDefinition1(AbstractKnowledgeBase kb) {
    kb.definitions().put("FN_MOCK_MATERIALIZE_FACTS", new Function("MOCK_MATERIALIZE_FACTS") {

      @Override
      protected boolean isCacheable() {
        return false;
      }

      @Override
      public BoxedType<?> evaluate(List<BoxedType<?>> parameters) {

        Preconditions.checkArgument(parameters.size() >= 3, "MOCK_MATERIALIZE_FACTS takes at least three parameters.");
        Preconditions.checkArgument(parameters.get(0).isString(), "%s should be a string", parameters.get(0));

        Map<String, Object> params = new HashMap<>();

        for (int i = 1; i < parameters.size(); i = i + 2) {

          String name = parameters.get(i).asString();
          String value = "_".equals(parameters.get(i + 1).asString()) ? null : parameters.get(i + 1).asString();

          params.put(name, value);
        }

        // Mock two distinct API calls
        Map<String, Object> json;
        String uri = parameters.get(0).asString();

        if (uri.equals("http://localhost:3000/crm1")) {
          json = JsonCodec.asObject(
              "{\"namespace\":\"crm1\",\"class\":\"clients\",\"facts\":[{\"id\":1,\"first_name\":\"Robert\",\"last_name\":\"Schwartz\",\"email\":\"rob23@gmail.com\"},{\"id\":2,\"first_name\":\"Lucy\",\"last_name\":\"Ballmer\",\"email\":\"lucyb56@gmail.com\"}]}");
        } else if (uri.equals("http://localhost:3000/crm2")) {
          json = JsonCodec.asObject(
              "{\"namespace\":\"crm2\",\"class\":\"clients\",\"facts\":[{\"id\":1,\"first_name\":\"Robert\",\"last_name\":\"Schwartz\",\"email\":\"rob23@gmail.com\"},{\"id\":3,\"first_name\":\"Anna\",\"last_name\":\"Smith\",\"email\":\"annasmith23@gmail.com\"},{\"id\":4,\"first_name\":\"Robert\",\"last_name\":\"Brown\",\"email\":\"bobbrown432@yahoo.com\"},{\"id\":5,\"first_name\":\"Roger\",\"last_name\":\"Bacon\",\"email\":\"rogerbacon12@yahoo.com\"}]}");
        } else {
          return BoxedType.empty();
        }

        // Mock server-side filtering
        json.put("facts", ((List<Map<String, Object>>) json.get("facts")).stream().filter(fact -> {
          for (Map.Entry<String, Object> param : params.entrySet()) {
            if (param.getValue() != null && fact.containsKey(param.getKey()) && !param.getValue()
                .equals(fact.get(param.getKey()))) {
              return false;
            }
          }
          return true;
        }).collect(Collectors.toList()));

        // Transform API result
        List<Literal> facts = ((List<Map<String, Object>>) json.get("facts")).stream().map(fact -> {

          List<AbstractTerm> terms = new ArrayList<>();
          terms.add(newConst(parameters.get(0)));

          for (int i = 1; i < parameters.size(); i = i + 2) {
            String name = parameters.get(i).asString();
            terms.add(newConst(name));
            terms.add(newConst(fact.get(name)));
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
    kb.definitions().put("FN_MOCK_MATERIALIZE_FACTS", new Function("MOCK_MATERIALIZE_FACTS") {

      @Override
      protected boolean isCacheable() {
        return false;
      }

      @Override
      public BoxedType<?> evaluate(List<BoxedType<?>> parameters) {

        List<Literal> literals = new ArrayList<>();
        literals.add(parseClause(
            "fn_mock_materialize_facts(\"https://localhost/facts/dab/fichier\", \"metadata.path\", \"*\", \"/var/sftp/file1.pdf\", \"metadata.md5_after\", \"824a*\", \"824a6d489b13f87d9006fe6842dd424b\").").head());
        literals.add(parseClause(
            "fn_mock_materialize_facts(\"https://localhost/facts/dab/fichier\", \"metadata.path\", \"*\", \"/var/sftp/file2.pdf\", \"metadata.md5_after\", \"824a*\", \"824afe9a2309abcf033bc74b7fe42a84\").").head());
        literals.add(parseClause(
            "fn_mock_materialize_facts(\"https://localhost/facts/dab/fichier\", \"metadata.path\", \"*\", \"/var/sftp/file2.pdf\", \"metadata.md5_after\", \"824a*\", \"824a6d489b13f87d9006fe6842dd424b\").").head());
        literals.add(parseClause(
            "fn_mock_materialize_facts(\"https://localhost/facts/dab/fichier\", \"metadata.path\", \"*\", \"/var/sftp/file3.pdf\", \"metadata.md5_after\", \"824a*\", \"824a6d489b13f87d9006fe6842dd424b\").").head());

        return BoxedType.create(literals);
      }
    });
    return kb;
  }

  private AbstractKnowledgeBase addMockMaterializeFactsQueryDefinition3(AbstractKnowledgeBase kb) {
    kb.definitions().put("FN_MOCK_MATERIALIZE_FACTS", new Function("MOCK_MATERIALIZE_FACTS") {

      @Override
      protected boolean isCacheable() {
        return false;
      }

      @Override
      public BoxedType<?> evaluate(List<BoxedType<?>> parameters) {

        List<Literal> literals = new ArrayList<>();

        String path = parameters.get(3).asString();
        String text = parameters.get(6).asString();

        if (parameters.get(0).asString().equals("https://localhost/facts/dab/fichier")) {
          if (("_".equals(path) || "/var/sftp/file1.pdf".equals(path)) && ("_".equals(text)
              || "The quick brown fox jumps over the lazy dog".equals(text))) {
            literals.add(new Literal("fn_mock_materialize_facts",
                Lists.newArrayList(newConst("https://localhost/facts/dab/fichier"), newConst("metadata.path"),
                    newConst("*"), newConst("/var/sftp/file1.pdf"), newConst("content.text"), newConst("*"),
                    newConst("The quick brown fox jumps over the lazy dog"))));
          }
          if (("_".equals(path) || "/var/sftp/file2.pdf".equals(path)) && ("_".equals(text)
              || "The quick brown fox\njumps over\r\nthe lazy dog".equals(text))) {
            literals.add(new Literal("fn_mock_materialize_facts",
                Lists.newArrayList(newConst("https://localhost/facts/dab/fichier"), newConst("metadata.path"),
                    newConst("*"), newConst("/var/sftp/file2.pdf"), newConst("content.text"), newConst("*"),
                    newConst("The quick brown fox\njumps over\r\nthe lazy dog"))));
          }
        } else {
          if (("_".equals(path) || "/var/sftp/file2.pdf".equals(path)) && ("_".equals(text)
              || "The quick brown fox\njumps over\r\nthe lazy dog".equals(text))) {
            Literal literal = new Literal("fn_mock_materialize_facts",
                Lists.newArrayList(newConst("https://localhost/facts/vam/fichier"), newConst("metadata.path"),
                    newConst("*"), newConst("/var/sftp/file2.pdf"), newConst("content.text"), newConst("*"),
                    newConst("The quick brown fox\njumps over\r\nthe lazy dog")));
            literals.add(literal);
          }
          if (("_".equals(path) || "/var/sftp/file3.pdf".equals(path)) && ("_".equals(text)
              || "The quick brown fox jumps over the lazy dog".equals(text))) {
            Literal literal = new Literal("fn_mock_materialize_facts",
                Lists.newArrayList(newConst("https://localhost/facts/vam/fichier"), newConst("metadata.path"),
                    newConst("*"), newConst("/var/sftp/file3.pdf"), newConst("content.text"), newConst("*"),
                    newConst("The quick brown fox jumps over the lazy dog")));
            literals.add(literal);
          }
        }
        return BoxedType.create(literals);
      }
    });
    return kb;
  }

  private AbstractKnowledgeBase addMockMaterializeFactsQueryDefinition4(AbstractKnowledgeBase kb) {
    kb.definitions().put("FN_MOCK_MATERIALIZE_FACTS", new Function("MOCK_MATERIALIZE_FACTS") {

      @Override
      protected boolean isCacheable() {
        return false;
      }

      @Override
      public BoxedType<?> evaluate(List<BoxedType<?>> parameters) {

        List<Literal> literals = new ArrayList<>();
        literals.add(parseClause(
            "fn_mock_materialize_facts(\"https://localhost/facts/dab/fichier\", \"metadata.path\", \"*\", \"/var/sftp/file1.pdf\", \"content.text\", \"*\", \"lhs := rhs — function etc. definition\").").head());
        literals.add(parseClause(
            "fn_mock_materialize_facts(\"https://localhost/facts/dab/fichier\", \"metadata.path\", \"*\", \"/var/sftp/file2.pdf\", \"content.text\", \"*\", \"x == val — test equality or represent a symbolic equation (!¤u003d for unequal)\").").head());
        literals.add(parseClause(
            "fn_mock_materialize_facts(\"https://localhost/facts/dab/fichier\", \"metadata.path\", \"*\", \"/var/sftp/file2.pdf\", \"content.text\", \"*\", \"lhs := rhs — function etc. definition\").").head());
        literals.add(parseClause(
            "fn_mock_materialize_facts(\"https://localhost/facts/dab/fichier\", \"metadata.path\", \"*\", \"/var/sftp/file3.pdf\", \"content.text\", \"*\", \"lhs := rhs — function etc. definition\").").head());

        return BoxedType.create(literals);
      }
    });
    return kb;
  }

  private AbstractKnowledgeBase addMockCreateJsonDefinition(AbstractKnowledgeBase kb) {

    String json = "[{\"col_1\": 11, \"col_2\": 12, \"col_3\": 13} , {\"col_1\": 21, \"col_2\": 22, \"col_3\": 23}]";

    kb.definitions().put("FN_MOCK_JSON_MATERIALIZE_FACTS", new Function("MOCK_JSON_MATERIALIZE_FACTS") {

      @Override
      protected boolean isCacheable() {
        return false;
      }

      @Override
      public BoxedType<?> evaluate(List<BoxedType<?>> parameters) {

        List<AbstractTerm> terms = new ArrayList<>();
        terms.add(newConst(parameters.get(0)));
        terms.add(newConst(json));

        return BoxedType.create(Lists.newArrayList(new Literal("fn_" + name().toLowerCase(), terms)));
      }
    });
    return kb;
  }
}
