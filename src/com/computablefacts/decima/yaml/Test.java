package com.computablefacts.decima.yaml;

import java.math.BigDecimal;
import java.util.Set;

import com.computablefacts.decima.problog.Clause;
import com.computablefacts.decima.problog.Estimator;
import com.computablefacts.decima.problog.InMemoryKnowledgeBase;
import com.computablefacts.decima.problog.Literal;
import com.computablefacts.decima.problog.Parser;
import com.computablefacts.decima.problog.Solver;
import com.computablefacts.nona.Generated;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CheckReturnValue;

@CheckReturnValue
final public class Test {

  @JsonProperty("kb")
  String kb_;

  @JsonProperty("query")
  String query_;

  @JsonProperty("output")
  String output_;

  public Test() {}

  public Test(String kb, String query) {
    this(kb, query, null);
  }

  public Test(String kb, String query, String output) {

    Preconditions.checkNotNull(kb, "kb should not be null");
    Preconditions.checkNotNull(query, "query should not be null");

    kb_ = kb;
    query_ = query;
    output_ = output;
  }

  @Generated
  @Override
  public String toString() {

    StringBuilder builder = new StringBuilder();
    builder.append(
        "===[ KB ]=============================================================================\n")
        .append(kb_)
        .append(
            "\n===[ QUERY ]=============================================================================\n")
        .append(query_)
        .append(
            "\n===[ EXPECTED OUTPUT ]=============================================================================\n")
        .append(output_);

    return builder.toString();
  }

  public boolean matchOutput(Set<Clause> moreRules) {

    if (!Strings.isNullOrEmpty(output_)) {

      Set<Clause> facts = Parser.parseClauses(output_);
      Estimator estimator = new Estimator(proofs(moreRules));

      return facts.stream().allMatch(fact -> {
        BigDecimal proba = estimator.probability(fact.head());
        return proba.compareTo(fact.head().probability()) == 0;
      });
    }
    return proofs(moreRules).isEmpty();
  }

  private Set<Clause> proofs(Set<Clause> moreRules) {

    Set<Clause> clauses = moreRules == null ? Parser.parseClauses(kb_)
        : Sets.union(moreRules, Parser.parseClauses(kb_));
    Literal query = Parser.parseQuery(query_);

    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();
    kb.azzert(clauses);

    Solver solver = new Solver(kb);
    Set<Clause> proofs = solver.proofs(query);
    return proofs;
  }
}
