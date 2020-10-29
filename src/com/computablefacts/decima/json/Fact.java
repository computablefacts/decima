package com.computablefacts.decima.json;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

import com.computablefacts.nona.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.errorprone.annotations.CheckReturnValue;

/**
 * <pre>
 * {
 *     "type": "FACT TYPE",
 *     "values": ["fact value 1", "fact value 2", ...],
 *     "is_valid": false,
 *     "authorizations": "AUTH1|AUTH2|AUTH3",
 *     "confidence_score": 0.87,
 *     "start_date": "2019-06-01T00:00:00Z",
 *     "end_date": "2019-06-02T00:00:00Z",
 *     "external_id": "3azvw|2019-10-21T12:11:27.599Z"
 *     "metadata": [...],
 *     "provenances": [...]
 * }
 * </pre>
 */
@Generated
@CheckReturnValue
@JsonInclude(JsonInclude.Include.NON_EMPTY)
final public class Fact extends HasId {

  @JsonProperty("metadata")
  public final List<Metadata> metadata_ = new ArrayList<>();
  @JsonProperty("provenances")
  public final List<Provenance> provenances_ = new ArrayList<>();
  @JsonProperty("values")
  public final List<String> values_ = new ArrayList<>();
  @JsonProperty("type")
  public final String type_;
  @JsonProperty("is_valid")
  public final Boolean isValid_;
  @JsonProperty("authorizations")
  public final String authorizations_;
  @JsonProperty("confidence_score")
  public final double confidenceScore_;
  @JsonProperty("start_date")
  public final String startDate_;
  @JsonProperty("end_date")
  public final String endDate_;

  public Fact(String type, double confidenceScore) {
    this(type, confidenceScore, null, new Date(), null, null);
  }

  public Fact(String type, double confidenceScore, String authorizations) {
    this(type, confidenceScore, authorizations, new Date(), null, null);
  }

  public Fact(String type, double confidenceScore, String authorizations, Date startDate,
      Date endDate, Boolean isValid) {

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
    if (!(o instanceof Fact)) {
      return false;
    }
    Fact fact = (Fact) o;
    return Objects.equals(type_, fact.type_) && Objects.equals(isValid_, fact.isValid_)
        && Objects.equals(authorizations_, fact.authorizations_)
        && Objects.equals(confidenceScore_, fact.confidenceScore_)
        && Objects.equals(startDate_, fact.startDate_) && Objects.equals(endDate_, fact.endDate_)
        && Objects.equals(metadata_, fact.metadata_)
        && Objects.equals(provenances_, fact.provenances_) && Objects.equals(values_, fact.values_);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type_, isValid_, authorizations_, confidenceScore_, startDate_, endDate_,
        metadata_, provenances_, values_);
  }

  public void value(String value) {

    Preconditions.checkNotNull(value);

    values_.add(value);
  }

  public void metadata(Collection<Metadata> metadata) {

    Preconditions.checkNotNull(metadata);

    this.metadata_.addAll(metadata);
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
