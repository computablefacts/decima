package com.computablefacts.decima.json;

import java.util.Objects;

import com.computablefacts.nona.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.errorprone.annotations.CheckReturnValue;

/**
 * <pre>
 * {
 *     "type": "MD_TYPE_2",
 *     "key": "key2",
 *     "value": "value2"
 * }
 * </pre>
 */
@Generated
@CheckReturnValue
@JsonInclude(JsonInclude.Include.NON_NULL)
final public class Metadata {

  @JsonProperty("type")
  private final String type_;
  @JsonProperty("key")
  private final String key_;
  @JsonProperty("value")
  private final String value_;

  public Metadata(String type, String key, String value) {

    Preconditions.checkArgument(!Strings.isNullOrEmpty(type),
        "type should neither be null nor empty");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(key),
        "key should neither be null nor empty");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(value),
        "value should neither be null nor empty");

    type_ = type;
    key_ = key;
    value_ = value;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof Metadata)) {
      return false;
    }
    Metadata metadata = (Metadata) o;
    return Objects.equals(type_, metadata.type_) && Objects.equals(key_, metadata.key_)
        && Objects.equals(value_, metadata.value_);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type_, key_, value_);
  }

  public String type() {
    return type_;
  }

  public String key() {
    return key_;
  }

  public String value() {
    return value_;
  }
}
