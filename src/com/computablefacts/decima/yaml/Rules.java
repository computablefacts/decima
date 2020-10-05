package com.computablefacts.decima.yaml;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.computablefacts.nona.Generated;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

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
 *
 * One compiled, the YAML above will create the following rules :
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

  public Rules() {}

  public static Rules load(File file) {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(file.exists(), "file does not exist : %s", file);

    try {
      YAMLFactory yamlFactory = new YAMLFactory();
      YAMLMapper yamlMapper = new YAMLMapper(yamlFactory);
      yamlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      return yamlMapper.readValue(file, Rules.class);
    } catch (JsonProcessingException e) {
      logger_.error(Throwables.getStackTraceAsString(Throwables.getRootCause(e)));
    } catch (IOException e) {
      logger_.error(Throwables.getStackTraceAsString(Throwables.getRootCause(e)));
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
}
