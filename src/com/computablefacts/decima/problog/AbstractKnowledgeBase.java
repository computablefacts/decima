package com.computablefacts.decima.problog;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.computablefacts.asterix.RandomString;
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
import com.computablefacts.nona.types.BoxedType;
import com.computablefacts.nona.types.Csv;
import com.computablefacts.nona.types.Json;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;

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

    Preconditions.checkState(!BigDecimal.ZERO.equals(probability),
        "head probability must be != 0.0 : %s", newClause);

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

    // Special operator. Allow KB modification at runtime.
    definitions_.put("FN_ASSERT_JSON", new Function("ASSERT_JSON") {

      @Override
      protected boolean isCacheable() {

        // The function's cache is shared between multiple processes
        return false;
      }

      @Override
      public BoxedType<?> evaluate(List<BoxedType<?>> parameters) {

        Preconditions.checkArgument(parameters.size() == 2,
            "ASSERT_JSON takes exactly two parameters.");
        Preconditions.checkArgument(parameters.get(0).isString(), "%s should be a string",
            parameters.get(0));
        Preconditions.checkArgument(parameters.get(1).value() instanceof Json,
            "%s should be a json array", parameters.get(1));

        String uuid = parameters.get(0).asString();
        Json jsons = (Json) parameters.get(1).value();

        for (int i = 0; i < jsons.nbObjects(); i++) {

          String json = JsonCodec.asString(jsons.object(i));

          azzert(Builder.json(uuid, Integer.toString(i, 10), json));
          azzert(Builder.jsonPaths(uuid, Integer.toString(i, 10), json));
        }
        return BoxedType.create(true);
      }
    });

    // Special operator. Allow KB modification at runtime.
    definitions_.put("FN_ASSERT_CSV", new Function("ASSERT_CSV") {

      @Override
      protected boolean isCacheable() {

        // The function's cache is shared between multiple processes
        return false;
      }

      @Override
      public BoxedType<?> evaluate(List<BoxedType<?>> parameters) {

        Preconditions.checkArgument(parameters.size() == 2,
            "ASSERT_CSV takes exactly two parameters.");
        Preconditions.checkArgument(parameters.get(0).isString(), "%s should be a string",
            parameters.get(0));
        Preconditions.checkArgument(parameters.get(1).value() instanceof Csv,
            "%s should be a csv array", parameters.get(1));

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

    // Special operator. See {@link Literal#execute} for details.
    definitions_.put("FN_EXIST_IN_KB", new Function("EXIST_IN_KB") {

      @Override
      protected boolean isCacheable() {

        // The function's cache is shared between multiple processes
        return false;
      }

      @Override
      public BoxedType<?> evaluate(List<BoxedType<?>> parameters) {

        Preconditions.checkArgument(parameters.size() > 1,
            "EXIST_IN_KB takes at least two parameters.");

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
     * Special operator. Execute a GET HTTP query and use the returned data as new facts.
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
        Preconditions.checkArgument(parameters.get(0).isString(), "%s should be a string",
            parameters.get(0));

        Base64.Encoder b64Encoder = Base64.getEncoder();
        StringBuilder builder = new StringBuilder();

        for (int i = 1; i < parameters.size(); i = i + 3) {

          String name = parameters.get(i).asString();
          String filter =
              "_".equals(parameters.get(i + 1).asString()) ? "" : parameters.get(i + 1).asString();
          String value =
              "_".equals(parameters.get(i + 2).asString()) ? "" : parameters.get(i + 2).asString();

          if (builder.length() > 0) {
            builder.append('&');
          }
          builder.append(name).append('=')
              .append(Base64Codec.encodeB64(b64Encoder, value.isEmpty() ? filter : value));
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
            try (BufferedReader in =
                new BufferedReader(new InputStreamReader(con.getErrorStream()))) {

              @Var
              String inputLine;

              while ((inputLine = in.readLine()) != null) {
                result.append(inputLine);
              }

              logger_.error(LogFormatter.create(true).message(result.toString()).formatError());
            }
            return BoxedType.empty();
          }

          try (
              BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {

            @Var
            String inputLine;

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
              terms.add(new Const(parameters.get(0)));

              for (int i = 1; i < parameters.size(); i = i + 3) {
                String name = parameters.get(i).asString();
                String filter = parameters.get(i + 1).asString();
                terms.add(new Const(name));
                terms.add(new Const(filter));
                terms.add(new Const(fact.get(name)));
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
   * @return a {@link Pair} with {@link Pair#t} containing the rewritten clause and {@link Pair#u}
   *         the newly created fact.
   */
  protected Pair<Clause, Clause> rewriteRuleHead(Clause clause) {

    Preconditions.checkNotNull(clause, "clause should not be null");
    Preconditions.checkArgument(clause.isRule(), "clause should be a clause : %s", clause);

    Literal head = clause.head();

    if (BigDecimal.ONE.compareTo(head.probability()) == 0) {
      return new Pair<>(clause, null);
    }

    Preconditions.checkState(!head.predicate().isNegated(),
        "the rule head should not be negated : %s", clause);

    String predicate = head.predicate().name();
    BigDecimal probability = head.probability();

    // Create fact
    String newPredicate = "proba_" + randomString_.nextString().toLowerCase();
    Literal newLiteral = new Literal(probability, newPredicate, new Const(true));
    Clause newFact = new Clause(newLiteral);

    // Rewrite clause
    Literal newHead = new Literal(predicate, clause.head().terms());
    List<Literal> newBody = new ArrayList<>(clause.body());
    newBody.add(new Literal(newPredicate, new Const(true)));
    Clause newRule = new Clause(newHead, newBody);

    return new Pair<>(newRule, newFact);
  }
}
