package com.darren.sudokuscope.core;

import java.util.Objects;

/** Validates Sudoku boards according to standard Sudoku rules. */
public final class BoardValidator {
  private BoardValidator() {}

  public static ValidationResult validate(SudokuBoard board) {
    Objects.requireNonNull(board, "board");
    int[] rowMasks = new int[SudokuBoard.SIZE];
    int[] columnMasks = new int[SudokuBoard.SIZE];
    int[] boxMasks = new int[SudokuBoard.SIZE];

    for (int row = 0; row < SudokuBoard.SIZE; row++) {
      for (int col = 0; col < SudokuBoard.SIZE; col++) {
        int value = board.valueAt(row, col);
        if (value == 0) {
          continue;
        }
        int bit = 1 << (value - 1);
        int boxIndex = boxIndex(row, col);
        if ((rowMasks[row] & bit) != 0) {
          return ValidationResult.rowConflict(row, value);
        }
        if ((columnMasks[col] & bit) != 0) {
          return ValidationResult.columnConflict(col, value);
        }
        if ((boxMasks[boxIndex] & bit) != 0) {
          return ValidationResult.boxConflict(boxIndex, value);
        }
        rowMasks[row] |= bit;
        columnMasks[col] |= bit;
        boxMasks[boxIndex] |= bit;
      }
    }
    return ValidationResult.success();
  }

  public static boolean isValid(SudokuBoard board) {
    return validate(board).valid();
  }

  public static void requireValid(SudokuBoard board) {
    ValidationResult result = validate(board);
    if (!result.valid()) {
      throw new IllegalStateException(result.message());
    }
  }

  private static int boxIndex(int row, int column) {
    return (row / SudokuBoard.REGION_SIZE) * SudokuBoard.REGION_SIZE
        + (column / SudokuBoard.REGION_SIZE);
  }

  public record ValidationResult(boolean valid, String message) {
    public static ValidationResult success() {
      return new ValidationResult(true, "Board is valid");
    }

    public static ValidationResult rowConflict(int row, int value) {
      return new ValidationResult(false, "Duplicate value " + value + " in row " + (row + 1));
    }

    public static ValidationResult columnConflict(int column, int value) {
      return new ValidationResult(false, "Duplicate value " + value + " in column " + (column + 1));
    }

    public static ValidationResult boxConflict(int boxIndex, int value) {
      int boxRow = boxIndex / SudokuBoard.REGION_SIZE;
      int boxCol = boxIndex % SudokuBoard.REGION_SIZE;
      return new ValidationResult(
          false, "Duplicate value " + value + " in box " + (boxRow + 1) + "/" + (boxCol + 1));
    }
  }
}
