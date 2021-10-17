package com.computablefacts.decima.problog;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class ConstTest {

  @Test
  public void testHashcodeAndEquals() {
    EqualsVerifier.forClass(Const.class).withNonnullFields("id_").withIgnoredFields("value_")
        .verify();
  }
}
