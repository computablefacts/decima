package com.computablefacts.decima.json;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class TestProvenance {

  @Test
  public void testHashcodeAndEquals() {
    EqualsVerifier.forClass(Provenance.class).verify();
  }
}
