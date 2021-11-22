package com.computablefacts.decima.problog;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

final public class TestUtils {

  private TestUtils() {}

  public static Solver solver(InMemoryKnowledgeBase kb) {
    return new Solver(kb, true);
  }

  public static InMemoryKnowledgeBase kb() {
    return new InMemoryKnowledgeBase();
  }

  public static Clause parseClause(String clause) {
    return Parser.parseClause(clause);
  }

  public static boolean isValid(Set<Clause> proofs, String head, List<String> body) {

    Literal newHead = parseClause(head + ".").head();
    List<Literal> newBody =
        body.stream().map(s -> parseClause(s + ".").head()).collect(Collectors.toList());

    return proofs.stream().filter(c -> c.head().equals(newHead)).anyMatch(c -> {
      for (int i = 0; i < newBody.size(); i++) {
        if (!c.body().subList(i, c.body().size()).contains(newBody.get(i))) {
          return false;
        }
      }
      return true;
    });
  }

  /**
   * Check if a set of proofs contains at least one occurrence of a given proof.
   *
   * @param proofs set of proofs.
   * @param proof proof to find.
   * @return true if the proof has been found.
   */
  public static boolean isValid(Set<Clause> proofs, String proof) {

    Clause clause = parseClause(proof);

    Preconditions.checkState(clause != null && clause.isGrounded(), "Invalid proof to match : %s",
        proof);

    Literal head = clause.head();
    List<Literal> body = clause.body();

    return proofs.stream().filter(c -> c.head().isRelevant(head)).anyMatch(c -> {
      for (int i = 0; i < body.size(); i++) {
        if (!c.body().subList(i, c.body().size()).contains(body.get(i))) {
          return false;
        }
      }
      return true;
    });
  }
}
