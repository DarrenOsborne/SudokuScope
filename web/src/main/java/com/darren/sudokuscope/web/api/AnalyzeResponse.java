package com.darren.sudokuscope.web.api;

import com.darren.sudokuscope.core.SudokuBoard;
import com.darren.sudokuscope.core.solver.SudokuAnalysis;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

public record AnalyzeResponse(
    boolean valid,
    String status,
    String solutionCount,
    boolean limitReached,
    boolean unique,
    String message,
    List<Integer> exemplarSolution) {

  public static AnalyzeResponse from(SudokuAnalysis analysis) {
    Optional<SudokuBoard> solution = analysis.exemplarSolution();
    return new AnalyzeResponse(
        analysis.valid(),
        analysis.status().name(),
        formatCount(analysis.solutionCount()),
        analysis.limitReached(),
        analysis.hasUniqueSolution(),
        analysis.message(),
        solution.map(AnalyzeResponse::toList).orElse(null));
  }

  private static List<Integer> toList(SudokuBoard board) {
    return java.util.Arrays.stream(board.toIntArray()).boxed().toList();
  }

  private static String formatCount(BigInteger count) {
    return count.compareTo(BigInteger.valueOf(1_000_000)) > 0 ? count.toString() : count.toString();
  }
}
