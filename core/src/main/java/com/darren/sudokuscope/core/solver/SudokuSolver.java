package com.darren.sudokuscope.core.solver;

import com.darren.sudokuscope.core.SudokuBoard;
import java.util.Optional;

/** Contract for Sudoku solving strategies. */
public interface SudokuSolver {
  SudokuAnalysis analyze(SudokuBoard board, SolverOptions options);

  default SudokuAnalysis analyze(SudokuBoard board) {
    return analyze(board, SolverOptions.defaultOptions());
  }

  default Optional<SudokuBoard> solve(SudokuBoard board) {
    SudokuAnalysis analysis = analyze(board, SolverOptions.findFirstSolution());
    return analysis.exemplarSolution();
  }

  default SudokuBoard solveOrThrow(SudokuBoard board) {
    return solve(board)
        .orElseThrow(() -> new IllegalStateException("Board does not have a valid solution"));
  }

  static SudokuSolver createDefault() {
    return new BacktrackingSudokuSolver();
  }
}
