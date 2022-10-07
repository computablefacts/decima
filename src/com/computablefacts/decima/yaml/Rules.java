package com.computablefacts.decima.yaml;

import com.computablefacts.asterix.Generated;
import com.computablefacts.decima.problog.Clause;
import com.computablefacts.decima.problog.Parser;
import com.computablefacts.logfmt.LogFormatter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.base.Preconditions;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Group ProbLog rules in a YAML file.
 *
 * <pre>
 * name: PROBLOG_RULES
 * description: Rules for Hermès
 * rules:
 *
 *   - name: predicate_1
 *     description: user-friendly description of the rule
 *     parameters: term_1, term_2, ...
 *     confidence: 0.xxx
 *     body:
 *       - literal_1.1, literal_1.2, ...
 *       - literal_2.1, literal_2.2, ...
 *       - ...
 *
 *   - name: predicate_2
 *     description: user-friendly description of the rule
 *     parameters: term_1, term_2, ...
 *     confidence: 0.yyy
 *     body:
 *       - literal_1.1, literal_1.2, ...
 *       - literal_2.1, literal_2.2, ...
 *       - ...
 *
 *  - ...
 * </pre>
 * <p>
 * Once compiled, the YAML above will create the following rules :
 *
 * <pre>
 *     0.xxx:predicate_1(term_1, term_2, ...) :- literal_1.1, literal_1.2, ...
 *     0.xxx:predicate_1(term_1, term_2, ...) :- literal_2.1, literal_2.2, ...
 *     0.yyy:predicate_2(term_1, term_2, ...) :- literal_1.1, literal_1.2, ...
 *     0.yyy:predicate_2(term_1, term_2, ...) :- literal_2.1, literal_2.2, ...
 * </pre>
 */
final public class Rules {

  private static final Logger logger_ = LoggerFactory.getLogger(Rules.class);

  @JsonProperty("name")
  String name_;

  @JsonProperty("description")
  String description_;

  @JsonProperty("rules")
  Rule[] rules_;

  public Rules() {
  }

  public static Rules load(File file) {
    return load(file, false);
  }

  public static Rules load(File file, boolean test) {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(file.exists(), "file does not exist : %s", file);

    try {
      YAMLFactory yamlFactory = new YAMLFactory();
      YAMLMapper yamlMapper = new YAMLMapper(yamlFactory);
      yamlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      Rules rules = yamlMapper.readValue(file, Rules.class);

      if (rules != null && test) {
        return rules.isValid() ? rules : null;
      }
      return rules;
    } catch (JsonProcessingException e) {
      logger_.error(LogFormatter.create(true).message(e).formatError());
    } catch (IOException e) {
      logger_.error(LogFormatter.create(true).message(e).formatError());
    }
    return null;
  }

  @Generated
  @Override
  public String toString() {

    StringBuilder builder = new StringBuilder();

    for (Rule rule : rules_) {
      builder.append(rule.toString());
    }
    return builder.toString();
  }

  public int nbRules() {
    return rules_ == null ? 0 : rules_.length;
  }

  private boolean isValid() {

    Set<Clause> clauses = Parser.parseClauses(toString());

    for (Rule rule : rules_) {
      if (rule.tests_ != null) {
        for (Test test : rule.tests_) {
          if (!test.matchOutput(clauses)) {

            StringBuilder builder = new StringBuilder();
            builder.append("\nTest failed for :")
                .append("\n===[ RULE ]=============================================================================\n")
                .append(rule).append(test);

            logger_.error(LogFormatter.create(true).message(builder.toString()).formatError());
            return false;
          }
        }
      }
    }
    return true;
  }
}
