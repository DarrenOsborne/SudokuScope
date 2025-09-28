package com.darren.sudokuscope.core.solver;

import com.darren.sudokuscope.core.SudokuBoard;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;

/** Result of analysing a Sudoku board. */
public record SudokuAnalysis(
    SudokuBoard initialBoard,
    boolean valid,
    SolverStatus status,
    BigInteger solutionCount,
    Optional<SudokuBoard> exemplarSolution,
    boolean limitReached,
    long exploredNodes,
    String message) {

  public SudokuAnalysis {
    Objects.requireNonNull(initialBoard, "initialBoard");
    Objects.requireNonNull(solutionCount, "solutionCount");
    Objects.requireNonNull(exemplarSolution, "exemplarSolution");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(message, "message");
    if (solutionCount.signum() < 0) {
      throw new IllegalArgumentException("solutionCount must be non-negative");
    }
  }

  public boolean hasUniqueSolution() {
    return status == SolverStatus.UNIQUE_SOLUTION;
  }

  public boolean hasMultipleSolutions() {
    return status == SolverStatus.MULTIPLE_SOLUTIONS || status == SolverStatus.LIMIT_REACHED;
  }

  public static SudokuAnalysis invalid(SudokuBoard board, String message) {
    return new SudokuAnalysis(
        board, false, SolverStatus.INVALID, BigInteger.ZERO, Optional.empty(), false, 0L, message);
  }

  public static SudokuAnalysis emptyBoard(SudokuBoard board, BigInteger knownCount) {
    return new SudokuAnalysis(
        board,
        true,
        SolverStatus.MULTIPLE_SOLUTIONS,
        knownCount,
        Optional.empty(),
        false,
        0L,
        "Empty board has a known number of completions");
  }
}
