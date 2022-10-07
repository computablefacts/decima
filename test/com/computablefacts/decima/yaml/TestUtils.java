package com.computablefacts.decima.yaml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

final public class TestUtils {

  public static String load(String path) throws IOException {
    try (InputStream stream = TestUtils.class.getResourceAsStream(path)) {
      try (BufferedReader buffer = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {

        StringBuilder builder = new StringBuilder();

        for (String line; (line = buffer.readLine()) != null; ) {
          builder.append(line).append('\n');
        }
        return builder.toString();
      }
    }
  }
}
