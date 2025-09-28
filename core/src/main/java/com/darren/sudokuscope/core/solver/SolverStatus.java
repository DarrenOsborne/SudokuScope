package com.darren.sudokuscope.core.solver;

/** High-level solver verdict for a Sudoku board. */
public enum SolverStatus {
  INVALID,
  NO_SOLUTION,
  UNIQUE_SOLUTION,
  MULTIPLE_SOLUTIONS,
  LIMIT_REACHED
}
