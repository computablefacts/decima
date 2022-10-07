package com.computablefacts.decima.robdd;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

public class TestTuple {

  @Test
  public void testHashcodeAndEquals() {
    EqualsVerifier.forClass(Tuple.class).verify();
  }
}
