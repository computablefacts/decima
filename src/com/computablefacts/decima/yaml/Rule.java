package com.computablefacts.decima.yaml;

import com.computablefacts.asterix.Generated;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A single ProbLog rule.
 *
 * <pre>
 * - name: predicate
 *   description: user-friendly description of the rule
 *   parameters: term_1, term_2, ...
 *   confidence: 0.xxx
 *   body:
 *     - literal_1.1, literal_1.2, ...
 *     - literal_2.1, literal_2.2, ...
 *     - ...
 *   tests:
 *     - kb: "fact1(...).\nfact2(...).\n..."
 *       query: "query1(...)?"
 *       output: "0.xxx::query1(...)."
 *     - kb: "fact1(...).\nfact2(...).\n..."
 *       query: "query2(...)?"
 *       output: "0.xxx::query2(...)."
 * </pre>
 * <p>
 * Once compiled, the YAML above will create the following rules :
 *
 * <pre>
 *     0.xxx:predicate_1(term_1, term_2, ...) :- literal_1.1, literal_1.2, ...
 *     0.xxx:predicate_1(term_1, term_2, ...) :- literal_2.1, literal_2.2, ...
 * </pre>
 */
@CheckReturnValue
final public class Rule {

  @JsonProperty("name")
  String name_;

  @JsonProperty("description")
  String description_;

  @JsonProperty("parameters")
  String parameters_;

  @JsonProperty("confidence")
  Double confidence_;

  @JsonProperty("body")
  String[] body_;

  @JsonProperty("tests")
  Test[] tests_;

  public Rule() {
  }

  public Rule(String name, String description, double confidence, String parameters, String[] body) {

    Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "name should neither be null nor empty");
    Preconditions.checkArgument(body != null && body.length > 0, "body should neither be null nor empty");
    Preconditions.checkNotNull(parameters, "parameters should not be null");
    Preconditions.checkState(confidence >= 0.0 && confidence <= 1.0,
        "confidence must be such as 0.0 <= confidence <= 1.0");

    name_ = name;
    description_ = description;
    confidence_ = confidence;
    parameters_ = parameters;
    body_ = body;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof Rule)) {
      return false;
    }
    Rule rule = (Rule) obj;
    return Objects.equals(name_, rule.name_) && Objects.equals(description_, rule.description_) && Objects.equals(
        parameters_, rule.parameters_) && Objects.equals(confidence_, rule.confidence_) && Arrays.equals(body_,
        rule.body_);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name_, description_, parameters_, confidence_, Arrays.hashCode(body_));
  }

  @Generated
  @Override
  public String toString() {

    String head = head();
    List<String> bodies = bodies();

    StringBuilder builder = new StringBuilder();

    for (int i = 0; i < bodies.size(); i++) {

      builder.append(proba());
      builder.append("::");
      builder.append(head);
      builder.append(" :- ");
      builder.append(bodies.get(i));
      builder.append(".\n");
    }
    return builder.toString();
  }

  private double proba() {
    return confidence_ == null || confidence_ <= 0.0 || confidence_ >= 1.0 ? 1.0 : confidence_;
  }

  private String head() {
    return name_ + "(" + parameters_ + ")";
  }

  private List<String> bodies() {
    return Arrays.stream(body_).collect(Collectors.toList());
  }
}
