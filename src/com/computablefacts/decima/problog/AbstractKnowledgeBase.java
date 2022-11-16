package com.computablefacts.decima.problog;

import static com.computablefacts.decima.problog.AbstractTerm.newConst;
import static com.computablefacts.decima.problog.Parser.reorderBodyLiterals;

import com.computablefacts.asterix.BoxedType;
import com.computablefacts.asterix.RandomString;
import com.computablefacts.asterix.View;
import com.computablefacts.asterix.codecs.Base64Codec;
import com.computablefacts.asterix.codecs.JsonCodec;
import com.computablefacts.decima.Builder;
import com.computablefacts.decima.robdd.Pair;
import com.computablefacts.logfmt.LogFormatter;
import com.computablefacts.nona.Function;
import com.computablefacts.nona.functions.comparisonoperators.Equal;
import com.computablefacts.nona.functions.csvoperators.CsvValue;
import com.computablefacts.nona.functions.stringoperators.StrLength;
import com.computablefacts.nona.functions.stringoperators.ToInteger;
import com.computablefacts.nona.functions.stringoperators.ToLowerCase;
import com.computablefacts.nona.functions.stringoperators.ToUpperCase;
import com.computablefacts.nona.types.Csv;
import com.google.common.annotations.Beta;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class allows us to be agnostic from the storage layer. It is used to assert facts and rules.
 */
@CheckReturnValue
public abstract class AbstractKnowledgeBase {

  private static final Logger logger_ = LoggerFactory.getLogger(AbstractKnowledgeBase.class);

  private final RandomString randomString_ = new RandomString(7);
  private final Map<String, Function> definitions_ = new ConcurrentHashMap<>();

  public AbstractKnowledgeBase() {
    setDefinitions();
  }

  @Override
  public String toString() {

    StringBuilder builder = new StringBuilder();
    Iterator<Clause> facts = facts();
    Iterator<Clause> rules = rules();

    while (facts.hasNext()) {
      builder.append(facts.next().toString());
      builder.append(".\n");
    }
    while (rules.hasNext()) {
      builder.append(rules.next().toString());
      builder.append(".\n");
    }
    return builder.toString();
  }

  /**
   * Add a new fact or rule to the database. There are two assumptions here :
   *
   * <ul>
   * <li>facts and rules predicates must not overlap ;</li>
   * <li>a rule body literals should not have probabilities attached.</li>
   * </ul>
   *
   * @param clause fact or rule.
   */
  public void azzert(Clause clause) {

    Preconditions.checkNotNull(clause, "clause should not be null");
    Preconditions.checkArgument(clause.isSafe(), "clause should be safe : %s", clause);

    if (clause.head().predicate().isPrimitive()) {
      return; // Ignore assertions for primitives
    }

    Clause newClause;

    if (clause.isFact()) {
      newClause = clause;
    } else {

      // Remove probability from the rule head (otherwise it is a no-op)
      Pair<Clause, Clause> clauses = rewriteRuleHead(clause);

      if (clauses.u != null) {
        azzert(clauses.u); // Assert created fact (if any)
      }

      newClause = clauses.t; // Assert rewritten rule
    }

    Literal head = newClause.head();
    BigDecimal probability = head.probability();

    Preconditions.checkState(!BigDecimal.ZERO.equals(probability), "head probability must be != 0.0 : %s", newClause);

    if (clause.isFact()) {
      azzertFact(newClause);
    } else {

      Preconditions.checkState(BigDecimal.ONE.equals(probability),
          "rule head should not have a probability attached : %s", newClause);

      for (int i = 0; i < newClause.body().size(); i++) {

        Literal literal = newClause.body().get(i);

        Preconditions.checkState(BigDecimal.ONE.equals(literal.probability()),
            "body literals should not have probabilities attached : %s", newClause);
      }

      azzertRule(newClause);
    }
  }

  /**
   * Adds new facts or rules to the database.
   *
   * @param clauses clauses.
   */
  public void azzert(Set<Clause> clauses) {

    Preconditions.checkNotNull(clauses, "clauses should not be null");

    clauses.forEach(this::azzert);
  }

  protected abstract void azzertFact(@NotNull Clause fact);

  protected abstract void azzertRule(@NotNull Clause rule);

  protected abstract Iterator<Clause> facts(@NotNull Literal literal);

  protected abstract Iterator<Clause> rules(@NotNull Literal literal);

  public abstract Iterator<Clause> facts();

  public abstract Iterator<Clause> rules();

  public long nbFacts(@NotNull Literal literal) {
    return Iterators.size(facts(literal));
  }

  public long nbRules(@NotNull Literal literal) {
    return Iterators.size(rules(literal));
  }

  public long nbFacts() {
    return Iterators.size(facts());
  }

  public long nbRules() {
    return Iterators.size(rules());
  }

  @Beta
  public List<Clause> compact() {

    // Find all rules that do not reference another rule (if any)
    Map.Entry<List<Clause>, List<Clause>> rules = View.of(rules()).divide(rule -> rule.body().stream()
        .noneMatch(literal -> View.of(rules()).map(Clause::head).anyMatch(head -> head.isRelevant(literal))));
    List<Clause> dontReferenceOtherRules = rules.getKey();
    List<Clause> referenceOtherRules = rules.getValue();

    // Inline all rules that are not referenced by another rule
    List<Clause> newRules = referenceOtherRules.stream().flatMap(rule -> {

      @Var List<Clause> list = Lists.newArrayList(rule);

      while (true) {

        List<Clause> newList = new ArrayList<>();

        for (int i = 0; newList.isEmpty() && i < list.size(); i++) {

          Clause clause = list.get(i);

          for (int j = 0; newList.isEmpty() && j < clause.body().size(); j++) {

            int pos = j;
            Literal literal = clause.body().get(pos);

            newList.addAll(dontReferenceOtherRules.stream().filter(r -> r.head().isRelevant(literal)).map(or -> {

              Clause referencingRule = clause.rename();
              Clause referencedRule = or.rename();
              Clause newClause = mergeRules(referencingRule, referencedRule, pos);

              return reorderBodyLiterals(newClause);
            }).collect(Collectors.toList()));
          }
        }
        if (newList.isEmpty()) {
          break;
        }
        list = newList;
      }
      return list.stream();
    }).collect(Collectors.toList());

    newRules.addAll(dontReferenceOtherRules);
    return newRules;
  }

  /**
   * Return the list of available definitions for primitives.
   *
   * @return list of definitions.
   */
  public Map<String, Function> definitions() {
    return definitions_;
  }

  /**
   * Set the list of available primitives.
   */
  protected void setDefinitions() {

    Map<String, Function> defs = Function.definitions();

    for (Map.Entry<String, Function> def : defs.entrySet()) {
      definitions_.put("FN_" + def.getKey(), def.getValue());
    }

    // TODO : legacy functions. Remove ASAP.
    definitions_.put("FN_EQ", new Equal());
    definitions_.put("FN_CSV_VALUE", new CsvValue());
    definitions_.put("FN_LOWER_CASE", new ToLowerCase());
    definitions_.put("FN_UPPER_CASE", new ToUpperCase());
    definitions_.put("FN_INT", new ToInteger());
    definitions_.put("FN_LENGTH", new StrLength());

    // [DEPRECATED] Special operator. Allow KB modification at runtime.
    definitions_.put("FN_ASSERT_JSON", new Function("ASSERT_JSON") {

      @Override
      protected boolean isCacheable() {

        // The function's cache is shared between multiple processes
        return false;
      }

      @Override
      public BoxedType<?> evaluate(List<BoxedType<?>> parameters) {

        Preconditions.checkArgument(parameters.size() == 2, "ASSERT_JSON takes exactly two parameters.");
        Preconditions.checkArgument(parameters.get(0).isString(), "%s should be a string", parameters.get(0));
        Preconditions.checkArgument(parameters.get(1).isCollection() || parameters.get(1).isMap(),
            "%s should be a json array or object", parameters.get(1));

        String uuid = parameters.get(0).asString();
        List<?> jsons = parameters.get(1).isMap() ? Lists.newArrayList(parameters.get(1).asMap())
            : Lists.newArrayList(parameters.get(1).asCollection());

        for (int i = 0; i < jsons.size(); i++) {

          String json = JsonCodec.asString(jsons.get(i));

          azzert(Builder.json(uuid, Integer.toString(i, 10), json));
          azzert(Builder.jsonPaths(uuid, Integer.toString(i, 10), json));
        }
        return BoxedType.create(true);
      }
    });

    // [DEPRECATED] Special operator. Allow KB modification at runtime.
    definitions_.put("FN_ASSERT_CSV", new Function("ASSERT_CSV") {

      @Override
      protected boolean isCacheable() {

        // The function's cache is shared between multiple processes
        return false;
      }

      @Override
      public BoxedType<?> evaluate(List<BoxedType<?>> parameters) {

        Preconditions.checkArgument(parameters.size() == 2, "ASSERT_CSV takes exactly two parameters.");
        Preconditions.checkArgument(parameters.get(0).isString(), "%s should be a string", parameters.get(0));
        Preconditions.checkArgument(parameters.get(1).value() instanceof Csv, "%s should be a csv array",
            parameters.get(1));

        String uuid = parameters.get(0).asString();
        Csv csvs = (Csv) parameters.get(1).value();

        for (int i = 0; i < csvs.nbRows(); i++) {

          String json = JsonCodec.asString(csvs.row(i));

          azzert(Builder.json(uuid, Integer.toString(i, 10), json));
          azzert(Builder.jsonPaths(uuid, Integer.toString(i, 10), json));
        }
        return BoxedType.create(true);
      }
    });

    // [DEPRECATED] Special operator. See {@link Literal#execute} for details.
    definitions_.put("FN_EXIST_IN_KB", new Function("EXIST_IN_KB") {

      @Override
      protected boolean isCacheable() {

        // The function's cache is shared between multiple processes
        return false;
      }

      @Override
      public BoxedType<?> evaluate(List<BoxedType<?>> parameters) {

        Preconditions.checkArgument(parameters.size() > 1, "EXIST_IN_KB takes at least two parameters.");

        String predicate = parameters.get(0).asString();
        List<String> terms = parameters.subList(1, parameters.size()).stream().map(bt -> {

          String term = bt.asString();

          if (bt.isNumber() || bt.isBoolean()) {
            return term;
          }
          return "_".equals(term) ? "_" : "\"" + term + "\"";
        }).collect(Collectors.toList());

        String fact = predicate + "(" + Joiner.on(',').join(terms) + ")?";
        Literal query = Parser.parseQuery(fact);
        Iterator<Clause> clauses = facts(query);

        while (clauses.hasNext()) {
          if (clauses.next().isFact()) {
            return BoxedType.create(true);
          }
        }
        return BoxedType.create(false);
      }
    });

    /**
     * [DEPRECATED] Special operator. Execute a GET HTTP query and use the returned data as new
     * facts.
     *
     * Example :
     *
     * <pre>
     * fn_http_materialize_facts("https://api.cf.com/api/v0/facts/crm/client", "prenom", _, Prenom,
     *     "nom", _, Nom, "email", _, Email)
     * </pre>
     *
     * Result :
     *
     * <pre>
     *  [
     *    {
     *      "namespace": "crm",
     *      "class": "client",
     *      "facts": [
     *        {
     * 	          "prenom": "Jane"
     * 	          "nom": "Doe"
     * 	  	      "email": "jane.doe@gmail.com"
     *        }, {
     * 	  	     "prenom": "John"
     * 	  	      "nom": "Doe"
     * 	  	      "email": "j.doe@gmail.com"
     *        },
     * 	      ...
     *      ]
     *    },
     *    ...
     *  ]
     * </pre>
     */
    definitions_.put("FN_HTTP_MATERIALIZE_FACTS", new Function("HTTP_MATERIALIZE_FACTS") {

      @Override
      protected boolean isCacheable() {

        // The function's cache is shared between multiple processes
        return false;
      }

      @Override
      public BoxedType<?> evaluate(List<BoxedType<?>> parameters) {

        Preconditions.checkArgument(parameters.size() >= 4,
            "HTTP_MATERIALIZE_FACTS_QUERY takes at least four parameters.");
        Preconditions.checkArgument(parameters.get(0).isString(), "%s should be a string", parameters.get(0));

        Base64.Encoder b64Encoder = Base64.getEncoder();
        StringBuilder builder = new StringBuilder();

        for (int i = 1; i < parameters.size(); i = i + 3) {

          String name = parameters.get(i).asString();
          String filter = "_".equals(parameters.get(i + 1).asString()) ? "" : parameters.get(i + 1).asString();
          String value = "_".equals(parameters.get(i + 2).asString()) ? "" : parameters.get(i + 2).asString();

          if (builder.length() > 0) {
            builder.append('&');
          }
          builder.append(name).append('=').append(Base64Codec.encodeB64(b64Encoder, value.isEmpty() ? filter : value));
        }

        String httpUrl = parameters.get(0).asString();
        String queryString = builder.toString();

        try {

          URL url = new URL(httpUrl);
          HttpURLConnection con = (HttpURLConnection) url.openConnection();
          con.setRequestMethod("GET");
          con.setInstanceFollowRedirects(true);
          con.setConnectTimeout(5 * 1000);
          con.setReadTimeout(10 * 1000);
          con.setDoOutput(true);

          try (DataOutputStream out = new DataOutputStream(con.getOutputStream())) {
            out.writeBytes(queryString);
          }

          StringBuilder result = new StringBuilder();
          int status = con.getResponseCode();

          if (status > 299) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getErrorStream()))) {

              @Var String inputLine;

              while ((inputLine = in.readLine()) != null) {
                result.append(inputLine);
              }

              logger_.error(LogFormatter.create(true).message(result.toString()).formatError());
            }
            return BoxedType.empty();
          }

          try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {

            @Var String inputLine;

            while ((inputLine = in.readLine()) != null) {
              result.append(inputLine);
            }
          }

          con.disconnect();
          List<Literal> facts = new ArrayList<>();
          Map<String, Object>[] jsons = JsonCodec.asArray(result.toString());

          for (Map<String, Object> json : jsons) {

            if (!json.containsKey("namespace")) {
              return BoxedType.empty();
            }
            if (!json.containsKey("class")) {
              return BoxedType.empty();
            }
            if (!json.containsKey("facts")) {
              return BoxedType.empty();
            }

            // String namespace = (String) json.get("namespace");
            // String clazz = (String) json.get("class");

            facts.addAll(((List<Map<String, Object>>) json.get("facts")).stream().map(fact -> {

              List<AbstractTerm> terms = new ArrayList<>();
              terms.add(newConst(parameters.get(0)));

              for (int i = 1; i < parameters.size(); i = i + 3) {
                String name = parameters.get(i).asString();
                String filter = parameters.get(i + 1).asString();
                terms.add(newConst(name));
                terms.add(newConst(filter));
                terms.add(newConst(fact.get(name)));
              }
              return new Literal("fn_" + name().toLowerCase(), terms);
            }).collect(Collectors.toList()));
          }
          return BoxedType.create(facts);
        } catch (IOException e) {
          logger_.error(LogFormatter.create(true).message(e).formatError());
          // fall through
        }
        return BoxedType.empty();
      }
    });

    /**
     * Ask the solver to materialize each collection's element as a fact.
     *
     * <pre>
     *     fn_materialize_facts(b64_(...), _).
     * </pre>
     */
    definitions_.put("FN_MATERIALIZE_FACTS", new Function("MATERIALIZE_FACTS") {

      @Override
      public BoxedType<?> evaluate(List<BoxedType<?>> parameters) {

        Preconditions.checkArgument(parameters.size() == 2, "MATERIALIZE_FACTS takes exactly two parameters.");
        Preconditions.checkArgument(parameters.get(0).isString(), "%s must be a string", parameters.get(0));
        Preconditions.checkArgument(parameters.get(1).isString(), "%s must be a string", parameters.get(1));

        String predicate = "fn_" + name().toLowerCase();
        Collection<Literal> newCollection = new ArrayList<>();
        Object[] oldCollection = Strings.isNullOrEmpty(parameters.get(0).asString()) ? new Map[0]
            : JsonCodec.asArrayOfUnknownType(parameters.get(0).asString());
        String filter = parameters.get(1).asString();

        for (Object obj : oldCollection) {

          List<AbstractTerm> terms = new ArrayList<>();
          terms.add(newConst(parameters.get(0).asString()));
          terms.add(newConst(obj));

          if ("_".equals(filter)) {
            newCollection.add(new Literal(predicate, terms));
          } else if (filter.equals(terms.get(1).toString())) {
            newCollection.add(new Literal(predicate, terms));
          }
        }
        return newCollection.isEmpty() ? BoxedType.empty() : BoxedType.create(newCollection);
      }
    });
  }

  /**
   * Perform the following actions :
   *
   * <ul>
   * <li>Remove probability from the clause head ;</li>
   * <li>Create a random fact name with the clause head probability ;</li>
   * <li>Add a reference to the newly created fact in the clause body.</li>
   * </ul>
   *
   * @param clause clause.
   * @return a {@link Pair} with {@link Pair#t} containing the rewritten clause and {@link Pair#u} the newly created
   * fact.
   */
  protected Pair<Clause, Clause> rewriteRuleHead(Clause clause) {

    Preconditions.checkNotNull(clause, "clause should not be null");
    Preconditions.checkArgument(clause.isRule(), "clause should be a clause : %s", clause);

    Literal head = clause.head();

    if (BigDecimal.ONE.compareTo(head.probability()) == 0) {
      return new Pair<>(clause, null);
    }

    Preconditions.checkState(!head.predicate().isNegated(), "the rule head should not be negated : %s", clause);

    String predicate = head.predicate().name();
    BigDecimal probability = head.probability();

    // Create fact
    String newPredicate = "proba_" + randomString_.nextString().toLowerCase();
    Literal newLiteral = new Literal(probability, newPredicate, newConst(true));
    Clause newFact = new Clause(newLiteral);

    // Rewrite clause
    Literal newHead = new Literal(predicate, clause.head().terms());
    List<Literal> newBody = new ArrayList<>(clause.body());
    newBody.add(new Literal(newPredicate, newConst(true)));
    Clause newRule = new Clause(newHead, newBody);

    return new Pair<>(newRule, newFact);
  }

  @Beta
  protected Clause mergeRules(Clause referencingRule, Clause referencedRule, int pos) {

    Preconditions.checkNotNull(referencingRule, "referencingRule should not be null");
    Preconditions.checkNotNull(referencedRule, "referencedRule should not be null");
    Preconditions.checkArgument(pos >= 0 && pos < referencingRule.body().size(), "pos must be such as 0 <= pos < %s",
        referencingRule.body().size());

    Map<com.computablefacts.decima.problog.Var, AbstractTerm> env = referencedRule.head()
        .unify(referencingRule.body().get(pos));

    Preconditions.checkState(env != null, "env should not be null");

    Clause newReferencedRule = referencedRule.subst(env);
    Clause newReferencingRule = referencingRule.subst(env);

    Literal newHead = newReferencingRule.head();
    List<Literal> newBody = new ArrayList<>();

    for (int k = 0; k < newReferencingRule.body().size(); k++) {
      if (k != pos) {
        newBody.add(newReferencingRule.body().get(k));
      } else {
        newBody.addAll(newReferencedRule.body());
      }
    }
    return new Clause(newHead, newBody);
  }
}
