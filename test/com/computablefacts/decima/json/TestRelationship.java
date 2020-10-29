package com.computablefacts.decima.json;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

public class TestRelationship {

  @Test
  public void testHashcodeAndEquals() {
    EqualsVerifier.forClass(Relationship.class).suppress(Warning.NONFINAL_FIELDS)
        .withIgnoredFields("id_", "externalId_", "fromExternalId_", "toExternalId_").verify();
  }
}
