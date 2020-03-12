package com.computablefacts.decima.trie;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class TrieTest {

  @Test
  public void testEmptyTrie() {
    Trie<Character> trie = new Trie<>();
    assertTrue(trie.isEmpty());
  }

  @Test
  public void testTrieNotEmpty() {
    Trie<String> trie = createExampleTrie();
    assertFalse(trie.isEmpty());
  }

  @Test
  public void testAddElements() {

    Trie<String> trie = createExampleTrie();

    assertFalse(trie.contains(Arrays.asList("3")));
    assertFalse(trie.contains(Arrays.asList("v", "i", "d", "a")));

    assertTrue(trie.contains(Arrays.asList("P", "r", "o", "g", "r", "a", "m", "m", "i", "n", "g")));
    assertTrue(trie.contains(Arrays.asList("i", "s")));
    assertTrue(trie.contains(Arrays.asList("a")));
    assertTrue(trie.contains(Arrays.asList("w", "a", "y")));
    assertTrue(trie.contains(Arrays.asList("o", "f")));
    assertTrue(trie.contains(Arrays.asList("l", "i", "f", "e")));
  }

  @Test
  public void testLookForNonExistingElement() {
    Trie<String> trie = createExampleTrie();
    assertFalse(trie.contains(Arrays.asList("9", "9")));
  }

  @Test
  public void testDeleteElements() {

    Trie<String> trie = createExampleTrie();
    assertTrue(trie.contains(Arrays.asList("P", "r", "o", "g", "r", "a", "m", "m", "i", "n", "g")));

    trie.delete(Arrays.asList("P", "r", "o", "g", "r", "a", "m", "m", "i", "n", "g"));
    assertFalse(
        trie.contains(Arrays.asList("P", "r", "o", "g", "r", "a", "m", "m", "i", "n", "g")));
  }

  @Test
  public void testDeleteOverlappingElements() {

    Trie<String> trie = new Trie<>();

    trie.insert(Arrays.asList("p", "i", "e"));
    trie.insert(Arrays.asList("p", "i", "e", "s"));

    trie.delete(Arrays.asList("p", "i", "e", "s"));

    assertTrue(trie.contains(Arrays.asList("p", "i", "e")));
  }

  @Test
  public void testRemoveNode() {

    Trie<String> trie = createExampleTrie();

    trie.remove((str) -> str.equals("i"));

    assertFalse(
        trie.contains(Arrays.asList("P", "r", "o", "g", "r", "a", "m", "m", "i", "n", "g")));
    assertFalse(trie.contains(Arrays.asList("i", "s")));
    assertFalse(trie.contains(Arrays.asList("l", "i", "f", "e")));

    assertTrue(trie.contains(Arrays.asList("a")));
    assertTrue(trie.contains(Arrays.asList("w", "a", "y")));
    assertTrue(trie.contains(Arrays.asList("o", "f")));

    // Ensure no partial paths remain
    assertFalse(trie.contains(Arrays.asList("P", "r", "o", "g", "r", "a", "m", "m")));
    assertFalse(trie.contains(Arrays.asList("l")));
  }

  private Trie<String> createExampleTrie() {
    Trie<String> trie = new Trie<>();
    trie.insert(Arrays.asList("P", "r", "o", "g", "r", "a", "m", "m", "i", "n", "g"));
    trie.insert(Arrays.asList("i", "s"));
    trie.insert(Arrays.asList("a"));
    trie.insert(Arrays.asList("w", "a", "y"));
    trie.insert(Arrays.asList("o", "f"));
    trie.insert(Arrays.asList("l", "i", "f", "e"));
    return trie;
  }
}
