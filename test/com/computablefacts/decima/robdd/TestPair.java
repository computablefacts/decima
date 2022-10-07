package com.computablefacts.decima.robdd;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

public class TestPair {

  @Test
  public void testHashcodeAndEquals() {
    EqualsVerifier.forClass(Pair.class).verify();
  }
}
