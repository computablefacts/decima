/**
 * Copyright (c) 2011-2019 MNCC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * @author http://www.mncc.fr
 */
package com.computablefacts.decima.problog;

import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.computablefacts.decima.utils.RandomString;
import com.computablefacts.nona.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;

@CheckReturnValue
public final class Parser {

  public static Set<Literal> parseQueries(String string) {

    Preconditions.checkNotNull(string, "string should not be null");

    try {

      Set<Literal> literals = new HashSet<>();

      for (String line : Splitter.on('\n').trimResults().omitEmptyStrings().split(string)) {

        Literal literal = parseQuery(new StringReader(line));

        if (literal != null) {
          literals.add(literal);
        }
      }
      return literals;
    } catch (IOException e) {
      // TODO
    }
    return null;
  }

  public static Literal parseQuery(String string) {

    Preconditions.checkNotNull(string, "string should not be null");

    try {
      return parseQuery(new StringReader(string));
    } catch (IOException e) {
      // TODO
    }
    return null;
  }

  public static Set<Clause> parseClauses(String string) {

    Preconditions.checkNotNull(string, "string should not be null");

    try {

      Set<Clause> clauses = new HashSet<>();

      for (String line : Splitter.on('\n').trimResults().omitEmptyStrings().split(string)) {

        Clause clause = parseClause(new StringReader(line));

        if (clause != null) {
          clauses.add(clause);
        }
      }
      return clauses;
    } catch (IOException e) {
      // TODO
    }
    return null;
  }

  public static Clause parseClause(String string) {

    Preconditions.checkNotNull(string, "string should not be null");

    try {
      return parseClause(new StringReader(string));
    } catch (IOException e) {
      // TODO
    }
    return null;
  }

  private static Literal parseQuery(Reader reader) throws IOException {

    Preconditions.checkNotNull(reader, "reader should not be null");

    return parseQuery(tokenizer(reader));
  }

  private static Literal parseQuery(StreamTokenizer scan) throws IOException {

    Preconditions.checkNotNull(scan, "scan should not be null");

    Map<String, com.computablefacts.decima.problog.Var> map = new HashMap<>();

    Literal head = parseLiteral(BigDecimal.ONE, scan, map).get(0);

    scan.nextToken();

    Preconditions.checkState(scan.ttype == '?',
        "[line " + scan.lineno() + "] Expected '?' after query");

    return head;
  }

  private static Clause parseClause(Reader reader) throws IOException {

    Preconditions.checkNotNull(reader, "reader should not be null");

    return parseClause(tokenizer(reader));
  }

  private static Clause parseClause(StreamTokenizer scan) throws IOException {

    Preconditions.checkNotNull(scan, "scan should not be null");

    Map<String, com.computablefacts.decima.problog.Var> map = new HashMap<>();
    List<Literal> goals = new ArrayList<>();

    BigDecimal probability = parseProbability(scan);

    if (scan.ttype == StreamTokenizer.TT_EOF) { // commented line
      return null;
    }

    Literal head = parseLiteral(probability, scan, map).get(0);

    if (scan.nextToken() == ':') { // Rule

      Preconditions.checkState(scan.nextToken() == '-',
          "[line " + scan.lineno() + "] Expected ':-'");

      List<Literal> body = new ArrayList<>();

      do {
        body.addAll(parseLiteral(BigDecimal.ONE, scan, map));
      } while (scan.nextToken() == ',');

      // Preconditions.checkState(scan.ttype == '.',
      // "[line " + scan.lineno() + "] Expected '.' after rule");

      return new Clause(head, body);
    }

    if (scan.ttype == '.') { // Fact

      Clause fact = new Clause(head);

      Preconditions.checkState(fact.isFact(), fact + " is not a valid fact");

      return fact;
    }

    // Query
    goals.clear();
    goals.add(head);

    Preconditions.checkState(scan.ttype == '.' || scan.ttype == '?' || scan.ttype == ',',
        "[line " + scan.lineno() + "] Expected one of '.', ',' or '?' after fact/query expression");

    while (scan.ttype == ',') {
      goals.addAll(parseLiteral(BigDecimal.ONE, scan, map));
      scan.nextToken();
    }

    if (scan.ttype == '?') {
      return new Clause(goals.get(0));
    }

    Preconditions.checkState(false, "[line " + scan.lineno() + "] Expected '?' after query");

    return null;
  }

  private static BigDecimal parseProbability(StreamTokenizer scan) throws IOException {

    Preconditions.checkNotNull(scan, "scan should not be null");

    scan.nextToken();

    String number = number(scan);

    if (number == null) {
      scan.pushBack();
      return BigDecimal.ONE;
    }

    scan.nextToken();

    Preconditions.checkState(scan.ttype == ':',
        "[line " + scan.lineno() + "] Missing ':' between probability and predicate");

    scan.nextToken();

    Preconditions.checkState(scan.ttype == ':',
        "[line " + scan.lineno() + "] Missing ':' between probability and predicate");

    return new BigDecimal(number);
  }

  private static List<Literal> parseLiteral(BigDecimal probability, StreamTokenizer scan,
      Map<String, com.computablefacts.decima.problog.Var> map) throws IOException {

    Preconditions.checkNotNull(probability, "probability should not be null");
    Preconditions.checkNotNull(scan, "scan should not be null");
    Preconditions.checkNotNull(map, "map should not be null");

    // Extract negation (if any)
    scan.nextToken();
    boolean negated;

    if (scan.ttype == '~') {
      negated = true;
      scan.nextToken();
    } else if (scan.ttype == '\\') {

      scan.nextToken();

      if (scan.ttype == '+') {
        negated = true;
        scan.nextToken();
      } else {
        scan.pushBack();
        negated = false;
      }
    } else {
      negated = false;
    }

    // Extract left hand side
    String lhs;
    boolean builtInExpected;

    if (scan.ttype == StreamTokenizer.TT_WORD) {

      String number = number(scan);

      if (number == null) {
        lhs = scan.sval;
        builtInExpected = false;
      } else {
        lhs = number;
        builtInExpected = true;
      }
    } else if (scan.ttype == '"' || scan.ttype == '\'') {
      lhs = scan.sval;
      builtInExpected = true;
    } else {
      lhs = null;
      builtInExpected = false;
    }

    Preconditions.checkState(lhs != null,
        "[line " + scan.lineno() + "] Predicate or start of expression expected");

    scan.nextToken();

    if (scan.ttype == StreamTokenizer.TT_WORD || scan.ttype == '=' || scan.ttype == '!'
        || scan.ttype == '<' || scan.ttype == '>') {

      Preconditions.checkState(!negated,
          "[line " + scan.lineno() + "] Built-in should not be negated");

      scan.pushBack();
      return parseBuiltInPredicate(scan, map, lhs);
    }

    Preconditions.checkState(!builtInExpected,
        "[line " + scan.lineno() + "] Unexpected built-in predicate");
    Preconditions.checkState(scan.ttype == '(',
        "[line " + scan.lineno() + "] Expected '(' after predicate or an operator");

    @Var
    List<AbstractTerm> terms = new ArrayList<>();

    if (scan.nextToken() != ')') {

      scan.pushBack();

      if (lhs.startsWith("fn_")) {

        @Var
        List<Literal> literals = new ArrayList<>();
        terms = parseFunction(lhs, scan, map, literals);
        Literal lit = new Literal(probability, negated ? "~" + lhs : lhs, terms);
        literals.add(lit);

        if (literals.size() == 1) {
          return literals;
        }

        if (lhs.startsWith("fn_is")) {
          if (literals.size() == 2) {
            return literals;
          }
          literals = literals.subList(0, literals.size() - 1);
        }

        List<AbstractTerm> outputTerms = new ArrayList<>();
        terms = new ArrayList<>();

        for (Literal literal : literals) {
          for (int i = 0; i < literal.terms().size(); i++) {

            AbstractTerm term = literal.terms().get(i);

            if (i == 0) {
              outputTerms.add(term);
            } else {
              if (!term.isConst()) {
                if (!outputTerms.contains(term)) {
                  terms.add(term);
                }
              }
            }
          }
        }

        terms.add(0, literals.get(literals.size() - 1).terms().get(0));

        // Create a fake synthetic term
        RandomString rnd = new RandomString(7);
        String predicate = "fn_shadow_" + rnd.nextString().toLowerCase();

        List<Literal> newLiterals = new ArrayList<>();
        newLiterals.add(new Literal(probability, predicate, terms, literals));

        if (lhs.startsWith("fn_is")) {
          newLiterals.add(lit);
        }
        return newLiterals;
      }

      do {
        if (scan.nextToken() == StreamTokenizer.TT_WORD) {
          terms.add(stringToVarOrConst(map, numberOrString(scan)));
        } else if (scan.ttype == '"' || scan.ttype == '\'') {
          terms.add(new Const(scan.sval));
        } else {
          Preconditions.checkState(false,
              "[line " + scan.lineno() + "] Expected term in expression");
        }
      } while (scan.nextToken() == ',');

      Preconditions.checkState(scan.ttype == ')', "[line " + scan.lineno() + "] Expected ')'");
    }
    return list(new Literal(probability, negated ? "~" + lhs : lhs, terms));
  }

  /**
   * Parse functions. Internally, functions are expanded as a sequence of one ore more
   * {@link Literal}. For example, the function :
   *
   * <pre>
   * fn_if(O, fn_and(fn_lt(X, 0), fn_gt(Y, 0)), 1, 0)
   * </pre>
   *
   * will be expanded as :
   *
   * <pre>
   * fn_lt(U, X, 0), fn_gt(V, Y, 0), fn_and(W, U, V), fn_if(O, W, 1, 0)
   * </pre>
   *
   * @param lhs
   * @param scan
   * @param map
   * @param literals
   * @return
   * @throws IOException
   */
  private static List<AbstractTerm> parseFunction(String lhs, StreamTokenizer scan,
                                                  Map<String, com.computablefacts.decima.problog.Var> map, List<Literal> literals)
      throws IOException {

    Preconditions.checkNotNull(lhs, "lhs should not be null");
    Preconditions.checkArgument(lhs.startsWith("fn_"), "lhs should be a function");
    Preconditions.checkNotNull(scan, "scan should not be null");
    Preconditions.checkNotNull(map, "map should not be null");

    List<AbstractTerm> terms = new ArrayList<>();

    do {
      if (scan.nextToken() == StreamTokenizer.TT_WORD) {

        AbstractTerm term = stringToVarOrConst(map, numberOrString(scan));

        if (term.isConst() && ((String) ((Const) term).value()).startsWith("fn_")) {

          Preconditions.checkState(scan.nextToken() == '(', "invalid function usage : %s", term);

          List<AbstractTerm> tmp = parseFunction((String) ((Const) term).value(), scan, map, literals);
          String name = (String) ((Const) term).value();
          String function = name + "(" + Joiner.on(",").join(tmp) + ")";

          Preconditions.checkState(new Function(function) != null, "invalid function : %s",
              function);

          com.computablefacts.decima.problog.Var var = new com.computablefacts.decima.problog.Var();
          tmp.add(0, var);
          Literal literal = new Literal(name, tmp);
          literals.add(literal);
          terms.add(var);
        } else {
          terms.add(term);
        }
      } else if (scan.ttype == '"' || scan.ttype == '\'') {
        terms.add(new Const(scan.sval));
      } else {
        Preconditions.checkState(false, "[line " + scan.lineno() + "] Expected term in expression");
      }
    } while (scan.nextToken() == ',');

    Preconditions.checkState(scan.ttype == ')', "[line " + scan.lineno() + "] Expected ')'");

    return terms;
  }

  /**
   * Parses one of the built-in predicates. Internally, built-in predicates are expanded as two
   * {@link Literal}. For example, the built-in predicate :
   *
   * <pre>
   * X > Y
   * </pre>
   *
   * will be expanded as :
   *
   * <pre>
   *     fn_gt(U, X, Y), fn_is_true(U)
   * </pre>
   *
   * @param scan
   * @param map
   * @param lhs
   * @throws IOException
   * @return
   */
  private static List<Literal> parseBuiltInPredicate(StreamTokenizer scan,
      Map<String, com.computablefacts.decima.problog.Var> map, String lhs) throws IOException {

    Preconditions.checkNotNull(scan, "scan should not be null");
    Preconditions.checkNotNull(map, "map should not be null");
    Preconditions.checkNotNull(lhs, "lhs should not be null");

    // Extract operator
    @Var
    String operator;
    scan.nextToken();

    if (scan.ttype == StreamTokenizer.TT_WORD) {
      operator = scan.sval;
    } else {

      operator = Character.toString((char) scan.ttype);
      scan.nextToken();

      if (scan.ttype == '=' || scan.ttype == '>' || scan.ttype == '<') {
        operator += Character.toString((char) scan.ttype);
      } else {
        scan.pushBack();
      }
    }

    @Var
    boolean isNegative = false;
    String op;

    if ("=".equals(operator)) {
      op = "fn_eq";
    } else if ("!=".equals(operator) || "<>".equals(operator)) {
      op = "fn_eq";
      isNegative = true;
    } else if ("<".equals(operator)) {
      op = "fn_lt";
    } else if ("<=".equals(operator) || "=<".equals(operator)) {
      op = "fn_lte";
    } else if (">".equals(operator)) {
      op = "fn_gt";
    } else if (">=".equals(operator) || "=>".equals(operator)) {
      op = "fn_gte";
    } else if ("is".equals(operator)) {
      op = "fn_is";
    } else {
      op = null;
    }

    Preconditions.checkState(op != null,
        "[line " + scan.lineno() + "] Invalid operator '" + operator + "'");

    // Extract operator right hand side
    String rhs;
    scan.nextToken();

    if (scan.ttype == StreamTokenizer.TT_WORD) {
      rhs = numberOrString(scan);
    } else if (scan.ttype == '"' || scan.ttype == '\'') {
      rhs = scan.sval;
    } else {
      rhs = null;
    }

    Preconditions.checkState(rhs != null,
        "[line " + scan.lineno() + "] Right hand side of expression expected");

    // Materialize built-in

    if (op.equals("fn_is")) {

      AbstractTerm dest = stringToVarOrConst(map, lhs);
      AbstractTerm src = stringToVarOrConst(map, rhs);

      Preconditions.checkState(!dest.isConst(),
          "[line " + scan.lineno() + "] It is forbidden to assign to a constant");

      Literal lit = new Literal(op, dest, src);
      return list(lit);
    }

    com.computablefacts.decima.problog.Var var = new com.computablefacts.decima.problog.Var();

    Literal lit1 = new Literal(op, var, stringToVarOrConst(map, lhs), stringToVarOrConst(map, rhs));
    Literal lit2 = isNegative ? new Literal("fn_is_false", var) : new Literal("fn_is_true", var);

    return list(lit1, lit2);
  }

  /**
   * Create a new {@link AbstractTerm} from a string.
   *
   * @param map
   * @param name
   * @return
   */
  private static AbstractTerm stringToVarOrConst(Map<String, com.computablefacts.decima.problog.Var> map,
                                                 String name) {
    if (isWildcard(name)) {
      return new com.computablefacts.decima.problog.Var(true);
    }
    if (isVar(name)) {
      if (!map.containsKey(name)) {
        map.put(name, new com.computablefacts.decima.problog.Var());
      }
      return map.get(name);
    }
    return new Const(name);
  }

  /**
   * Check if a string is a wildcard '_' variable.
   *
   * @param name
   * @return
   */
  private static boolean isWildcard(String name) {

    Preconditions.checkNotNull(name, "name should not be null");

    return name.equals("_");
  }

  /**
   * Check if a string is a variable.
   *
   * @param name
   * @return
   */
  private static boolean isVar(String name) {

    Preconditions.checkNotNull(name, "name should not be null");

    return name.length() > 0 && Character.isUpperCase(name.codePointAt(0));
  }

  /**
   * Initialize a specific tokenizer for our Datalog flavor.
   *
   * @param reader
   * @return
   */
  private static StreamTokenizer tokenizer(Reader reader) {

    Preconditions.checkNotNull(reader, "reader should not be null");

    StreamTokenizer tokenizer = new StreamTokenizer(reader);
    tokenizer.resetSyntax();

    tokenizer.wordChars('a', 'z');
    tokenizer.wordChars('A', 'Z');
    tokenizer.wordChars(128 + 32, 255);
    tokenizer.whitespaceChars(0, ' ');

    // '.' looks like a number to StreamTokenizer by default
    tokenizer.ordinaryChar('.');

    // Recognize numbers as words (we need to deal with BigDecimal)
    tokenizer.wordChars('0', '9');

    // '_' is allowed in predicate names
    tokenizer.wordChars('_', '_');

    // Prolog-style % comments
    tokenizer.commentChar('%');

    // Quoting characters
    tokenizer.quoteChar('"');
    tokenizer.quoteChar('\'');

    return tokenizer;
  }

  private static String number(StreamTokenizer scan) throws IOException {

    Preconditions.checkNotNull(scan, "scan should not be null");

    if (scan.ttype != StreamTokenizer.TT_WORD) {
      return null; // not a number
    }

    for (int i = 0; i < scan.sval.length(); i++) {
      if (!Character.isDigit((int) scan.sval.charAt(i))) {
        return null; // not a number
      }
    }

    StringBuilder builder = new StringBuilder(scan.sval.length());
    builder.append(scan.sval); // sval = integer part of a number
    scan.nextToken();

    if (scan.ttype != '.') {
      scan.pushBack();
    } else {

      scan.nextToken();

      if (scan.ttype == StreamTokenizer.TT_EOF) {
        scan.pushBack(); // The stream ends with a number
      } else {

        @Var
        boolean hasDecimalPart = true;

        for (int i = 0; i < scan.sval.length(); i++) {
          if (!Character.isDigit((int) scan.sval.charAt(i))) {
            hasDecimalPart = false;
            break;
          }
        }

        if (!hasDecimalPart) {
          scan.pushBack();
        } else {
          builder.append('.'); // dot
          builder.append(scan.sval); // decimal part
        }
      }
    }
    return builder.toString();
  }

  private static String numberOrString(StreamTokenizer scan) throws IOException {

    Preconditions.checkNotNull(scan, "scan should not be null");

    String number = number(scan);
    return number == null ? scan.sval : number;
  }

  private static List<Literal> list(Literal literal) {

    Preconditions.checkNotNull(literal, "literal should not be null");

    List<Literal> list = new ArrayList<>();
    list.add(literal);

    return list;
  }

  private static List<Literal> list(Literal lit1, Literal lit2) {

    Preconditions.checkNotNull(lit1, "lit1 should not be null");
    Preconditions.checkNotNull(lit2, "lit2 should not be null");

    List<Literal> list = new ArrayList<>();
    list.add(lit1);
    list.add(lit2);

    return list;
  }
}
