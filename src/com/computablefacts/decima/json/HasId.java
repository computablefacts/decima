package com.computablefacts.decima.json;

import java.time.Instant;

import com.computablefacts.decima.problog.RandomString;
import com.computablefacts.nona.Generated;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@Generated
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HasId {

  @JsonProperty("external_id")
  public final String externalId_;
  @JsonIgnore
  public int id_;

  public HasId() {
    externalId_ = new RandomString(5).nextString() + '|' + Instant.now().toString();
  }
}
