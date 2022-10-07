package com.computablefacts.decima.problog;

import static com.computablefacts.decima.problog.AbstractTerm.newConst;
import static com.computablefacts.decima.problog.AbstractTerm.newVar;
import static com.computablefacts.decima.problog.Parser.parseClause;
import static com.computablefacts.decima.problog.Parser.reorderBodyLiterals;
import static com.computablefacts.decima.problog.TestUtils.permute;

import com.computablefacts.asterix.codecs.JsonCodec;
import com.google.common.collect.Lists;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

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

    Clause rule = parseClause("under_and_above(X, Y) :- fn_if(O, fn_and(fn_lt(X, 0), fn_gt(Y, 0)), 1, 0).");

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
        "json_path(\"jhWTAETz\", \"data\", \"9\", \"rawOutput\", \"b64_(W3siTW9kaWZpZWQiOiIyMDIwLTA3LTA3VDEyOjI0OjAwIiwiUHVibGlzaGVkIjoxNTk0MDg4MTAwMDAwLCJhY2Nlc3MuYXV0aGVudGljYXRpb24iOiJOT05FIiwiYWNjZXNzLmNvbXBsZXhpdHkiOiJMT1ciLCJhY2Nlc3MudmVjdG9yIjoiTkVUV09SSyIsImFzc2lnbmVyIjoiY3ZlQG1pdHJlLm9yZyIsImN2c3MiOjcuNSwiY3Zzcy10aW1lIjpudWxsLCJjdnNzLXZlY3RvciI6bnVsbCwiY3dlIjoiTlZELUNXRS1ub2luZm8iLCJpZCI6IkNWRS0yMDIwLTE1NTA1IiwiaW1wYWN0LmF2YWlsYWJpbGl0eSI6IlBBUlRJQUwiLCJpbXBhY3QuY29uZmlkZW50aWFsaXR5IjoiUEFSVElBTCIsImltcGFjdC5pbnRlZ3JpdHkiOiJQQVJUSUFMIiwibGFzdC1tb2RpZmllZCI6IjIwMjAtMDktMThUMTY6MTU6MDAiLCJyZWZlcmVuY2VzIjpbImh0dHBzOi8vd3d3Lm1vYmlsZWlyb24uY29tL2VuL2Jsb2cvbW9iaWxlaXJvbi1zZWN1cml0eS11cGRhdGVzLWF2YWlsYWJsZSJdLCJzdW1tYXJ5IjoiQSByZW1vdGUgY29kZSBleGVjdXRpb24gdnVsbmVyYWJpbGl0eSBpbiBNb2JpbGVJcm9uIENvcmUgJiBDb25uZWN0b3IgdmVyc2lvbnMgMTAuMy4wLjMgYW5kIGVhcmxpZXIsIDEwLjQuMC4wLCAxMC40LjAuMSwgMTAuNC4wLjIsIDEwLjQuMC4zLCAxMC41LjEuMCwgMTAuNS4yLjAgYW5kIDEwLjYuMC4wOyBhbmQgU2VudHJ5IHZlcnNpb25zIDkuNy4yIGFuZCBlYXJsaWVyLCBhbmQgOS44LjA7IGFuZCBNb25pdG9yIGFuZCBSZXBvcnRpbmcgRGF0YWJhc2UgKFJEQikgdmVyc2lvbiAyLjAuMC4xIGFuZCBlYXJsaWVyIHRoYXQgYWxsb3dzIHJlbW90ZSBhdHRhY2tlcnMgdG8gZXhlY3V0ZSBhcmJpdHJhcnkgY29kZSB2aWEgdW5zcGVjaWZpZWQgdmVjdG9ycy4iLCJ2dWxuZXJhYmxlX2NvbmZpZ3VyYXRpb24iOlsiY3BlOjIuMzphOm1vYmlsZWlyb246Y2xvdWQ6LToqOio6KjoqOio6KjoqIiwiY3BlOjIuMzphOm1vYmlsZWlyb246Y2xvdWQ6MTAuNjoqOio6KjoqOio6KjoqIiwiY3BlOjIuMzphOm1vYmlsZWlyb246Y29yZTotOio6KjoqOio6KjoqOioiLCJjcGU6Mi4zOmE6bW9iaWxlaXJvbjpjb3JlOjEwLjY6KjoqOio6KjoqOio6KiIsImNwZToyLjM6YTptb2JpbGVpcm9uOmVudGVycHJpc2VfY29ubmVjdG9yOi06KjoqOio6KjoqOio6KiIsImNwZToyLjM6YTptb2JpbGVpcm9uOmVudGVycHJpc2VfY29ubmVjdG9yOjEwLjY6KjoqOio6KjoqOio6KiIsImNwZToyLjM6YTptb2JpbGVpcm9uOnJlcG9ydGluZ19kYXRhYmFzZTotOio6KjoqOio6KjoqOioiLCJjcGU6Mi4zOmE6bW9iaWxlaXJvbjpyZXBvcnRpbmdfZGF0YWJhc2U6MTAuNjoqOio6KjoqOio6KjoqIiwiY3BlOjIuMzphOm1vYmlsZWlyb246c2VudHJ5Oi06KjoqOio6KjoqOio6KiIsImNwZToyLjM6YTptb2JpbGVpcm9uOnNlbnRyeTo5Ljg6KjoqOio6KjoqOio6KiJdLCJ2dWxuZXJhYmxlX2NvbmZpZ3VyYXRpb25fY3BlXzJfMiI6W10sInZ1bG5lcmFibGVfcHJvZHVjdCI6WyJjcGU6Mi4zOmE6bW9iaWxlaXJvbjpjbG91ZDotOio6KjoqOio6KjoqOioiLCJjcGU6Mi4zOmE6bW9iaWxlaXJvbjpjbG91ZDoxMC42Oio6KjoqOio6KjoqOioiLCJjcGU6Mi4zOmE6bW9iaWxlaXJvbjpjb3JlOi06KjoqOio6KjoqOio6KiIsImNwZToyLjM6YTptb2JpbGVpcm9uOmNvcmU6MTAuNjoqOio6KjoqOio6KjoqIiwiY3BlOjIuMzphOm1vYmlsZWlyb246ZW50ZXJwcmlzZV9jb25uZWN0b3I6LToqOio6KjoqOio6KjoqIiwiY3BlOjIuMzphOm1vYmlsZWlyb246ZW50ZXJwcmlzZV9jb25uZWN0b3I6MTAuNjoqOio6KjoqOio6KjoqIiwiY3BlOjIuMzphOm1vYmlsZWlyb246cmVwb3J0aW5nX2RhdGFiYXNlOi06KjoqOio6KjoqOio6KiIsImNwZToyLjM6YTptb2JpbGVpcm9uOnJlcG9ydGluZ19kYXRhYmFzZToxMC42Oio6KjoqOio6KjoqOioiLCJjcGU6Mi4zOmE6bW9iaWxlaXJvbjpzZW50cnk6LToqOio6KjoqOio6KjoqIiwiY3BlOjIuMzphOm1vYmlsZWlyb246c2VudHJ5OjkuODoqOio6KjoqOio6KjoqIl19LHsiTW9kaWZpZWQiOiIyMDIwLTA3LTA3VDEyOjI0OjAwIiwiUHVibGlzaGVkIjoxNTk0MDg4MTAwMDAwLCJhY2Nlc3MuYXV0aGVudGljYXRpb24iOiJOT05FIiwiYWNjZXNzLmNvbXBsZXhpdHkiOiJMT1ciLCJhY2Nlc3MudmVjdG9yIjoiTkVUV09SSyIsImFzc2lnbmVyIjoiY3ZlQG1pdHJlLm9yZyIsImN2c3MiOjcuNSwiY3Zzcy10aW1lIjpudWxsLCJjdnNzLXZlY3RvciI6bnVsbCwiY3dlIjoiQ1dFLTI4NyIsImlkIjoiQ1ZFLTIwMjAtMTU1MDYiLCJpbXBhY3QuYXZhaWxhYmlsaXR5IjoiUEFSVElBTCIsImltcGFjdC5jb25maWRlbnRpYWxpdHkiOiJQQVJUSUFMIiwiaW1wYWN0LmludGVncml0eSI6IlBBUlRJQUwiLCJsYXN0LW1vZGlmaWVkIjoiMjAyMC0wOS0xOFQxNzoxNTowMCIsInJlZmVyZW5jZXMiOlsiaHR0cHM6Ly93d3cubW9iaWxlaXJvbi5jb20vZW4vYmxvZy9tb2JpbGVpcm9uLXNlY3VyaXR5LXVwZGF0ZXMtYXZhaWxhYmxlIl0sInN1bW1hcnkiOiJBbiBhdXRoZW50aWNhdGlvbiBieXBhc3MgdnVsbmVyYWJpbGl0eSBpbiBNb2JpbGVJcm9uIENvcmUgJiBDb25uZWN0b3IgdmVyc2lvbnMgMTAuMy4wLjMgYW5kIGVhcmxpZXIsIDEwLjQuMC4wLCAxMC40LjAuMSwgMTAuNC4wLjIsIDEwLjQuMC4zLCAxMC41LjEuMCwgMTAuNS4yLjAgYW5kIDEwLjYuMC4wIHRoYXQgYWxsb3dzIHJlbW90ZSBhdHRhY2tlcnMgdG8gYnlwYXNzIGF1dGhlbnRpY2F0aW9uIG1lY2hhbmlzbXMgdmlhIHVuc3BlY2lmaWVkIHZlY3RvcnMuIiwidnVsbmVyYWJsZV9jb25maWd1cmF0aW9uIjpbImNwZToyLjM6YTptb2JpbGVpcm9uOmNsb3VkOi06KjoqOio6KjoqOio6KiIsImNwZToyLjM6YTptb2JpbGVpcm9uOmNsb3VkOjEwLjY6KjoqOio6KjoqOio6KiIsImNwZToyLjM6YTptb2JpbGVpcm9uOmNvcmU6LToqOio6KjoqOio6KjoqIiwiY3BlOjIuMzphOm1vYmlsZWlyb246Y29yZToxMC42Oio6KjoqOio6KjoqOioiLCJjcGU6Mi4zOmE6bW9iaWxlaXJvbjplbnRlcnByaXNlX2Nvbm5lY3RvcjotOio6KjoqOio6KjoqOioiLCJjcGU6Mi4zOmE6bW9iaWxlaXJvbjplbnRlcnByaXNlX2Nvbm5lY3RvcjoxMC42Oio6KjoqOio6KjoqOioiLCJjcGU6Mi4zOmE6bW9iaWxlaXJvbjpyZXBvcnRpbmdfZGF0YWJhc2U6LToqOio6KjoqOio6KjoqIiwiY3BlOjIuMzphOm1vYmlsZWlyb246cmVwb3J0aW5nX2RhdGFiYXNlOjEwLjY6KjoqOio6KjoqOio6KiIsImNwZToyLjM6YTptb2JpbGVpcm9uOnNlbnRyeTotOio6KjoqOio6KjoqOioiLCJjcGU6Mi4zOmE6bW9iaWxlaXJvbjpzZW50cnk6OS44Oio6KjoqOio6KjoqOioiLCJjcGU6Mi4zOmE6bW9iaWxlaXJvbjpzZW50cnk6MTAuNjoqOio6KjoqOio6KjoqIl0sInZ1bG5lcmFibGVfY29uZmlndXJhdGlvbl9jcGVfMl8yIjpbXSwidnVsbmVyYWJsZV9wcm9kdWN0IjpbImNwZToyLjM6YTptb2JpbGVpcm9uOmNsb3VkOi06KjoqOio6KjoqOio6KiIsImNwZToyLjM6YTptb2JpbGVpcm9uOmNsb3VkOjEwLjY6KjoqOio6KjoqOio6KiIsImNwZToyLjM6YTptb2JpbGVpcm9uOmNvcmU6LToqOio6KjoqOio6KjoqIiwiY3BlOjIuMzphOm1vYmlsZWlyb246Y29yZToxMC42Oio6KjoqOio6KjoqOioiLCJjcGU6Mi4zOmE6bW9iaWxlaXJvbjplbnRlcnByaXNlX2Nvbm5lY3RvcjotOio6KjoqOio6KjoqOioiLCJjcGU6Mi4zOmE6bW9iaWxlaXJvbjplbnRlcnByaXNlX2Nvbm5lY3RvcjoxMC42Oio6KjoqOio6KjoqOioiLCJjcGU6Mi4zOmE6bW9iaWxlaXJvbjpyZXBvcnRpbmdfZGF0YWJhc2U6LToqOio6KjoqOio6KjoqIiwiY3BlOjIuMzphOm1vYmlsZWlyb246cmVwb3J0aW5nX2RhdGFiYXNlOjEwLjY6KjoqOio6KjoqOio6KiIsImNwZToyLjM6YTptb2JpbGVpcm9uOnNlbnRyeTotOio6KjoqOio6KjoqOioiLCJjcGU6Mi4zOmE6bW9iaWxlaXJvbjpzZW50cnk6OS44Oio6KjoqOio6KjoqOioiLCJjcGU6Mi4zOmE6bW9iaWxlaXJvbjpzZW50cnk6MTAuNjoqOio6KjoqOio6KjoqIl19LHsiTW9kaWZpZWQiOiIyMDIwLTAyLTIxVDE1OjEzOjAwIiwiUHVibGlzaGVkIjoxNTgxNjM1NzAwMDAwLCJhY2Nlc3MuYXV0aGVudGljYXRpb24iOiJOT05FIiwiYWNjZXNzLmNvbXBsZXhpdHkiOiJMT1ciLCJhY2Nlc3MudmVjdG9yIjoiTkVUV09SSyIsImFzc2lnbmVyIjoiY3ZlQG1pdHJlLm9yZyIsImN2c3MiOjEwLjAsImN2c3MtdGltZSI6IjIwMjAtMDItMjFUMTU6MTM6MDAiLCJjdnNzLXZlY3RvciI6IkFWOk4vQUM6TC9BdTpOL0M6Qy9JOkMvQTpDIiwiY3dlIjoiQ1dFLTMyNiIsImlkIjoiQ1ZFLTIwMTMtNzI4NyIsImltcGFjdC5hdmFpbGFiaWxpdHkiOiJDT01QTEVURSIsImltcGFjdC5jb25maWRlbnRpYWxpdHkiOiJDT01QTEVURSIsImltcGFjdC5pbnRlZ3JpdHkiOiJDT01QTEVURSIsImxhc3QtbW9kaWZpZWQiOm51bGwsInJlZmVyZW5jZXMiOlsiaHR0cDovL3NlY2xpc3RzLm9yZy9mdWxsZGlzY2xvc3VyZS8yMDE0L0Fwci8yMSIsImh0dHBzOi8vd3d3LnNlY3VyaXR5Zm9jdXMuY29tL2FyY2hpdmUvMS81MzE3MTMiXSwic3VtbWFyeSI6Ik1vYmlsZUlyb24gVlNQIDwgNS45LjEgYW5kIFNlbnRyeSA8IDUuMCBoYXMgYW4gaW5zZWN1cmUgZW5jcnlwdGlvbiBzY2hlbWUuIiwidnVsbmVyYWJsZV9jb25maWd1cmF0aW9uIjpbImNwZToyLjM6YTptb2JpbGVpcm9uOnNlbnRyeToqOio6KjoqOio6KjoqOioiLCJjcGU6Mi4zOmE6bW9iaWxlaXJvbjp2aXJ0dWFsX3NtYXJ0cGhvbmVfcGxhdGZvcm06KjoqOio6KjoqOio6KjoqIl0sInZ1bG5lcmFibGVfY29uZmlndXJhdGlvbl9jcGVfMl8yIjpbXSwidnVsbmVyYWJsZV9wcm9kdWN0IjpbImNwZToyLjM6YTptb2JpbGVpcm9uOnNlbnRyeToqOio6KjoqOio6KjoqOioiLCJjcGU6Mi4zOmE6bW9iaWxlaXJvbjp2aXJ0dWFsX3NtYXJ0cGhvbmVfcGxhdGZvcm06KjoqOio6KjoqOio6KjoqIl19XQ==)\").");

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
        new Literal("fn_match_regex", tmp, input, newConst("(?m:^OPEN\\\\s+[a-zA-Z0-9]+\\\\s+BUCKET:\\\\s+.*$)")),
        new Literal("fn_to_text", output, tmp));

    Clause clause1 = new Clause(head, body);
    Clause clause2 = parseClause(
        "match(V474, V475) :- fn_match_regex(V476, V474, \"b64_(KD9tOl5PUEVOXFxzK1thLXpBLVowLTldK1xccytCVUNLRVQ6XFxzKy4qJCk=)\"), fn_to_text(V475, V476)");

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
    permute(new Literal[]{fnIsTrue, fnLt, nodeX, nodeY}, permutations);

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
    permute(new Literal[]{isFalse, fnLt, nodeX, nodeY}, permutations);

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

    List<String> literals = Lists.newArrayList("fn_is(V6031, V6032)", "fn_concat(V6032,  \"random\", V6019)",
        "fn_shadow_rwufvo2(V6019, V6021)");

    List<List<String>> permutations = new ArrayList<>();
    permute(literals, permutations);

    for (List<String> body : permutations) {

      String rule = String.format("convertir_identifiant_adresse_en_uex(V6021, V6031) :- %s, %s, %s.", body.get(0),
          body.get(1), body.get(2));

      Clause actual = parseClause(rule);

      Assert.assertEquals("fn_shadow_rwufvo2", actual.body().get(0).predicate().baseName());
      Assert.assertEquals("fn_concat", actual.body().get(1).predicate().baseName());
      Assert.assertEquals("fn_is", actual.body().get(2).predicate().baseName());
    }
  }

  @Test
  public void testWrapUnwrap() {
    Assert.assertEquals("\n\r\t)(:=", Parser.unwrap(Parser.wrap(Parser.wrap(Parser.wrap("\n\r\t)(:=")))));
  }

  @Test(expected = IllegalStateException.class)
  public void testTopoSortDetectsSimpleCycles() {
    Clause clause = parseClause("has_cycle(X, Y) :- fn_get_node(X, Z, Y), fn_get_node(Y, Z, X).");
  }

  @Test
  public void testTopoSortWithMaterializationsAndShadowRules() {

    Clause clause = parseClause(
        "infos_geometry(PARCELLE_ID, COORDINATE_UNWRAPPED) :- load_parcelle(JSON), fn_get(PARCELLE_ID, JSON, parcelleId), fn_get(COORDINATES, fn_get(JSON, geometry), coordinates), fn_materialize_facts(COORDINATES, COORDINATE), fn_materialize_facts(COORDINATE, COORDINATE_UNWRAPPED).");

    Assert.assertTrue(clause.body().get(0).isRelevant(new Literal("load_parcelle", newVar())));
    Assert.assertTrue(
        clause.body().get(1).isRelevant(new Literal("fn_get", newVar(), newVar(), newConst("parcelleId"))));
    Assert.assertTrue(clause.body().get(2).predicate().baseName().startsWith("fn_shadow_"));
    Assert.assertTrue(clause.body().get(3).isRelevant(new Literal("fn_materialize_facts", newVar(), newVar())));
    Assert.assertTrue(clause.body().get(4).isRelevant(new Literal("fn_materialize_facts", newVar(), newVar())));
  }
}
