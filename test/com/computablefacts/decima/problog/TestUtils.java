package com.computablefacts.decima.problog;

import static com.computablefacts.decima.problog.Parser.parseClause;

import com.computablefacts.asterix.trie.Trie;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

final public class TestUtils {

  private TestUtils() {
  }

  public static Clause buildClause(String head, List<String> body) {

    Literal newHead = parseClause(head + ".").head();
    List<Literal> newBody = body.stream().map(s -> parseClause(s + ".").head()).collect(Collectors.toList());

    Clause clause = new Clause(newHead, newBody);
    Preconditions.checkState(clause.isGrounded(), "invalid clause : %s", clause);
    return clause;
  }

  public static boolean checkAnswers(Set<Clause> actual, Set<Clause> expected) {
    for (Clause answer : expected) {

      System.out.println("Checking answer : " + answer);
      Stopwatch stopwatch = Stopwatch.createStarted();

      if (!isValidAnswer(actual, answer.head())) {
        return false;
      }
      stopwatch.stop();
      System.out.println("Answer checked in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms : " + answer);
    }
    return true;
  }

  public static boolean checkProofs(Map<Literal, Trie<Literal>> actual, Set<Clause> expected) {
    return checkProofs(actual, expected, false);
  }

  public static boolean checkProofs(Map<Literal, Trie<Literal>> actual, Set<Clause> expected, boolean ignoreFunctions) {
    for (Clause proof : expected) {

      System.out.println("Checking proof : " + proof);
      Stopwatch stopwatch = Stopwatch.createStarted();

      if (proof.isFact()) {
        if (!actual.containsKey(proof.head())) {
          return false;
        }
      } else {

        List<List<Literal>> bodies = new ArrayList<>();
        permute(proof.body().stream().filter(b -> !ignoreFunctions || !b.predicate().isPrimitive())
            .collect(Collectors.toList()), bodies);

        if (bodies.stream().noneMatch(body -> {
          if (actual.containsKey(proof.head())) {

            List<List<Literal>> proofs = actual.get(proof.head()).paths().stream().map(path -> path.stream()
                    .filter(p -> !p.predicate().baseName().startsWith("proba_") /* ignore synthetic facts */)
                    .filter(p -> !ignoreFunctions || !p.predicate().isPrimitive()).collect(Collectors.toList()))
                .collect(Collectors.toList());

            return proofs.contains(body);
          }
          return false;
        })) {
          return false;
        }
      }
      stopwatch.stop();
      System.out.println("Proof checked in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms : " + proof);
    }
    return true;
  }

  private static boolean isValidAnswer(Set<Clause> answers, Literal fact) {

    Preconditions.checkState(fact.isGrounded(), "invalid fact : %s", fact);

    return answers.stream().anyMatch(c -> c.head().isRelevant(fact));
  }

  public static <T> void permute(List<T> a, List<List<T>> output) {
    permute(a.toArray((T[]) Array.newInstance(a.get(0).getClass(), 0)), a.size(), a.size(), output);
  }

  public static <T> void permute(T[] a, List<List<T>> output) {
    permute(a, a.length, a.length, output);
  }

  private static <T> void permute(T[] a, int size, int n, List<List<T>> output) {

    // if size becomes 1 then prints the obtained permutation
    if (size == 1) {
      List<T> list = new ArrayList<>();
      for (int i = 0; i < n; i++) {
        list.add(a[i]);
      }
      output.add(list);
    }
    for (int i = 0; i < size; i++) {

      permute(a, size - 1, n, output);

      // if size is odd, swap 0th i.e (first) and (size-1)th i.e (last) element
      if (size % 2 == 1) {
        T temp = a[0];
        a[0] = a[size - 1];
        a[size - 1] = temp;
      }

      // If size is even, swap ith and (size-1)th i.e last element
      else {
        T temp = a[i];
        a[i] = a[size - 1];
        a[size - 1] = temp;
      }
    }
  }
}
