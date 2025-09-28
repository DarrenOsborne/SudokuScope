package com.darren.sudokuscope.core;

/** Immutable representation of a zero-indexed cell position on a Sudoku board. */
public record CellPosition(int row, int column) {
  public CellPosition {
    if (row < 0 || row >= SudokuBoard.SIZE) {
      throw new IllegalArgumentException("Row must be between 0 and 8 but was " + row);
    }
    if (column < 0 || column >= SudokuBoard.SIZE) {
      throw new IllegalArgumentException("Column must be between 0 and 8 but was " + column);
    }
  }

  public int toIndex() {
    return row * SudokuBoard.SIZE + column;
  }
}
