package com.darren.sudokuscope.core;

/** Immutable event describing a single mutation of a {@link SudokuBoard}. */
public record BoardEvent(
    SudokuBoard previousBoard,
    SudokuBoard newBoard,
    CellPosition position,
    int previousValue,
    int newValue) {

  public boolean isNoOp() {
    return previousValue == newValue;
  }
}
