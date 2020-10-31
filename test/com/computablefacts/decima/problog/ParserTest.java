package com.computablefacts.decima.problog;

import static com.computablefacts.decima.problog.TestUtils.parseClause;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.computablefacts.nona.helpers.Codecs;

public class ParserTest {

  @Test
  public void testParseComment() {
    Assert.assertNull(Parser.parseClause("% My comment"));
  }

  @Test(expected = IllegalStateException.class)
  public void testMissingFinalDotAfterFact() {
    Clause clause = parseClause("edge(a, b)");
  }

  // @Test(expected = IllegalStateException.class)
  // public void testMissingFinalDotAfterRule() {
  // Clause clause = parseClause("edge(X, Y) :- node(X), node(Y)");
  // }

  @Test(expected = IllegalStateException.class)
  public void testFactWithVariable() {
    Clause clause = parseClause("edge(a, U).");
  }

  @Test
  public void testParseFact() {

    Clause fact0 = new Clause(new Literal("edge", new Const("a"), new Const(0), new Const(1.1)));
    Clause fact1 = parseClause("edge(\"a\", 0, 1.1).");
    Clause fact2 = parseClause("edge(a, \"0\", \"1.1\").");

    Assert.assertEquals(fact0, fact1);
    Assert.assertEquals(fact0, fact2);
  }

  @Test
  public void testParseNegatedFact() {

    Clause fact0 = new Clause(new Literal("~edge", new Const("a"), new Const("b")));
    Clause fact1 = parseClause("~edge(a, b).");
    Clause fact2 = parseClause("\\+ edge(a, b).");

    Assert.assertEquals(fact0, fact1);
    Assert.assertEquals(fact0, fact2);
  }

  @Test
  public void testParseRule() {

    Var x = new Var();
    Var y = new Var();

    Literal edgeXY = new Literal("edge", x, y);
    Literal nodeX = new Literal("node", x);
    Literal nodeY = new Literal("node", y);

    Clause rule0 = new Clause(edgeXY, nodeX, nodeY);
    Clause rule1 = parseClause("edge(X, Y) :- node(X), node(Y).");

    Assert.assertTrue(rule0.isRelevant(rule1));
  }

  @Test
  public void testParseRuleWithNegatedLiteralsInBody() {

    Var x = new Var();
    Var y = new Var();

    Literal edgeXY = new Literal("not_edge", x, y);
    Literal nodeX = new Literal("~node", x);
    Literal nodeY = new Literal("node", y);

    Clause rule0 = new Clause(edgeXY, nodeX, nodeY);
    Clause rule1 = parseClause("not_edge(X, Y) :- ~node(X), node(Y).");

    Assert.assertTrue(rule0.isRelevant(rule1));
  }

  @Test
  public void testParseEqBuiltin() {

    Clause rule1 = parseClause("edge(X, Y) :- node(X), node(Y), X=Y.");
    Clause rule2 = parseClause("edge(X, Y) :- node(X), node(Y), fn_eq(U, X, Y), fn_is_true(U).");

    Assert.assertTrue(rule1.isRelevant(rule2));
  }

  @Test
  public void testParseNotEqBuiltin() {

    Clause rule1 = parseClause("edge(X, Y) :- node(X), node(Y), X!=Y.");
    Clause rule2 = parseClause("edge(X, Y) :- node(X), node(Y), X<>Y.");
    Clause rule3 = parseClause("edge(X, Y) :- node(X), node(Y), fn_eq(U, X, Y), fn_is_false(U).");

    Assert.assertTrue(rule1.isRelevant(rule2));
    Assert.assertTrue(rule1.isRelevant(rule3));
    Assert.assertTrue(rule2.isRelevant(rule3));
  }

  @Test
  public void testParseLtBuiltin() {

    Clause rule1 = parseClause("edge(X, Y) :- node(X), node(Y), X<Y.");
    Clause rule2 = parseClause("edge(X, Y) :- node(X), node(Y), fn_lt(U, X, Y), fn_is_true(U).");

    Assert.assertTrue(rule1.isRelevant(rule2));
  }

  @Test
  public void testParseLteBuiltin() {

    Clause rule1 = parseClause("edge(X, Y) :- node(X), node(Y), X<=Y.");
    Clause rule2 = parseClause("edge(X, Y) :- node(X), node(Y), fn_lte(U, X, Y), fn_is_true(U).");

    Assert.assertTrue(rule1.isRelevant(rule2));
  }

  @Test
  public void testParseGtBuiltin() {

    Clause rule1 = parseClause("edge(X, Y) :- node(X), node(Y), X>Y.");
    Clause rule2 = parseClause("edge(X, Y) :- node(X), node(Y), fn_gt(U, X, Y), fn_is_true(U).");

    Assert.assertTrue(rule1.isRelevant(rule2));
  }

  @Test
  public void testParseGteBuiltin() {

    Clause rule1 = parseClause("edge(X, Y) :- node(X), node(Y), X>=Y.");
    Clause rule2 = parseClause("edge(X, Y) :- node(X), node(Y), fn_gte(U, X, Y), fn_is_true(U).");

    Assert.assertTrue(rule1.isRelevant(rule2));
  }

  @Test
  public void testParseProbabilityOnFact() {

    Clause fact = parseClause("0.3::edge(a, b).");

    Assert.assertTrue(fact.isFact());
    Assert.assertFalse(fact.isRule());

    Assert.assertEquals("edge", fact.head().predicate().name());
    Assert.assertEquals(2, fact.head().predicate().arity());

    Assert.assertEquals(BigDecimal.valueOf(0.3), fact.head().probability());
  }

  @Test
  public void testParseProbabilityOnRule() {

    Clause rule = parseClause("0.3::edge(X, Y) :- node(X), node(Y).");

    Assert.assertTrue(rule.isRule());
    Assert.assertFalse(rule.isFact());

    Assert.assertEquals("edge", rule.head().predicate().name());
    Assert.assertEquals(2, rule.head().predicate().arity());

    Assert.assertEquals(BigDecimal.valueOf(0.3), rule.head().probability());
  }

  @Test
  public void testParseFunction() {

    Clause rule = Parser
        .parseClause("under_and_above(X, Y) :- fn_if(O, fn_and(fn_lt(X, 0), fn_gt(Y, 0)), 1, 0).");

    Assert.assertTrue(rule.isRule());
    Assert.assertFalse(rule.isFact());
  }

  @Test
  public void testParseClauseAsQuery() {

    Clause query = Parser.parseClause("edge(X, Y)?");

    Assert.assertEquals(new Literal("edge", new Var(), new Var()), query.head());
  }

  @Test
  public void testParseQuery() {

    Literal query = Parser.parseQuery("edge(X, Y)?");

    Assert.assertEquals(new Literal("edge", new Var(), new Var()), query);
  }

  @Test
  public void testParseQueries() {

    Set<Literal> queries = Parser.parseQueries("edge(X, Y)?\nedge(a, Y)?\nedge(X, b)?");

    Assert.assertEquals(3, queries.size());
    Assert.assertTrue(queries.contains(new Literal("edge", new Var(), new Var())));
    Assert.assertTrue(queries.contains(new Literal("edge", new Const("a"), new Var())));
    Assert.assertTrue(queries.contains(new Literal("edge", new Var(), new Const("b"))));
  }

  @Test
  public void testParseJsonString() {

    Clause clause = Parser.parseClause(
        "json_path(\"jhWTAETz\", \"data\", \"9\", \"rawOutput\", \"[{\\u0022Modified\\u0022:\\u00222020-07-07T12:24:00\\u0022\\u002c\\u0022Published\\u0022:1594088100000\\u002c\\u0022access.authentication\\u0022:\\u0022NONE\\u0022\\u002c\\u0022access.complexity\\u0022:\\u0022LOW\\u0022\\u002c\\u0022access.vector\\u0022:\\u0022NETWORK\\u0022\\u002c\\u0022assigner\\u0022:\\u0022cve@mitre.org\\u0022\\u002c\\u0022cvss\\u0022:7.5\\u002c\\u0022cvss-time\\u0022:null\\u002c\\u0022cvss-vector\\u0022:null\\u002c\\u0022cwe\\u0022:\\u0022NVD-CWE-noinfo\\u0022\\u002c\\u0022id\\u0022:\\u0022CVE-2020-15505\\u0022\\u002c\\u0022impact.availability\\u0022:\\u0022PARTIAL\\u0022\\u002c\\u0022impact.confidentiality\\u0022:\\u0022PARTIAL\\u0022\\u002c\\u0022impact.integrity\\u0022:\\u0022PARTIAL\\u0022\\u002c\\u0022last-modified\\u0022:\\u00222020-09-18T16:15:00\\u0022\\u002c\\u0022references\\u0022:[\\u0022https:\\/\\/www.mobileiron.com\\/en\\/blog\\/mobileiron-security-updates-available\\u0022]\\u002c\\u0022summary\\u0022:\\u0022A remote code execution vulnerability in MobileIron Core & Connector versions 10.3.0.3 and earlier\\u002c 10.4.0.0\\u002c 10.4.0.1\\u002c 10.4.0.2\\u002c 10.4.0.3\\u002c 10.5.1.0\\u002c 10.5.2.0 and 10.6.0.0; and Sentry versions 9.7.2 and earlier\\u002c and 9.8.0; and Monitor and Reporting Database \\u0028RDB\\u0029 version 2.0.0.1 and earlier that allows remote attackers to execute arbitrary code via unspecified vectors.\\u0022\\u002c\\u0022vulnerable_configuration\\u0022:[\\u0022cpe:2.3:a:mobileiron:cloud:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:cloud:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:core:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:core:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:enterprise_connector:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:enterprise_connector:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:reporting_database:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:reporting_database:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:sentry:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:sentry:9.8:*:*:*:*:*:*:*\\u0022]\\u002c\\u0022vulnerable_configuration_cpe_2_2\\u0022:[]\\u002c\\u0022vulnerable_product\\u0022:[\\u0022cpe:2.3:a:mobileiron:cloud:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:cloud:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:core:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:core:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:enterprise_connector:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:enterprise_connector:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:reporting_database:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:reporting_database:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:sentry:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:sentry:9.8:*:*:*:*:*:*:*\\u0022]}\\u002c{\\u0022Modified\\u0022:\\u00222020-07-07T12:24:00\\u0022\\u002c\\u0022Published\\u0022:1594088100000\\u002c\\u0022access.authentication\\u0022:\\u0022NONE\\u0022\\u002c\\u0022access.complexity\\u0022:\\u0022LOW\\u0022\\u002c\\u0022access.vector\\u0022:\\u0022NETWORK\\u0022\\u002c\\u0022assigner\\u0022:\\u0022cve@mitre.org\\u0022\\u002c\\u0022cvss\\u0022:7.5\\u002c\\u0022cvss-time\\u0022:null\\u002c\\u0022cvss-vector\\u0022:null\\u002c\\u0022cwe\\u0022:\\u0022CWE-287\\u0022\\u002c\\u0022id\\u0022:\\u0022CVE-2020-15506\\u0022\\u002c\\u0022impact.availability\\u0022:\\u0022PARTIAL\\u0022\\u002c\\u0022impact.confidentiality\\u0022:\\u0022PARTIAL\\u0022\\u002c\\u0022impact.integrity\\u0022:\\u0022PARTIAL\\u0022\\u002c\\u0022last-modified\\u0022:\\u00222020-09-18T17:15:00\\u0022\\u002c\\u0022references\\u0022:[\\u0022https:\\/\\/www.mobileiron.com\\/en\\/blog\\/mobileiron-security-updates-available\\u0022]\\u002c\\u0022summary\\u0022:\\u0022An authentication bypass vulnerability in MobileIron Core & Connector versions 10.3.0.3 and earlier\\u002c 10.4.0.0\\u002c 10.4.0.1\\u002c 10.4.0.2\\u002c 10.4.0.3\\u002c 10.5.1.0\\u002c 10.5.2.0 and 10.6.0.0 that allows remote attackers to bypass authentication mechanisms via unspecified vectors.\\u0022\\u002c\\u0022vulnerable_configuration\\u0022:[\\u0022cpe:2.3:a:mobileiron:cloud:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:cloud:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:core:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:core:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:enterprise_connector:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:enterprise_connector:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:reporting_database:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:reporting_database:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:sentry:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:sentry:9.8:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:sentry:10.6:*:*:*:*:*:*:*\\u0022]\\u002c\\u0022vulnerable_configuration_cpe_2_2\\u0022:[]\\u002c\\u0022vulnerable_product\\u0022:[\\u0022cpe:2.3:a:mobileiron:cloud:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:cloud:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:core:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:core:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:enterprise_connector:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:enterprise_connector:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:reporting_database:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:reporting_database:10.6:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:sentry:-:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:sentry:9.8:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:sentry:10.6:*:*:*:*:*:*:*\\u0022]}\\u002c{\\u0022Modified\\u0022:\\u00222020-02-21T15:13:00\\u0022\\u002c\\u0022Published\\u0022:1581635700000\\u002c\\u0022access.authentication\\u0022:\\u0022NONE\\u0022\\u002c\\u0022access.complexity\\u0022:\\u0022LOW\\u0022\\u002c\\u0022access.vector\\u0022:\\u0022NETWORK\\u0022\\u002c\\u0022assigner\\u0022:\\u0022cve@mitre.org\\u0022\\u002c\\u0022cvss\\u0022:10.0\\u002c\\u0022cvss-time\\u0022:\\u00222020-02-21T15:13:00\\u0022\\u002c\\u0022cvss-vector\\u0022:\\u0022AV:N\\/AC:L\\/Au:N\\/C:C\\/I:C\\/A:C\\u0022\\u002c\\u0022cwe\\u0022:\\u0022CWE-326\\u0022\\u002c\\u0022id\\u0022:\\u0022CVE-2013-7287\\u0022\\u002c\\u0022impact.availability\\u0022:\\u0022COMPLETE\\u0022\\u002c\\u0022impact.confidentiality\\u0022:\\u0022COMPLETE\\u0022\\u002c\\u0022impact.integrity\\u0022:\\u0022COMPLETE\\u0022\\u002c\\u0022last-modified\\u0022:null\\u002c\\u0022references\\u0022:[\\u0022http:\\/\\/seclists.org\\/fulldisclosure\\/2014\\/Apr\\/21\\u0022\\u002c\\u0022https:\\/\\/www.securityfocus.com\\/archive\\/1\\/531713\\u0022]\\u002c\\u0022summary\\u0022:\\u0022MobileIron VSP < 5.9.1 and Sentry < 5.0 has an insecure encryption scheme.\\u0022\\u002c\\u0022vulnerable_configuration\\u0022:[\\u0022cpe:2.3:a:mobileiron:sentry:*:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:virtual_smartphone_platform:*:*:*:*:*:*:*:*\\u0022]\\u002c\\u0022vulnerable_configuration_cpe_2_2\\u0022:[]\\u002c\\u0022vulnerable_product\\u0022:[\\u0022cpe:2.3:a:mobileiron:sentry:*:*:*:*:*:*:*:*\\u0022\\u002c\\u0022cpe:2.3:a:mobileiron:virtual_smartphone_platform:*:*:*:*:*:*:*:*\\u0022]}]\").");

    Predicate predicate = clause.head().predicate();
    List<AbstractTerm> terms = clause.head().terms();

    Assert.assertEquals("json_path", predicate.baseName());
    Assert.assertEquals(5, predicate.arity());
    Assert.assertEquals(
        "[{\"Modified\":\"2020-07-07T12:24:00\",\"Published\":1594088100000,\"access.authentication\":\"NONE\",\"access.complexity\":\"LOW\",\"access.vector\":\"NETWORK\",\"assigner\":\"cve@mitre.org\",\"cvss\":7.5,\"cvss-time\":null,\"cvss-vector\":null,\"cwe\":\"NVD-CWE-noinfo\",\"id\":\"CVE-2020-15505\",\"impact.availability\":\"PARTIAL\",\"impact.confidentiality\":\"PARTIAL\",\"impact.integrity\":\"PARTIAL\",\"last-modified\":\"2020-09-18T16:15:00\",\"references\":[\"https://www.mobileiron.com/en/blog/mobileiron-security-updates-available\"],\"summary\":\"A remote code execution vulnerability in MobileIron Core & Connector versions 10.3.0.3 and earlier, 10.4.0.0, 10.4.0.1, 10.4.0.2, 10.4.0.3, 10.5.1.0, 10.5.2.0 and 10.6.0.0; and Sentry versions 9.7.2 and earlier, and 9.8.0; and Monitor and Reporting Database (RDB) version 2.0.0.1 and earlier that allows remote attackers to execute arbitrary code via unspecified vectors.\",\"vulnerable_configuration\":[\"cpe:2.3:a:mobileiron:cloud:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:cloud:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:core:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:core:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:enterprise_connector:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:enterprise_connector:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:reporting_database:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:reporting_database:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:9.8:*:*:*:*:*:*:*\"],\"vulnerable_configuration_cpe_2_2\":[],\"vulnerable_product\":[\"cpe:2.3:a:mobileiron:cloud:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:cloud:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:core:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:core:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:enterprise_connector:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:enterprise_connector:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:reporting_database:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:reporting_database:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:9.8:*:*:*:*:*:*:*\"]},{\"Modified\":\"2020-07-07T12:24:00\",\"Published\":1594088100000,\"access.authentication\":\"NONE\",\"access.complexity\":\"LOW\",\"access.vector\":\"NETWORK\",\"assigner\":\"cve@mitre.org\",\"cvss\":7.5,\"cvss-time\":null,\"cvss-vector\":null,\"cwe\":\"CWE-287\",\"id\":\"CVE-2020-15506\",\"impact.availability\":\"PARTIAL\",\"impact.confidentiality\":\"PARTIAL\",\"impact.integrity\":\"PARTIAL\",\"last-modified\":\"2020-09-18T17:15:00\",\"references\":[\"https://www.mobileiron.com/en/blog/mobileiron-security-updates-available\"],\"summary\":\"An authentication bypass vulnerability in MobileIron Core & Connector versions 10.3.0.3 and earlier, 10.4.0.0, 10.4.0.1, 10.4.0.2, 10.4.0.3, 10.5.1.0, 10.5.2.0 and 10.6.0.0 that allows remote attackers to bypass authentication mechanisms via unspecified vectors.\",\"vulnerable_configuration\":[\"cpe:2.3:a:mobileiron:cloud:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:cloud:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:core:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:core:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:enterprise_connector:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:enterprise_connector:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:reporting_database:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:reporting_database:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:9.8:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:10.6:*:*:*:*:*:*:*\"],\"vulnerable_configuration_cpe_2_2\":[],\"vulnerable_product\":[\"cpe:2.3:a:mobileiron:cloud:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:cloud:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:core:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:core:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:enterprise_connector:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:enterprise_connector:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:reporting_database:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:reporting_database:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:9.8:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:10.6:*:*:*:*:*:*:*\"]},{\"Modified\":\"2020-02-21T15:13:00\",\"Published\":1581635700000,\"access.authentication\":\"NONE\",\"access.complexity\":\"LOW\",\"access.vector\":\"NETWORK\",\"assigner\":\"cve@mitre.org\",\"cvss\":10.0,\"cvss-time\":\"2020-02-21T15:13:00\",\"cvss-vector\":\"AV:N/AC:L/Au:N/C:C/I:C/A:C\",\"cwe\":\"CWE-326\",\"id\":\"CVE-2013-7287\",\"impact.availability\":\"COMPLETE\",\"impact.confidentiality\":\"COMPLETE\",\"impact.integrity\":\"COMPLETE\",\"last-modified\":null,\"references\":[\"http://seclists.org/fulldisclosure/2014/Apr/21\",\"https://www.securityfocus.com/archive/1/531713\"],\"summary\":\"MobileIron VSP < 5.9.1 and Sentry < 5.0 has an insecure encryption scheme.\",\"vulnerable_configuration\":[\"cpe:2.3:a:mobileiron:sentry:*:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:virtual_smartphone_platform:*:*:*:*:*:*:*:*\"],\"vulnerable_configuration_cpe_2_2\":[],\"vulnerable_product\":[\"cpe:2.3:a:mobileiron:sentry:*:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:virtual_smartphone_platform:*:*:*:*:*:*:*:*\"]}]",
        terms.get(4).toString());

    Collection<Map<String, Object>> jsons = Codecs.asCollection(terms.get(4).toString());

    Assert.assertEquals(3, jsons.size());
  }
}
