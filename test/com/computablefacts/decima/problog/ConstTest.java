package com.computablefacts.decima.problog;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class ConstTest {

  @Test
  public void testHashcodeAndEquals() {
    EqualsVerifier.forClass(Const.class).withIgnoredFields("value_").withNonnullFields("id_")
        .verify();
  }
}
