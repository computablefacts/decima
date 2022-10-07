package com.computablefacts.decima.problog;

import static com.computablefacts.decima.problog.AbstractTerm.newConst;
import static com.computablefacts.decima.problog.AbstractTerm.newVar;

import com.computablefacts.asterix.RandomString;
import com.computablefacts.nona.Function;
import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@CheckReturnValue
public final class Parser {

  public static String wrap(String text) {

    String newText = text.trim();

    if (newText.startsWith("b64_(") && newText.endsWith(")")) {
      return text;
    }
    if (CharMatcher.anyOf(newText).matchesAnyOf("\r\n\\=:,\")(")) {
      return "b64" + Function.wrap(text);
    }
    return text;
  }

  public static String unwrap(String text) {

    String newText = text.trim();

    if (newText.startsWith("b64_(") && newText.endsWith(")")) {
      return Function.unwrap(newText.substring(3));
    }
    return text;
  }

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

    Preconditions.checkState(scan.ttype == '?', "[line " + scan.lineno() + "] Expected '?' after query");

    return head;
  }

  private static Clause parseClause(Reader reader) throws IOException {

    Preconditions.checkNotNull(reader, "reader should not be null");

    return reorderBodyLiterals(parseClause(tokenizer(reader)));
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

      Preconditions.checkState(scan.nextToken() == '-', "[line " + scan.lineno() + "] Expected ':-'");

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

    Preconditions.checkState(lhs != null, "[line " + scan.lineno() + "] Predicate or start of expression expected");

    scan.nextToken();

    if (scan.ttype == StreamTokenizer.TT_WORD || scan.ttype == '=' || scan.ttype == '!' || scan.ttype == '<'
        || scan.ttype == '>') {

      Preconditions.checkState(!negated, "[line " + scan.lineno() + "] Built-in should not be negated");

      scan.pushBack();
      return parseBuiltInPredicate(scan, map, lhs);
    }

    Preconditions.checkState(!builtInExpected, "[line " + scan.lineno() + "] Unexpected built-in predicate");
    Preconditions.checkState(scan.ttype == '(',
        "[line " + scan.lineno() + "] Expected '(' after predicate or an operator");

    @Var List<AbstractTerm> terms = new ArrayList<>();

    if (scan.nextToken() != ')') {

      scan.pushBack();

      if (lhs.startsWith("fn_")) {

        @Var List<Literal> literals = new ArrayList<>();
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
          terms.add(newConst(unwrap(scan.sval)));
        } else {
          Preconditions.checkState(false, "[line " + scan.lineno() + "] Expected term in expression");
        }
      } while (scan.nextToken() == ',');

      Preconditions.checkState(scan.ttype == ')',
          "[line " + scan.lineno() + "] Expected ')', terms = [" + Joiner.on(',').join(terms) + "]");
    }
    return list(new Literal(probability, negated ? "~" + lhs : lhs, terms));
  }

  /**
   * Parse functions. Internally, functions are expanded as a sequence of one ore more {@link Literal}. For example, the
   * function :
   *
   * <pre>
   * fn_if(O, fn_and(fn_lt(X, 0), fn_gt(Y, 0)), 1, 0)
   * </pre>
   * <p>
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
      Map<String, com.computablefacts.decima.problog.Var> map, List<Literal> literals) throws IOException {

    Preconditions.checkNotNull(lhs, "lhs should not be null");
    Preconditions.checkArgument(lhs.startsWith("fn_"), "lhs should be a function");
    Preconditions.checkNotNull(scan, "scan should not be null");
    Preconditions.checkNotNull(map, "map should not be null");

    List<AbstractTerm> terms = new ArrayList<>();

    do {
      if (scan.nextToken() == StreamTokenizer.TT_WORD) {

        AbstractTerm term = stringToVarOrConst(map, numberOrString(scan));

        if (term.isConst() && term.toString().startsWith("fn_")) {

          Preconditions.checkState(scan.nextToken() == '(', "invalid function usage : %s", term);

          List<AbstractTerm> tmp = parseFunction(term.toString(), scan, map, literals);
          String name = term.toString();

          com.computablefacts.decima.problog.Var var = newVar();
          tmp.add(0, var);
          Literal literal = new Literal(name, tmp);
          literals.add(literal);
          terms.add(var);
        } else {
          terms.add(term);
        }
      } else if (scan.ttype == '"' || scan.ttype == '\'') {
        terms.add(newConst(unwrap(scan.sval)));
      } else {
        Preconditions.checkState(false, "[line " + scan.lineno() + "] Expected term in expression");
      }
    } while (scan.nextToken() == ',');

    Preconditions.checkState(scan.ttype == ')', "[line " + scan.lineno() + "] Expected ')'");

    return terms;
  }

  /**
   * Parses one of the built-in predicates. Internally, built-in predicates are expanded as two {@link Literal}. For
   * example, the built-in predicate :
   *
   * <pre>
   * X > Y
   * </pre>
   * <p>
   * will be expanded as :
   *
   * <pre>
   *     fn_gt(U, X, Y), fn_is_true(U)
   * </pre>
   *
   * @param scan
   * @param map
   * @param lhs
   * @return
   * @throws IOException
   */
  private static List<Literal> parseBuiltInPredicate(StreamTokenizer scan,
      Map<String, com.computablefacts.decima.problog.Var> map, String lhs) throws IOException {

    Preconditions.checkNotNull(scan, "scan should not be null");
    Preconditions.checkNotNull(map, "map should not be null");
    Preconditions.checkNotNull(lhs, "lhs should not be null");

    // Extract operator
    @Var String operator;
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

    @Var boolean isNegative = false;
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

    Preconditions.checkState(op != null, "[line " + scan.lineno() + "] Invalid operator '" + operator + "'");

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

    Preconditions.checkState(rhs != null, "[line " + scan.lineno() + "] Right hand side of expression expected");

    // Materialize built-in

    if (op.equals("fn_is")) {

      AbstractTerm dest = stringToVarOrConst(map, lhs);
      AbstractTerm src = stringToVarOrConst(map, rhs);

      Preconditions.checkState(!dest.isConst(), "[line " + scan.lineno() + "] It is forbidden to assign to a constant");

      Literal lit = new Literal(op, dest, src);
      return list(lit);
    }

    com.computablefacts.decima.problog.Var var = newVar();

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
  private static AbstractTerm stringToVarOrConst(Map<String, com.computablefacts.decima.problog.Var> map, String name) {
    if (isWildcard(name)) {
      return newVar(true);
    }
    if (isVar(name)) {
      if (!map.containsKey(name)) {
        map.put(name, newVar());
      }
      return map.get(name);
    }
    return newConst(name);
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

    // Reset standard comments
    tokenizer.slashSlashComments(false);
    tokenizer.slashStarComments(false);

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

        @Var boolean hasDecimalPart = true;

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

  static Clause reorderBodyLiterals(Clause clause) {

    if (clause == null) {
      return null;
    }

    List<Literal> body = new ArrayList<>(clause.body());

    // Reorder the rule body literals to :
    // - Ensure the output of one primitive is not used before it is computed
    // - Ensure the parameter of one primitive is grounded before the primitive is executed
    // - Ensure negated literals are grounded
    Comparator<Literal> comparator = comparator();
    List<List<Integer>> constraints = new ArrayList<>();

    for (int i = 0; i < body.size(); i++) {
      constraints.add(new ArrayList<>());
    }
    for (int i = 0; i < body.size(); i++) {

      Literal literal = body.get(i);

      for (int j = i + 1; j < body.size(); j++) {

        int cmp = comparator.compare(literal, body.get(j));
        if (cmp > 0) {
          constraints.get(j).add(i);
        } else if (cmp < 0) {
          constraints.get(i).add(j);
        }
      }
    }

    // constraints contains for each body literal the set of clauses it must be positioned after
    boolean[][] adjacency = new boolean[body.size()][body.size()];

    for (int i = 0; i < constraints.size(); i++) {
      for (int j = 0; j < constraints.get(i).size(); j++) {
        adjacency[constraints.get(i).get(j)][i] = true;
      }
    }

    Graph<Literal> graph = new Graph<>(body, adjacency);
    List<Literal> list = graph.topoSort();

    Preconditions.checkState(list != null, "rule has cycles : %s", clause);

    return new Clause(clause.head(), list);
  }

  /**
   * This comparator IS NOT transitive.
   */
  static Comparator<Literal> comparator() {
    return (p1, p2) -> {

      boolean p1IsNegated = p1.predicate().isNegated();
      boolean p2IsNegated = p2.predicate().isNegated();

      boolean p1IsPrimitive = p1.predicate().isPrimitive();
      boolean p2IsPrimitive = p2.predicate().isPrimitive();

      boolean p1IsMaterialization = p1.predicate().baseName().endsWith("_materialize_facts");
      boolean p2IsMaterialization = p2.predicate().baseName().endsWith("_materialize_facts");

      boolean p1IsTrueOrIsFalse =
          p1.predicate().baseName().equals("fn_is_true") || p1.predicate().baseName().equals("fn_is_false");
      boolean p2IsTrueOrIsFalse =
          p2.predicate().baseName().equals("fn_is_true") || p2.predicate().baseName().equals("fn_is_false");

      Preconditions.checkState(!p1IsMaterialization || p1IsPrimitive, "materializations must be primitives : %s", p1);
      Preconditions.checkState(!p2IsMaterialization || p2IsPrimitive, "materializations must be primitives : %s", p2);
      Preconditions.checkState(!p1IsPrimitive || !p1IsNegated, "primitives cannot be negated : %s", p1);
      Preconditions.checkState(!p2IsPrimitive || !p2IsNegated, "primitives cannot be negated : %s", p2);

      // For each literal, find variables that must be grounded before the resolution begins
      Set<AbstractTerm> p1Provides = new HashSet<>();
      Set<AbstractTerm> p2Provides = new HashSet<>();

      Set<AbstractTerm> p1Needs = new HashSet<>();
      Set<AbstractTerm> p2Needs = new HashSet<>();

      if (p1IsMaterialization) {
        AbstractTerm head = p1.terms().get(0);
        if (!head.isConst()) {
          p1Needs.add(head);
        }
        p1Provides.addAll(p1.terms().stream().skip(1).filter(t -> !t.isConst()).collect(Collectors.toList()));
      } else if (p1IsPrimitive) {
        p1Needs.addAll(p1.terms().stream().skip(p1IsTrueOrIsFalse ? 0 : 1 /* result stored in first term */)
            .filter(t -> !t.isConst()).collect(Collectors.toList()));
        if (!p1IsTrueOrIsFalse) {
          p1Provides.add(p1.terms().get(0));
        }
      } else if (p1IsNegated) {
        p1Needs.addAll(p1.terms().stream().filter(t -> !t.isConst()).collect(Collectors.toList()));
      } else {
        p1Provides.addAll(p1.terms().stream().filter(t -> !t.isConst()).collect(Collectors.toList()));
      }
      if (p2IsMaterialization) {
        AbstractTerm head = p2.terms().get(0);
        if (!head.isConst()) {
          p2Needs.add(head);
        }
        p2Provides.addAll(p2.terms().stream().skip(1).filter(t -> !t.isConst()).collect(Collectors.toList()));
      } else if (p2IsPrimitive) {
        p2Needs.addAll(p2.terms().stream().skip(p2IsTrueOrIsFalse ? 0 : 1 /* result stored in first term */)
            .filter(t -> !t.isConst()).collect(Collectors.toList()));
        if (!p2IsTrueOrIsFalse) {
          p2Provides.add(p2.terms().get(0));
        }
      } else if (p2IsNegated) {
        p2Needs.addAll(p2.terms().stream().filter(t -> !t.isConst()).collect(Collectors.toList()));
      } else {
        p2Provides.addAll(p2.terms().stream().filter(t -> !t.isConst()).collect(Collectors.toList()));
      }

      // If both literals do not share variables, the order of theses two body literals does not
      // matter
      Set<AbstractTerm> p1DependsOnP2 = Sets.intersection(p1Needs, p2Provides);
      Set<AbstractTerm> p2DependsOnP1 = Sets.intersection(p2Needs, p1Provides);

      if (p1DependsOnP2.isEmpty() && p2DependsOnP1.isEmpty()) {
        return 0; // the order does not matter
      }

      Preconditions.checkState(p1DependsOnP2.isEmpty() || p2DependsOnP1.isEmpty(),
          "cyclic dependency found : %s <-> %s", p1, p2);

      if (p1DependsOnP2.isEmpty()) {
        return -1; // p1 can provide a grounded term to p2
      }
      return 1; // p2 can provide a grounded term to p1
    };
  }

  private static class Graph<T> {

    List<T> vertices_;
    boolean[][] adjacency_;
    int numVertices_;

    public Graph(List<T> s, boolean[][] adjacency) {
      vertices_ = new ArrayList<>(s);
      numVertices_ = vertices_.size();
      adjacency_ = adjacency;
    }

    public List<T> topoSort() {

      List<T> result = new ArrayList<>();
      List<Integer> todo = new LinkedList<>();

      for (int i = 0; i < numVertices_; i++) {
        todo.add(i);
      }
      try {
        outer:
        while (!todo.isEmpty()) {
          for (Integer r : todo) {
            if (!hasDependency(r, todo)) {
              todo.remove(r);
              result.add(vertices_.get(r));
              // no need to worry about concurrent modification
              continue outer;
            }
          }
          throw new Exception("Graph has cycles");
        }
      } catch (Exception e) {
        System.out.println(e);
        return null;
      }
      return result;
    }

    private boolean hasDependency(Integer r, List<Integer> todo) {
      for (Integer c : todo) {
        if (adjacency_[r][c]) {
          return true;
        }
      }
      return false;
    }
  }
}
