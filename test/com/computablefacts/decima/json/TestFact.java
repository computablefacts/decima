package com.computablefacts.decima.json;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class TestFact {

  @Test
  public void testHashcodeAndEquals() {
    EqualsVerifier.forClass(Fact.class).withIgnoredFields("id_", "externalId_").verify();
  }
}
