package com.darren.sudokuscope.core;

import java.math.BigInteger;

/** Collection of mathematical facts about the standard 9x9 Sudoku universe. */
public final class SudokuFacts {
  /** Total number of distinct completed 9x9 Sudoku grids. */
  public static final BigInteger TOTAL_COMPLETED_GRIDS = new BigInteger("6670903752021072936960");

  private SudokuFacts() {
    // Utility class
  }
}
