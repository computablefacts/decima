package com.computablefacts.decima.robdd;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class TestPair {

  @Test
  public void testHashcodeAndEquals() {
    EqualsVerifier.forClass(Pair.class).verify();
  }
}
