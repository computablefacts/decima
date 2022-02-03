package com.computablefacts.decima.problog;

import static com.computablefacts.decima.problog.AbstractTerm.newConst;
import static com.computablefacts.decima.problog.AbstractTerm.newVar;
import static com.computablefacts.decima.problog.Parser.parseClause;
import static com.computablefacts.decima.problog.Parser.reorderBodyLiterals;
import static com.computablefacts.decima.problog.TestUtils.permute;

import java.math.BigDecimal;
import java.util.*;

import org.junit.Assert;
import org.junit.Test;

import com.computablefacts.asterix.codecs.JsonCodec;
import com.google.common.collect.Lists;

public class ParserTest {

  @Test
  public void testParseComment() {
    Assert.assertNull(parseClause("% My comment"));
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

    Clause fact0 = new Clause(new Literal("edge", newConst("a"), newConst(0), newConst(1.1)));
    Clause fact1 = parseClause("edge(\"a\", 0, 1.1).");
    Clause fact2 = parseClause("edge(a, \"0\", \"1.1\").");

    Assert.assertEquals(fact0, fact1);
    Assert.assertEquals(fact0, fact2);
  }

  @Test
  public void testParseNegatedFact() {

    Clause fact0 = new Clause(new Literal("~edge", newConst("a"), newConst("b")));
    Clause fact1 = parseClause("~edge(a, b).");
    Clause fact2 = parseClause("\\+ edge(a, b).");

    Assert.assertEquals(fact0, fact1);
    Assert.assertEquals(fact0, fact2);
  }

  @Test
  public void testParseRule() {

    Var x = newVar();
    Var y = newVar();

    Literal edgeXY = new Literal("edge", x, y);
    Literal nodeX = new Literal("node", x);
    Literal nodeY = new Literal("node", y);

    Clause rule0 = new Clause(edgeXY, nodeX, nodeY);
    Clause rule1 = parseClause("edge(X, Y) :- node(X), node(Y).");

    Assert.assertTrue(rule0.isRelevant(rule1));
  }

  @Test
  public void testParseRuleWithNegatedLiteralsInBody() {

    Var x = newVar();
    Var y = newVar();

    Literal edgeXY = new Literal("not_edge", x, y);
    Literal nodeX = new Literal("~node", x);
    Literal nodeY = new Literal("node", y);

    Clause rule0 = reorderBodyLiterals(new Clause(edgeXY, nodeX, nodeY));
    Clause rule1 = parseClause("not_edge(X, Y) :- node(Y), ~node(X).");

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
  public void testParseFnIsWithOneFunction() {

    Clause rule1 = parseClause("is_valid(X) :- fn_is_true(fn_test(X)).");
    Clause rule2 = parseClause("is_valid(X) :- fn_test(Y, X), fn_is_true(Y).");

    Assert.assertTrue(rule1.isRelevant(rule2));
  }

  @Test
  public void testParseFnIsWithMoreThanOneFunction() {

    Clause rule = parseClause("is_valid(X, Y) :- fn_is_true(fn_and(fn_test(X), fn_test(Y))).");

    Assert.assertTrue(rule.head().isRelevant(new Literal("is_valid", newVar(), newVar())));
    Assert.assertEquals(2, rule.body().size());
    Assert.assertTrue(rule.body().get(0).predicate().name().startsWith("fn_shadow_"));
    Assert.assertEquals(3, rule.body().get(0).predicate().arity());
    Assert.assertTrue(rule.body().get(1).isRelevant(new Literal("fn_is_true", newVar())));
  }

  @Test
  public void testParseIs() {

    Clause rule1 = parseClause("is_even(X) :- fn_mod(U, X, 2), U is 0.");
    Clause rule2 = parseClause("is_even(X) :- fn_mod(U, X, 2), fn_is(U, 0)");

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

    Clause rule =
        parseClause("under_and_above(X, Y) :- fn_if(O, fn_and(fn_lt(X, 0), fn_gt(Y, 0)), 1, 0).");

    Assert.assertTrue(rule.isRule());
    Assert.assertFalse(rule.isFact());
  }

  @Test
  public void testParseClauseAsQuery() {

    Clause query = parseClause("edge(X, Y)?");

    Assert.assertEquals(new Literal("edge", newVar(), newVar()), query.head());
  }

  @Test
  public void testParseQuery() {

    Literal query = Parser.parseQuery("edge(X, Y)?");

    Assert.assertEquals(new Literal("edge", newVar(), newVar()), query);
  }

  @Test
  public void testParseQueries() {

    Set<Literal> queries = Parser.parseQueries("edge(X, Y)?\nedge(a, Y)?\nedge(X, b)?");

    Assert.assertEquals(3, queries.size());
    Assert.assertTrue(queries.contains(new Literal("edge", newVar(), newVar())));
    Assert.assertTrue(queries.contains(new Literal("edge", newConst("a"), newVar())));
    Assert.assertTrue(queries.contains(new Literal("edge", newVar(), newConst("b"))));
  }

  @Test
  public void testParseJsonString() {

    Clause clause = parseClause(
        "json_path(\"jhWTAETz\", \"data\", \"9\", \"rawOutput\", \"[{¤u0022Modified¤u0022:¤u00222020-07-07T12:24:00¤u0022¤u002c¤u0022Published¤u0022:1594088100000¤u002c¤u0022access.authentication¤u0022:¤u0022NONE¤u0022¤u002c¤u0022access.complexity¤u0022:¤u0022LOW¤u0022¤u002c¤u0022access.vector¤u0022:¤u0022NETWORK¤u0022¤u002c¤u0022assigner¤u0022:¤u0022cve@mitre.org¤u0022¤u002c¤u0022cvss¤u0022:7.5¤u002c¤u0022cvss-time¤u0022:null¤u002c¤u0022cvss-vector¤u0022:null¤u002c¤u0022cwe¤u0022:¤u0022NVD-CWE-noinfo¤u0022¤u002c¤u0022id¤u0022:¤u0022CVE-2020-15505¤u0022¤u002c¤u0022impact.availability¤u0022:¤u0022PARTIAL¤u0022¤u002c¤u0022impact.confidentiality¤u0022:¤u0022PARTIAL¤u0022¤u002c¤u0022impact.integrity¤u0022:¤u0022PARTIAL¤u0022¤u002c¤u0022last-modified¤u0022:¤u00222020-09-18T16:15:00¤u0022¤u002c¤u0022references¤u0022:[¤u0022https:\\/\\/www.mobileiron.com\\/en\\/blog\\/mobileiron-security-updates-available¤u0022]¤u002c¤u0022summary¤u0022:¤u0022A remote code execution vulnerability in MobileIron Core & Connector versions 10.3.0.3 and earlier¤u002c 10.4.0.0¤u002c 10.4.0.1¤u002c 10.4.0.2¤u002c 10.4.0.3¤u002c 10.5.1.0¤u002c 10.5.2.0 and 10.6.0.0; and Sentry versions 9.7.2 and earlier¤u002c and 9.8.0; and Monitor and Reporting Database ¤u0028RDB¤u0029 version 2.0.0.1 and earlier that allows remote attackers to execute arbitrary code via unspecified vectors.¤u0022¤u002c¤u0022vulnerable_configuration¤u0022:[¤u0022cpe:2.3:a:mobileiron:cloud:-:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:cloud:10.6:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:core:-:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:core:10.6:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:enterprise_connector:-:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:enterprise_connector:10.6:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:reporting_database:-:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:reporting_database:10.6:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:sentry:-:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:sentry:9.8:*:*:*:*:*:*:*¤u0022]¤u002c¤u0022vulnerable_configuration_cpe_2_2¤u0022:[]¤u002c¤u0022vulnerable_product¤u0022:[¤u0022cpe:2.3:a:mobileiron:cloud:-:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:cloud:10.6:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:core:-:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:core:10.6:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:enterprise_connector:-:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:enterprise_connector:10.6:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:reporting_database:-:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:reporting_database:10.6:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:sentry:-:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:sentry:9.8:*:*:*:*:*:*:*¤u0022]}¤u002c{¤u0022Modified¤u0022:¤u00222020-07-07T12:24:00¤u0022¤u002c¤u0022Published¤u0022:1594088100000¤u002c¤u0022access.authentication¤u0022:¤u0022NONE¤u0022¤u002c¤u0022access.complexity¤u0022:¤u0022LOW¤u0022¤u002c¤u0022access.vector¤u0022:¤u0022NETWORK¤u0022¤u002c¤u0022assigner¤u0022:¤u0022cve@mitre.org¤u0022¤u002c¤u0022cvss¤u0022:7.5¤u002c¤u0022cvss-time¤u0022:null¤u002c¤u0022cvss-vector¤u0022:null¤u002c¤u0022cwe¤u0022:¤u0022CWE-287¤u0022¤u002c¤u0022id¤u0022:¤u0022CVE-2020-15506¤u0022¤u002c¤u0022impact.availability¤u0022:¤u0022PARTIAL¤u0022¤u002c¤u0022impact.confidentiality¤u0022:¤u0022PARTIAL¤u0022¤u002c¤u0022impact.integrity¤u0022:¤u0022PARTIAL¤u0022¤u002c¤u0022last-modified¤u0022:¤u00222020-09-18T17:15:00¤u0022¤u002c¤u0022references¤u0022:[¤u0022https:\\/\\/www.mobileiron.com\\/en\\/blog\\/mobileiron-security-updates-available¤u0022]¤u002c¤u0022summary¤u0022:¤u0022An authentication bypass vulnerability in MobileIron Core & Connector versions 10.3.0.3 and earlier¤u002c 10.4.0.0¤u002c 10.4.0.1¤u002c 10.4.0.2¤u002c 10.4.0.3¤u002c 10.5.1.0¤u002c 10.5.2.0 and 10.6.0.0 that allows remote attackers to bypass authentication mechanisms via unspecified vectors.¤u0022¤u002c¤u0022vulnerable_configuration¤u0022:[¤u0022cpe:2.3:a:mobileiron:cloud:-:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:cloud:10.6:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:core:-:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:core:10.6:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:enterprise_connector:-:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:enterprise_connector:10.6:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:reporting_database:-:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:reporting_database:10.6:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:sentry:-:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:sentry:9.8:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:sentry:10.6:*:*:*:*:*:*:*¤u0022]¤u002c¤u0022vulnerable_configuration_cpe_2_2¤u0022:[]¤u002c¤u0022vulnerable_product¤u0022:[¤u0022cpe:2.3:a:mobileiron:cloud:-:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:cloud:10.6:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:core:-:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:core:10.6:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:enterprise_connector:-:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:enterprise_connector:10.6:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:reporting_database:-:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:reporting_database:10.6:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:sentry:-:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:sentry:9.8:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:sentry:10.6:*:*:*:*:*:*:*¤u0022]}¤u002c{¤u0022Modified¤u0022:¤u00222020-02-21T15:13:00¤u0022¤u002c¤u0022Published¤u0022:1581635700000¤u002c¤u0022access.authentication¤u0022:¤u0022NONE¤u0022¤u002c¤u0022access.complexity¤u0022:¤u0022LOW¤u0022¤u002c¤u0022access.vector¤u0022:¤u0022NETWORK¤u0022¤u002c¤u0022assigner¤u0022:¤u0022cve@mitre.org¤u0022¤u002c¤u0022cvss¤u0022:10.0¤u002c¤u0022cvss-time¤u0022:¤u00222020-02-21T15:13:00¤u0022¤u002c¤u0022cvss-vector¤u0022:¤u0022AV:N\\/AC:L\\/Au:N\\/C:C\\/I:C\\/A:C¤u0022¤u002c¤u0022cwe¤u0022:¤u0022CWE-326¤u0022¤u002c¤u0022id¤u0022:¤u0022CVE-2013-7287¤u0022¤u002c¤u0022impact.availability¤u0022:¤u0022COMPLETE¤u0022¤u002c¤u0022impact.confidentiality¤u0022:¤u0022COMPLETE¤u0022¤u002c¤u0022impact.integrity¤u0022:¤u0022COMPLETE¤u0022¤u002c¤u0022last-modified¤u0022:null¤u002c¤u0022references¤u0022:[¤u0022http:\\/\\/seclists.org\\/fulldisclosure\\/2014\\/Apr\\/21¤u0022¤u002c¤u0022https:\\/\\/www.securityfocus.com\\/archive\\/1\\/531713¤u0022]¤u002c¤u0022summary¤u0022:¤u0022MobileIron VSP < 5.9.1 and Sentry < 5.0 has an insecure encryption scheme.¤u0022¤u002c¤u0022vulnerable_configuration¤u0022:[¤u0022cpe:2.3:a:mobileiron:sentry:*:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:virtual_smartphone_platform:*:*:*:*:*:*:*:*¤u0022]¤u002c¤u0022vulnerable_configuration_cpe_2_2¤u0022:[]¤u002c¤u0022vulnerable_product¤u0022:[¤u0022cpe:2.3:a:mobileiron:sentry:*:*:*:*:*:*:*:*¤u0022¤u002c¤u0022cpe:2.3:a:mobileiron:virtual_smartphone_platform:*:*:*:*:*:*:*:*¤u0022]}]\").");

    Predicate predicate = clause.head().predicate();
    List<AbstractTerm> terms = clause.head().terms();

    Assert.assertEquals("json_path", predicate.baseName());
    Assert.assertEquals(5, predicate.arity());
    Assert.assertEquals(
        "[{\"Modified\":\"2020-07-07T12:24:00\",\"Published\":1594088100000,\"access.authentication\":\"NONE\",\"access.complexity\":\"LOW\",\"access.vector\":\"NETWORK\",\"assigner\":\"cve@mitre.org\",\"cvss\":7.5,\"cvss-time\":null,\"cvss-vector\":null,\"cwe\":\"NVD-CWE-noinfo\",\"id\":\"CVE-2020-15505\",\"impact.availability\":\"PARTIAL\",\"impact.confidentiality\":\"PARTIAL\",\"impact.integrity\":\"PARTIAL\",\"last-modified\":\"2020-09-18T16:15:00\",\"references\":[\"https://www.mobileiron.com/en/blog/mobileiron-security-updates-available\"],\"summary\":\"A remote code execution vulnerability in MobileIron Core & Connector versions 10.3.0.3 and earlier, 10.4.0.0, 10.4.0.1, 10.4.0.2, 10.4.0.3, 10.5.1.0, 10.5.2.0 and 10.6.0.0; and Sentry versions 9.7.2 and earlier, and 9.8.0; and Monitor and Reporting Database (RDB) version 2.0.0.1 and earlier that allows remote attackers to execute arbitrary code via unspecified vectors.\",\"vulnerable_configuration\":[\"cpe:2.3:a:mobileiron:cloud:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:cloud:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:core:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:core:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:enterprise_connector:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:enterprise_connector:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:reporting_database:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:reporting_database:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:9.8:*:*:*:*:*:*:*\"],\"vulnerable_configuration_cpe_2_2\":[],\"vulnerable_product\":[\"cpe:2.3:a:mobileiron:cloud:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:cloud:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:core:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:core:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:enterprise_connector:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:enterprise_connector:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:reporting_database:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:reporting_database:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:9.8:*:*:*:*:*:*:*\"]},{\"Modified\":\"2020-07-07T12:24:00\",\"Published\":1594088100000,\"access.authentication\":\"NONE\",\"access.complexity\":\"LOW\",\"access.vector\":\"NETWORK\",\"assigner\":\"cve@mitre.org\",\"cvss\":7.5,\"cvss-time\":null,\"cvss-vector\":null,\"cwe\":\"CWE-287\",\"id\":\"CVE-2020-15506\",\"impact.availability\":\"PARTIAL\",\"impact.confidentiality\":\"PARTIAL\",\"impact.integrity\":\"PARTIAL\",\"last-modified\":\"2020-09-18T17:15:00\",\"references\":[\"https://www.mobileiron.com/en/blog/mobileiron-security-updates-available\"],\"summary\":\"An authentication bypass vulnerability in MobileIron Core & Connector versions 10.3.0.3 and earlier, 10.4.0.0, 10.4.0.1, 10.4.0.2, 10.4.0.3, 10.5.1.0, 10.5.2.0 and 10.6.0.0 that allows remote attackers to bypass authentication mechanisms via unspecified vectors.\",\"vulnerable_configuration\":[\"cpe:2.3:a:mobileiron:cloud:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:cloud:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:core:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:core:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:enterprise_connector:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:enterprise_connector:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:reporting_database:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:reporting_database:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:9.8:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:10.6:*:*:*:*:*:*:*\"],\"vulnerable_configuration_cpe_2_2\":[],\"vulnerable_product\":[\"cpe:2.3:a:mobileiron:cloud:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:cloud:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:core:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:core:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:enterprise_connector:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:enterprise_connector:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:reporting_database:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:reporting_database:10.6:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:-:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:9.8:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:sentry:10.6:*:*:*:*:*:*:*\"]},{\"Modified\":\"2020-02-21T15:13:00\",\"Published\":1581635700000,\"access.authentication\":\"NONE\",\"access.complexity\":\"LOW\",\"access.vector\":\"NETWORK\",\"assigner\":\"cve@mitre.org\",\"cvss\":10.0,\"cvss-time\":\"2020-02-21T15:13:00\",\"cvss-vector\":\"AV:N/AC:L/Au:N/C:C/I:C/A:C\",\"cwe\":\"CWE-326\",\"id\":\"CVE-2013-7287\",\"impact.availability\":\"COMPLETE\",\"impact.confidentiality\":\"COMPLETE\",\"impact.integrity\":\"COMPLETE\",\"last-modified\":null,\"references\":[\"http://seclists.org/fulldisclosure/2014/Apr/21\",\"https://www.securityfocus.com/archive/1/531713\"],\"summary\":\"MobileIron VSP < 5.9.1 and Sentry < 5.0 has an insecure encryption scheme.\",\"vulnerable_configuration\":[\"cpe:2.3:a:mobileiron:sentry:*:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:virtual_smartphone_platform:*:*:*:*:*:*:*:*\"],\"vulnerable_configuration_cpe_2_2\":[],\"vulnerable_product\":[\"cpe:2.3:a:mobileiron:sentry:*:*:*:*:*:*:*:*\",\"cpe:2.3:a:mobileiron:virtual_smartphone_platform:*:*:*:*:*:*:*:*\"]}]",
        terms.get(4).toString());

    Collection<Map<String, Object>> jsons = JsonCodec.asCollection(terms.get(4).toString());

    Assert.assertEquals(3, jsons.size());
  }

  @Test
  public void testParseRegExp() {

    Var input = newVar();
    Var output = newVar();
    Literal head = new Literal("match", input, output);

    Var tmp = newVar();
    List<Literal> body = Lists.newArrayList(
        new Literal("fn_match_regex", tmp, input,
            newConst("(?m:^OPEN\\\\s+[a-zA-Z0-9]+\\\\s+BUCKET:\\\\s+.*$)")),
        new Literal("fn_to_text", output, tmp));

    Clause clause1 = new Clause(head, body);
    Clause clause2 = parseClause(
        "match(V474, V475) :- fn_match_regex(V476, V474, \"¤u0028?m¤u003a^OPEN¤u005c¤u005cs+[a-zA-Z0-9]+¤u005c¤u005cs+BUCKET¤u003a¤u005c¤u005cs+.*$¤u0029\"), fn_to_text(V475, V476)");

    Assert.assertEquals(clause1, clause2);
  }

  @Test
  public void testFunctionOutputComputedBeforeUsageInFunction() {

    Var x = newVar();
    Var y = newVar();
    Var u = newVar();

    Literal fnIsTrue = new Literal("fn_is_true", u);
    Literal fnLt = new Literal("fn_lt", u, x, y);
    Literal nodeX = new Literal("node", x);
    Literal nodeY = new Literal("node", y);
    Literal edgeXY = new Literal("edge", x, y);

    List<List<Literal>> permutations = new ArrayList<>();
    permute(new Literal[] {fnIsTrue, fnLt, nodeX, nodeY}, permutations);

    Clause expected = parseClause("edge(X, Y) :- node(X), node(Y), fn_lt(U, X, Y), fn_is_true(U).");

    for (List<Literal> body : permutations) {
      Clause actual = reorderBodyLiterals(new Clause(edgeXY, body));
      System.out.println(new Clause(edgeXY, body) + " -> " + actual);
      Assert.assertTrue(expected.isRelevant(actual));
      Assert.assertEquals("node/1", actual.body().get(0).predicate().id());
      Assert.assertEquals("node/1", actual.body().get(1).predicate().id());
      Assert.assertEquals("fn_lt/3", actual.body().get(2).predicate().id());
      Assert.assertEquals("fn_is_true/1", actual.body().get(3).predicate().id());
    }
  }

  @Test
  public void testFunctionOutputComputedBeforeUsageInNegatedRule() {

    Var x = newVar();
    Var y = newVar();
    Var u = newVar();

    Literal isFalse = new Literal("~is_false", u);
    Literal fnLt = new Literal("fn_lt", u, x, y);
    Literal nodeX = new Literal("node", x);
    Literal nodeY = new Literal("node", y);
    Literal edgeXY = new Literal("edge", x, y);

    List<List<Literal>> permutations = new ArrayList<>();
    permute(new Literal[] {isFalse, fnLt, nodeX, nodeY}, permutations);

    Clause expected = parseClause("edge(X, Y) :- node(X), node(Y), fn_lt(U, X, Y), ~is_false(U).");

    for (List<Literal> body : permutations) {
      Clause actual = reorderBodyLiterals(new Clause(edgeXY, body));
      System.out.println(new Clause(edgeXY, body) + " -> " + actual);
      Assert.assertTrue(expected.isRelevant(actual));
      Assert.assertEquals("node/1", actual.body().get(0).predicate().id());
      Assert.assertEquals("node/1", actual.body().get(1).predicate().id());
      Assert.assertEquals("fn_lt/3", actual.body().get(2).predicate().id());
      Assert.assertEquals("~is_false/1", actual.body().get(3).predicate().id());
    }
  }

  @Test
  public void testComparatorTransitivity() {

    List<String> literals = Lists.newArrayList("fn_is(V6031, V6032)",
        "fn_concat(V6032, V6033, V6019)", "fn_shadow_rwufvo2(V6019, V6021)");

    List<List<String>> permutations = new ArrayList<>();
    permute(literals, permutations);

    for (List<String> body : permutations) {

      String rule =
          String.format("convertir_identifiant_adresse_en_uex(V6021, V6031) :- %s, %s, %s.",
              body.get(0), body.get(1), body.get(2));

      Clause actual = parseClause(rule);

      Assert.assertEquals("fn_shadow_rwufvo2", actual.body().get(0).predicate().baseName());
      Assert.assertEquals("fn_concat", actual.body().get(1).predicate().baseName());
      Assert.assertEquals("fn_is", actual.body().get(2).predicate().baseName());
    }
  }

  @Test
  public void testComparatorWithMaterialization() {

    List<String> literals = Lists.newArrayList(
        "fn_shadow_azfpcdc(V5805, V5804, V5804, V5804, V5804, V5804, V5804, V5804, V5804, V5804, V5804, V5804, V5804)",
        "fn_shadow_u0cjyyy(V5806, V5807, V5807, V5807)",
        "fn_accumulo_materialize_facts(\"https¤u003a//localhost/facts/bauexp/bauexp\", \"content.text.C0\", _, V5809, \"content.text.C1\", _, _, \"content.text.C2\", _, V5807, \"content.text.C3\", _, _, \"content.text.C4\", _, V5804, \"content.text.C5\", _, _)",
        "fn_concat(V5818, V5806, V5805)", "fn_is(V5819, V5818)");

    List<List<String>> permutations = new ArrayList<>();
    permute(literals, permutations);

    for (List<String> body : permutations) {

      String rule =
          String.format("convertir_identifiant_adresse_en_uex(V5809, V5819) :- %s, %s, %s, %s, %s.",
              body.get(0), body.get(1), body.get(2), body.get(3), body.get(4));

      Clause actual = parseClause(rule);

      System.out.println(actual);

      Assert.assertEquals("fn_accumulo_materialize_facts",
          actual.body().get(0).predicate().baseName());
      Assert.assertEquals("fn_shadow_",
          actual.body().get(1).predicate().baseName().substring(0, "fn_shadow_".length()));
      Assert.assertEquals("fn_shadow_",
          actual.body().get(2).predicate().baseName().substring(0, "fn_shadow_".length()));
      Assert.assertEquals("fn_concat", actual.body().get(3).predicate().baseName());
      Assert.assertEquals("fn_is", actual.body().get(4).predicate().baseName());
    }
  }

  @Test
  public void testComparatorWithMaterialization2() {

    List<String> literals = Lists.newArrayList("convertir_identifiant_adresse_en_uex(V5752, V5753)",
        "fn_shadow_ob9ahdy(V5754, V5753, V5753, V5753, V5753, V5753, V5753, V5753, V5753, V5753, V5753, V5753, V5753, V5753, V5753, V5753)",
        "fn_shadow_4jf0yxn(V5758, V5753)", "fn_is_true(V5758)",
        "fn_shadow_pgo8nt3(V5759, V5756, V5755, V5756, V5755)",
        "fn_clickhouse_materialize_facts(\"https¤u003a//localhost/facts/bncltp/convertir_uex_en_identifiant_client\", \"xxx\", \"yyy\", \"TMP_UEX\", V5754, \"NOM_OU_RAISON_SOCIALE\", V5755, \"COMPLEMENT_RAISON_SOCIALE\", V5756, \"IDENTIFIANT_CLIENT\", V5757, \"SELECT¤u000d  tmp_bnuexp.CODUEX AS TMP_UEX¤u002c¤u000d  tmp_bnuexp.CODCLI AS IDENTIFIANT_CLIENT¤u002c¤u000d  tmp_bncltp.OCLIEN AS NOM_OU_RAISON_SOCIALE¤u002c¤u000d  tmp_bncltp.OCLICP AS COMPLEMENT_RAISON_SOCIALE¤u000dFROM¤u000d  tmp_bnuexp¤u000d  INNER JOIN tmp_bncltp ON tmp_bncltp.CODCLI ¤u003d tmp_bnuexp.CODCLI¤u000dWHERE 1 ¤u003d 1¤u000d  {TMP_UEX} AND tmp_bnuexp.CODUEX ¤u003d '¤u003aTMP_UEX'¤u000d  {NOM_OU_RAISON_SOCIALE} AND tmp_bncltp.OCLIEN ¤u003d '¤u003aNOM_OU_RAISON_SOCIALE'¤u000d  {COMPLEMENT_RAISON_SOCIALE} AND tmp_bncltp.OCLICP ¤u003d '¤u003aCOMPLEMENT_RAISON_SOCIALE'¤u000d  {IDENTIFIANT_CLIENT} AND tmp_bncltp.CODCLI ¤u003d '¤u003aIDENTIFIANT_CLIENT'\")");

    List<List<String>> permutations = new ArrayList<>();
    permute(literals, permutations);

    for (List<String> body : permutations) {

      String rule = String.format(
          "convertir_identifiant_adresse_en_uex_et_client_rapide(V5752, V5753, V5757, V5755) :- %s, %s, %s, %s, %s, %s.",
          body.get(0), body.get(1), body.get(2), body.get(3), body.get(4), body.get(5));

      Clause actual = parseClause(rule);

      System.out.println(actual);

      Assert.assertEquals("convertir_identifiant_adresse_en_uex",
          actual.body().get(0).predicate().baseName());
      Assert.assertEquals("fn_clickhouse_materialize_facts",
          actual.body().get(1).predicate().baseName());
      Assert.assertEquals("fn_shadow_",
          actual.body().get(2).predicate().baseName().substring(0, "fn_shadow_".length()));
    }
  }

  @Test
  public void testComparatorWithMaterialization3() {

    List<String> literals = Lists.newArrayList(
        "fn_clickhouse_materialize_facts(\"https://localhost/facts/tlgt-tot/tlgt_tot\", \"{{ client }}\", \"{{ env }}\", \"TMP_UEX\", TMP_UEX, \"BATIMENT\", BATIMENT, \"ESCALIER\", ESCALIER, \"ETAGE\", ETAGE, \"POSITION\", POSITION, \"EMPLACEMENT\", EMPLACEMENT, \"NOM_OCCUPANT\", NOM_OCCUPANT, \"DATE_ENTREE_OCCUPANT\", DATE_ENTREE_OCCUPANT, \"NOM_PROPRIETAIRE\", NOM_PROPRIETAIRE, \"SELECT\\n  tmp_bnuexp.CODUEX AS TMP_UEX, \\n  tmp_tlgt_tot.NBATIM AS BATIMENT, \\n tmp_tlgt_tot.NESCAL AS ESCALIER, \\n tmp_tlgt_tot.NETAGE AS ETAGE, \\n  tmp_tlgt_tot.NPOSIT AS POSITION, \\n tmp_tlgt_tot.TEMPLG AS EMPLACEMENT,\\n tmp_tlgt_tot.OOCCUP AS NOM_OCCUPANT, \\n tmp_tlgt_tot.DENTOC AS DATE_ENTREE_OCCUPANT,\\n proprio_chauffage.OPRLGT AS NOM_PROPRIETAIRE\\nFROM tmp_bnuexp\\nINNER JOIN tmp_tlgt_tot\\n ON tmp_tlgt_tot.CAGEXP = tmp_bnuexp.CAGEXP\\n  AND tmp_tlgt_tot.NUEXPL = tmp_bnuexp.NUEXPL\\nLEFT JOIN proprio_chauffage\\n ON proprio_chauffage.CAGEXP = tmp_tlgt_tot.CAGEXP\\n AND proprio_chauffage.NIDLGT = tmp_tlgt_tot.NIDLGT\\n{NOM_PROPRIETAIRE} AND proprio_chauffage.OPRLGT = ':NOM_PROPRIETAIRE'\\nWHERE 1=1\\n{TMP_UEX} AND tmp_bnuexp.CODUEX = ':TMP_UEX'\\n{BATIMENT} AND tmp_tlgt_tot.NBATIM = ':BATIMENT'\\n{ESCALIER} AND tmp_tlgt_tot.NESCAL = ':ESCALIER'\\n{ETAGE} AND tmp_tlgt_tot.NETAGE = ':ETAGE'\\n{POSITION} AND tmp_tlgt_tot.NPOSIT = ':POSITION'\\n{EMPLACEMENT} AND tmp_tlgt_tot.TEMPLG = ':EMPLACEMENT'\")",
        "denormaliser_uex(UEX, TMP_UEX)", "fn_is(ENV, \"HEAT\")");

    List<List<String>> permutations = new ArrayList<>();
    permute(literals, permutations);

    for (List<String> body : permutations) {

      String rule = String.format(
          "convertir_uex_en_logement_rapide(UEX, BATIMENT, ESCALIER, ETAGE, POSITION, EMPLACEMENT, NOM_PROPRIETAIRE, NOM_OCCUPANT, DATE_ENTREE_OCCUPANT, ENV) :- %s, %s, %s.",
          body.get(0), body.get(1), body.get(2));

      Clause actual = parseClause(rule);

      System.out.println(actual);

      Assert.assertEquals("denormaliser_uex", actual.body().get(0).predicate().baseName());
      Assert.assertEquals("fn_clickhouse_materialize_facts",
          actual.body().get(1).predicate().baseName());
      Assert.assertEquals("fn_is", actual.body().get(2).predicate().baseName());
    }
  }
}
