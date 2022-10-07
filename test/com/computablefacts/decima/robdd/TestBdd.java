package com.computablefacts.decima.robdd;

import com.google.errorprone.annotations.Var;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;

public class TestBdd {

  private static Map<Integer, Boolean> buildInterpretation(String key) {

    @Var int index = 0;
    Map<Integer, Boolean> interpretation = new HashMap<>();

    for (int i = 0; i < key.length(); i++) {
      char v = key.charAt(i);
      interpretation.put(index, v == '1');
      index++;
    }
    return interpretation;
  }

  protected Map<String, Boolean> buildThruthTable(BddManager manager, BddNode root) {
    Map<String, Boolean> truth = new HashMap<>();
    addThruthValue("", root, truth, manager.N());
    return truth;
  }

  protected void checkThruthTable(Map<String, Boolean> matrix, BddNode node) {
    for (Map.Entry<String, Boolean> kv : matrix.entrySet()) {
      Map<Integer, Boolean> interpretation = buildInterpretation(kv.getKey());
      boolean value = evaluateBDD(node, interpretation);
      Assert.assertEquals(matrix.get(kv.getKey()), value);
    }
  }

  void addThruthValue(String key, BddNode node, Map<String, Boolean> matrix, int acc) {
    if (acc == 0) {
      Map<Integer, Boolean> interpretation = buildInterpretation(key);
      boolean value = evaluateBDD(node, interpretation);
      matrix.put(key, value);
      return;
    }
    addThruthValue(key + "0", node, matrix, acc - 1);
    addThruthValue(key + "1", node, matrix, acc - 1);
  }

  boolean evaluateBDD(BddNode root, Map<Integer, Boolean> interpretation) {
    if (root.isOne()) {
      return true;
    } else if (root.isZero()) {
      return false;
    } else {
      boolean b = interpretation.get(root.index());
      if (b) {
        return evaluateBDD(root.high(), interpretation);
      }
      return evaluateBDD(root.low(), interpretation);
    }
  }
}
