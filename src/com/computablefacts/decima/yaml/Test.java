package com.computablefacts.decima.yaml;

import com.computablefacts.asterix.Generated;
import com.computablefacts.decima.problog.Clause;
import com.computablefacts.decima.problog.InMemoryKnowledgeBase;
import com.computablefacts.decima.problog.Literal;
import com.computablefacts.decima.problog.Parser;
import com.computablefacts.decima.problog.ProbabilityEstimator;
import com.computablefacts.decima.problog.Solver;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CheckReturnValue;
import java.math.BigDecimal;
import java.util.Set;

/**
 * Encode a single test case.
 *
 * <pre>
 * kb: "fact1(...).\nfact2(...).\n..."
 * query: "query1(...)?"
 * output: "0.xxx::query1(...)."
 * </pre>
 */
@CheckReturnValue
final public class Test {

  @JsonProperty("kb")
  String kb_;

  @JsonProperty("query")
  String query_;

  @JsonProperty("output")
  String output_;

  public Test() {
  }

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
    builder.append("===[ KB ]=============================================================================\n")
        .append(kb_)
        .append("\n===[ QUERY ]=============================================================================\n")
        .append(query_).append(
            "\n===[ EXPECTED OUTPUT ]=============================================================================\n")
        .append(output_);

    return builder.toString();
  }

  public boolean matchOutput(Set<Clause> moreRules) {

    if (!Strings.isNullOrEmpty(output_)) {

      Set<Clause> facts = Parser.parseClauses(output_);
      ProbabilityEstimator estimator = new ProbabilityEstimator(proofs(moreRules));

      return facts.stream().allMatch(fact -> {
        BigDecimal proba = estimator.probability(fact.head());
        return proba.compareTo(fact.head().probability()) == 0;
      });
    }
    return proofs(moreRules).isEmpty();
  }

  private Set<Clause> proofs(Set<Clause> moreRules) {

    Set<Clause> clauses =
        moreRules == null ? Parser.parseClauses(kb_) : Sets.union(moreRules, Parser.parseClauses(kb_));
    Literal query = Parser.parseQuery(query_);

    InMemoryKnowledgeBase kb = new InMemoryKnowledgeBase();
    kb.azzert(clauses);

    Solver solver = new Solver(kb, true);
    return solver.proofs(query);
  }
}
