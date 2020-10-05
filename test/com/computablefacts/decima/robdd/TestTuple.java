package com.computablefacts.decima.robdd;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class TestTuple {

  @Test
  public void testHashcodeAndEquals() {
    EqualsVerifier.forClass(Tuple.class).verify();
  }
}
