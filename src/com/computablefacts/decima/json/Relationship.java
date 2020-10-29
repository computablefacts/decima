package com.computablefacts.decima.json;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

import com.computablefacts.nona.Generated;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.errorprone.annotations.CheckReturnValue;

/**
 * <pre>
 * {
 *    "is_valid": true,
 *    "authorizations": "AUTH1|AUTH2|AUTH3",
 *    "confidence_score": 0.87,
 *    "type": "RELATION TYPE",
 *    "from_id": 1,
 *    "to_id": 2,
 *    "external_id": "EXT_ID",
 *    "start_date": "2019-06-01T00:00:00Z",
 *    "end_date": "2019-06-02T00:00:00Z",
 *    "metadata": [...],
 *    "provenances": [...]
 * }
 * </pre>
 */
@Generated
@CheckReturnValue
@JsonInclude(JsonInclude.Include.NON_EMPTY)
final public class Relationship extends HasId {

  @JsonProperty("is_valid")
  public final boolean isValid_;
  @JsonProperty("authorizations")
  public final String authorizations_;
  @JsonProperty("confidence_score")
  public final double confidenceScore_;
  @JsonProperty("type")
  public final String type_;
  @JsonProperty("start_date")
  public final String startDate_;
  @JsonProperty("end_date")
  public final String endDate_;
  @JsonProperty("metadata")
  public final List<Metadata> metadata_ = new ArrayList<>();
  @JsonProperty("provenances")
  public final List<Provenance> provenances_ = new ArrayList<>();
  @JsonProperty("from_id")
  public int fromId_;
  @JsonProperty("to_id")
  public int toId_;
  @JsonIgnore
  public String fromExternalId_;
  @JsonIgnore
  public String toExternalId_;

  public Relationship(String type, double confidenceScore, int fromId, int toId) {
    this(type, confidenceScore, fromId, toId, null, new Date(), null, true);
  }

  public Relationship(String type, double confidenceScore, int fromId, int toId,
      String authorizations, Date startDate, Date endDate, boolean isValid) {

    this(type, confidenceScore, authorizations, startDate, endDate, isValid);

    Preconditions.checkArgument(fromId >= 0, "fromId should be >= 0");
    Preconditions.checkArgument(toId >= 0, "toId should be >= 0");

    fromId_ = fromId;
    toId_ = toId;
  }

  public Relationship(String type, double confidenceScore, String fromId, String toId) {
    this(type, confidenceScore, fromId, toId, null, new Date(), null, true);
  }

  public Relationship(String type, double confidenceScore, String fromId, String toId,
      String authorizations) {
    this(type, confidenceScore, fromId, toId, authorizations, new Date(), null, true);
  }

  public Relationship(String type, double confidenceScore, String fromId, String toId,
      String authorizations, Date startDate, Date endDate, boolean isValid) {

    this(type, confidenceScore, authorizations, startDate, endDate, isValid);

    Preconditions.checkArgument(!Strings.isNullOrEmpty(fromId),
        "fromId should neither be null nor empty");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(toId),
        "toId should neither be null nor empty");

    fromExternalId_ = fromId;
    toExternalId_ = toId;
  }

  public Relationship(String type, double confidenceScore) {
    this(type, confidenceScore, null, new Date(), null, true);
  }

  public Relationship(String type, double confidenceScore, String authorizations, Date startDate,
      Date endDate, boolean isValid) {

    super();

    Preconditions.checkArgument(!Strings.isNullOrEmpty(type),
        "type should neither be null nor empty");
    Preconditions.checkArgument(confidenceScore >= 0.0 && confidenceScore <= 1.0,
        "confidenceScore should neither be >= 0 and <= 1");

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

    type_ = type;
    confidenceScore_ = confidenceScore;
    authorizations_ = authorizations;
    startDate_ = startDate == null ? null : sdf.format(startDate);
    endDate_ = endDate == null ? null : sdf.format(endDate);
    isValid_ = isValid;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof Relationship)) {
      return false;
    }
    Relationship fact = (Relationship) o;
    return Objects.equals(type_, fact.type_) && Objects.equals(isValid_, fact.isValid_)
        && Objects.equals(authorizations_, fact.authorizations_)
        && Objects.equals(confidenceScore_, fact.confidenceScore_)
        && Objects.equals(fromId_, fact.fromId_) && Objects.equals(toId_, fact.toId_)
        && Objects.equals(startDate_, fact.startDate_) && Objects.equals(endDate_, fact.endDate_)
        && Objects.equals(metadata_, fact.metadata_)
        && Objects.equals(provenances_, fact.provenances_);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type_, isValid_, authorizations_, confidenceScore_, fromId_, toId_,
        startDate_, endDate_, metadata_, provenances_);
  }

  public void metadata(Metadata metadata) {

    Preconditions.checkNotNull(metadata);

    this.metadata_.add(metadata);
  }

  public void provenance(Provenance provenance) {

    Preconditions.checkNotNull(provenance);

    provenances_.add(provenance);
  }
}
