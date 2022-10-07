package com.computablefacts.decima.problog;

import static com.computablefacts.decima.problog.AbstractTerm.newConst;
import static com.computablefacts.decima.problog.AbstractTerm.newVar;

import com.computablefacts.asterix.BoxedType;
import com.computablefacts.nona.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.errorprone.annotations.CheckReturnValue;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A literal is a predicate and a sequence of terms, the number of which must match the predicate's arity.
 * <p>
 * Literals can also be primitives of the Nona's programming language.
 */
@CheckReturnValue
final public class Literal {

  private static final char SEPARATOR_CURRENCY_SIGN = '¤';

  private final Predicate predicate_;
  private final List<AbstractTerm> terms_;
  private final List<Literal> functions_; // a sequence of functions to execute
  private final BigDecimal probability_;

  private String id_ = null;
  private String tag_ = null;
  private Boolean isGrounded_ = null;
  private Boolean isSemiGrounded_ = null;

  public Literal(String predicate, AbstractTerm... terms) {
    this(BigDecimal.ONE, predicate, Lists.newArrayList(terms));
  }

  public Literal(String predicate, List<AbstractTerm> terms) {
    this(BigDecimal.ONE, predicate, terms);
  }

  public Literal(BigDecimal probability, String predicate, AbstractTerm... terms) {
    this(probability, predicate, Lists.newArrayList(terms));
  }

  public Literal(BigDecimal probability, String predicate, List<AbstractTerm> terms) {
    this(probability, predicate, terms, new ArrayList<>());
  }

  public Literal(BigDecimal probability, String predicate, List<AbstractTerm> terms, List<Literal> functions) {

    Preconditions.checkArgument(probability.doubleValue() >= 0.0 && probability.doubleValue() <= 1.0,
        "probability should be in [0.0, 1.0] : %s", probability);
    Preconditions.checkNotNull(predicate, "predicate should not be null");
    Preconditions.checkNotNull(terms, "terms should not be null");
    Preconditions.checkNotNull(functions, "functions should not be null");

    probability_ = probability.stripTrailingZeros();
    predicate_ = new Predicate(predicate, terms.size());
    functions_ = new ArrayList<>(functions);
    terms_ = new ArrayList<>(terms);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof Literal)) {
      return false;
    }
    Literal literal = (Literal) obj;
    return probability_.compareTo(literal.probability_) == 0 && tag().equals(literal.tag());
  }

  @Override
  public int hashCode() {
    return Objects.hash(probability(), tag());
  }

  @Override
  public String toString() {

    StringBuilder builder = new StringBuilder();

    if (BigDecimal.ONE.compareTo(probability_) != 0) {
      builder.append(probability());
      builder.append("::");
    }

    builder.append(predicate_.toString());
    builder.append('(');

    for (int i = 0; i < terms_.size(); i++) {
      if (i > 0) {
        builder.append(", ");
      }

      AbstractTerm term = terms_.get(i);

      if (!term.isConst()) {
        builder.append(term);
      } else {
        builder.append('"');
        builder.append(Parser.wrap(term.toString()));
        builder.append('"');
      }
    }

    builder.append(')');
    return builder.toString();
  }

  /**
   * Compute the negated version of the current literal.
   *
   * @return negated literal.
   */
  public Literal negate() {
    if (predicate_.isNegated()) {
      return BigDecimal.ZERO.compareTo(probability_) == 0 || BigDecimal.ONE.compareTo(probability_) == 0 ? new Literal(
          BigDecimal.ONE, predicate_.baseName(), terms_, functions_)
          : new Literal(BigDecimal.ONE.subtract(probability_), predicate_.baseName(), terms_, functions_);
    }
    return BigDecimal.ZERO.compareTo(probability_) == 0 || BigDecimal.ONE.compareTo(probability_) == 0 ? new Literal(
        BigDecimal.ONE, "~" + predicate_.baseName(), terms_, functions_)
        : new Literal(BigDecimal.ONE.subtract(probability_), "~" + predicate_.baseName(), terms_, functions_);
  }

  /**
   * Get the current literal probability.
   *
   * @return probability.
   */
  public BigDecimal probability() {
    return probability_.stripTrailingZeros();
  }

  /**
   * A literal's id is used by a clause when creating its id. The id's encoding ensures that two literals are
   * structurally the same if they have the same id.
   *
   * @return an identifier.
   */
  public String id() {
    if (id_ == null) {

      List<String> list = new ArrayList<>(terms_.size() + 1 /* probability */ + 1 /* predicate */);
      list.add(probability().toString());
      list.add(predicate_.id());
      list.addAll(terms_.stream().map(AbstractTerm::id).collect(Collectors.toList()));

      id_ = Joiner.on(':').join(list);
    }
    return id_;
  }

  /**
   * Literal tag. Two literal's tags are the same if there is a one-to-one mapping of variables to variables, such that
   * when the mapping is applied to one literal, the result is a literal that is the same as the other one, when
   * compared using structural equality. The tag is used as a key by the subgoal table.
   *
   * @return a tag.
   */
  public String tag() {
    if (tag_ == null) {

      List<String> list = new ArrayList<>(terms_.size() + 1 /* predicate */);
      list.add(predicate_.id());
      list.addAll(terms_.stream().map(AbstractTerm::tag).collect(Collectors.toList()));

      tag_ = Joiner.on(':').join(list);
    }
    return tag_;
  }

  /**
   * Literal predicate.
   *
   * @return the current literal predicate.
   */
  public Predicate predicate() {
    return predicate_;
  }

  /**
   * Literal terms.
   *
   * @return the current literal terms.
   */
  public List<AbstractTerm> terms() {
    return terms_;
  }

  /**
   * Check if the current literal is grounded i.e. all terms are constants.
   *
   * @return true iif the current literal is grounded.
   */
  public boolean isGrounded() {
    if (isGrounded_ == null) {
      for (AbstractTerm term : terms_) {
        if (!term.isConst()) {
          isGrounded_ = false;
          return isGrounded_;
        }
      }
      isGrounded_ = true;
    }
    return isGrounded_;
  }

  /**
   * Check if the current literal is semi-grounded i.e. all terms are either constants or wildcards.
   *
   * @return true iif the current literal is semi-grounded.
   */
  public boolean isSemiGrounded() {
    if (isSemiGrounded_ == null) {
      for (AbstractTerm term : terms_) {
        if (!term.isConst()) {
          if (!term.isWildcard()) {
            isSemiGrounded_ = false;
            return isSemiGrounded_;
          }
        }
      }
      isSemiGrounded_ = true;
    }
    return isSemiGrounded_;
  }

  /**
   * Check if the current literal contains a given term.
   *
   * @param term term.
   * @return true iif the current literal contains the given term.
   */
  boolean hasTerm(AbstractTerm term) {

    Preconditions.checkNotNull(term, "term should not be null");

    for (AbstractTerm t : terms_) {
      if (term.equals(t)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Check if two literals can be unified.
   *
   * @param literal literal.
   * @return true iif the two literals can be unified.
   */
  public boolean isRelevant(Literal literal) {

    Preconditions.checkNotNull(literal, "literal should not be null");

    if (!predicate().id().equals(literal.predicate().id())) {
      return false;
    }
    for (int i = 0; i < terms_.size(); i++) {

      AbstractTerm t1 = terms_.get(i);
      AbstractTerm t2 = literal.terms_.get(i);

      if (t1.isConst() && t2.isConst()) {
        if (!t1.equals(t2)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Shuffle creates an environment in which all variables are mapped to freshly generated variables.
   *
   * @param env environment.
   * @return an environment.
   */
  Map<Var, AbstractTerm> shuffle(Map<Var, AbstractTerm> env) {

    Map<Var, AbstractTerm> e = env == null ? new HashMap<>() : env;

    for (AbstractTerm term : terms_) {
      if (!term.isConst()) {
        if (!e.containsKey(term)) {
          e.put((Var) term, newVar(term.isWildcard()));
        }
      }
    }
    return e;
  }

  /**
   * Rename a literal using an environment generated by shuffle.
   *
   * @return a new literal with new variable names.
   */
  Literal rename() {
    return subst(shuffle(null));
  }

  /**
   * Substitute variable terms according to a given environment.
   *
   * @param env an environment is a map from variables to terms.
   * @return a new literal.
   */
  Literal subst(Map<Var, AbstractTerm> env) {

    if (env == null || env.isEmpty()) {
      return this;
    }

    List<AbstractTerm> terms = new ArrayList<>();

    for (AbstractTerm term : terms_) {
      terms.add(term.subst(env));
    }
    return new Literal(probability_, predicate_.name(), terms, functions_);
  }

  /**
   * Unify two literals.
   *
   * @param literal literal.
   * @return the result is either an environment or null. Null is returned when the two literals cannot be unified. When
   * they can, applying the substitutions defined by the environment on both literals will create two literals that are
   * structurally equal.
   */
  public Map<Var, AbstractTerm> unify(Literal literal) {

    Preconditions.checkNotNull(literal, "literal should not be null");

    if (!predicate_.equals(literal.predicate_)) {
      return null;
    }

    Preconditions.checkState(terms_.size() == literal.terms_.size(),
        "terms_.size() should be equal to literal.terms_.size()");

    @com.google.errorprone.annotations.Var Map<Var, AbstractTerm> env = new HashMap<>();

    for (int i = 0; i < terms_.size(); i++) {

      AbstractTerm t1 = terms_.get(i).chase(env);
      AbstractTerm t2 = literal.terms_.get(i).chase(env);

      if (!t1.equals(t2)) {
        env = t1.unify(t2, env);
        if (env == null) {
          return env;
        }
      }
    }
    return env;
  }

  /**
   * Execute a function.
   *
   * @param definitions functions definitions.
   * @return a fact or possibly null if it is a pass/fail function.
   */
  public Iterator<Literal> execute(Map<String, Function> definitions) {

    Preconditions.checkNotNull(definitions, "definitions should not be null");
    Preconditions.checkState(predicate_.isPrimitive(), "the current literal should be a primitive");

    boolean isFirstTermVariable = !terms_.get(0).isConst();

    if (!isFirstTermVariable) {

      String predicate = predicate_.name().toUpperCase();

      if (predicate.equals("FN_IS_TRUE")) {

        Preconditions.checkState(terms_.size() == 1, "IS_TRUE does not produce an output");

        String bool = ((Const) terms_.get(0)).value().toString();

        return Boolean.parseBoolean(bool) ? Lists.newArrayList(
            new Literal(probability_, predicate_.name(), newConst(true))).iterator() : null;
      }

      if (predicate.equals("FN_IS_FALSE")) {

        Preconditions.checkState(terms_.size() == 1, "IS_FALSE does not produce an output");

        String bool = ((Const) terms_.get(0)).value().toString();

        return !Boolean.parseBoolean(bool) ? Lists.newArrayList(
            new Literal(probability_, predicate_.name(), newConst(false))).iterator() : null;
      }

      if (predicate.endsWith("_MATERIALIZE_FACTS")) {

        Function function = compile2();
        BoxedType<?> result = function.evaluate(definitions);

        return result == null ? null : result.isCollection() ? ((Collection<Literal>) result.asCollection()).iterator()
            : result.value() instanceof Iterator ? (Iterator<Literal>) result.value() : null;
      }
    }

    Preconditions.checkState(isValidPrimitive(), "Literal is not a valid primitive : %s", this);

    Function function = compile();
    BoxedType<?> result = function.evaluate(definitions);
    boolean isValid = result != null && (result.isString() || result.isNumber() || result.isBoolean() || result.isDate()
        || result.isCollection() || result.isMap());

    if (!isValid) {
      Preconditions.checkState(isValid,
          "The only return types allowed are String, Number, Boolean, Date, Collection and Map : %s(\n  %s\n)",
          function.name(), function.parameters().stream().map(Function::name).collect(Collectors.joining("\n  , ")));
    }
    if (!isFirstTermVariable) { // => FN_IS()

      AbstractTerm first = terms_.get(0);
      Comparable<?> object = (Comparable<?>) ((Const) first).value();
      BoxedType<?> boxedType = BoxedType.create(object);

      if (!boxedType.equals(result)) {
        return null;
      }
    }

    List<Object> parameters = terms_.stream().skip(1).collect(Collectors.toList());
    parameters.add(0, result.value());

    return Lists.newArrayList(new Literal(probability_, predicate_.name(),
        parameters.stream().map(AbstractTerm::newConst).collect(Collectors.toList()))).iterator();
  }

  private boolean isValidPrimitive() {

    if (!predicate().isPrimitive()) {
      return false;
    }

    for (int i = 1 /* The first term is always a variable */; i < terms_.size(); i++) {

      AbstractTerm term = terms_.get(i);

      if (!term.isConst()) {
        return false; // All terms must be grounded before delegating work to a function
      }

      Object value = ((Const) term).value();

      if (!(value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Date
          || value instanceof Collection || value instanceof Map)) {
        return false; // The only types allowed are String, Number, Boolean, Date, Collection And
        // Map
      }
    }
    return true;
  }

  private List<AbstractTerm> functionVariables() {

    List<AbstractTerm> functionOutputs = new ArrayList<>();
    List<AbstractTerm> functionVariables = new ArrayList<>();

    for (Literal literal : functions_) {
      for (int i = 0; i < literal.terms().size(); i++) {

        AbstractTerm term = literal.terms().get(i);

        if (i == 0) { // The first term of a function is always the function output
          functionOutputs.add(term);
        } else {
          if (!term.isConst()) {
            if (!functionOutputs.contains(term)) {
              functionVariables.add(term);
            }
          }
        }
      }
    }

    // The first term is the output of the whole
    functionVariables.add(0, functions_.get(functions_.size() - 1).terms().get(0));

    return functionVariables;
  }

  private String mergeFunctions() {

    // The functions_ class member is a list such as :
    //
    // <fn_lt(U, X, 0), fn_gt(V, Y, 0), fn_and(W, U, V), fn_if(O, W, 1, 0)>
    //
    // where all terms are const but the first of each function.

    List<AbstractTerm> functionVariables = functionVariables();

    // Compute the mapping between the current literal terms and the functions parameters
    Map<AbstractTerm, AbstractTerm> functionVariablesMapping = new HashMap<>();

    for (int i = 0; i < terms_.size(); i++) {
      functionVariablesMapping.put(functionVariables.get(i), terms_.get(i));
    }

    // Merge all functions together to create a single one :
    //
    // fn_if(O, fn_and(fn_lt(X, 0), fn_gt(Y, 0)), 1, 0)
    //
    // where all terms are const but the first one.

    // Compute the mapping between the intermediary variables and the partial functions
    Map<AbstractTerm, String> functionOutputsMapping = new HashMap<>();

    for (int i = 0; i < functions_.size(); i++) {

      List<AbstractTerm> terms = functions_.get(i).terms();
      String fnName = functions_.get(i).predicate().name().toUpperCase();
      List<String> fnParams = new ArrayList<>(terms.size() - 1);

      for (int k = 1 /* The first term is always a variable */; k < terms.size(); k++) {

        AbstractTerm term = terms.get(k);

        if (term.isConst()) {

          Object value = ((Const) term).value();

          if (value instanceof String) {
            fnParams.add(Function.wrap((String) value));
          } else {
            fnParams.add(value.toString());
          }
        } else if (functionVariablesMapping.containsKey(term)) {

          Object value = ((Const) functionVariablesMapping.get(term)).value();

          if (value instanceof String) {
            fnParams.add(Function.wrap((String) value));
          } else {
            fnParams.add(value.toString());
          }
        } else if (functionOutputsMapping.containsKey(term)) {
          fnParams.add(functionOutputsMapping.get(term));
        } else {
          Preconditions.checkState(false, "Invalid state : %s", fnName);
        }
      }

      String fn = fnName + "(" + Joiner.on(", ").join(fnParams) + ")";

      functionOutputsMapping.put(terms.get(0), fn);
    }

    // Get the function that is able to fill the literal first term (variable)
    return functionOutputsMapping.get(functionVariables.get(0));
  }

  private Function compile() {
    if (functions_.size() <= 0) {
      String function = predicate_.name().toUpperCase() + "(" + terms_.stream().skip(1)
          .map(term -> Function.wrap(((Const) term).value().toString())).collect(Collectors.joining(", ")) + ")";
      return new Function(function);
    }
    return new Function(mergeFunctions());
  }

  private Function compile2() {
    if (functions_.size() <= 0) {
      String function = predicate_.name().toUpperCase() + "(" + terms_.stream().map(term -> {
        if (term.isConst()) {
          return Function.wrap(((Const) term).value().toString());
        }
        return Function.wrap("_");
      }).collect(Collectors.joining(", ")) + ")";
      return new Function(function);
    }
    return new Function(mergeFunctions());
  }
}
