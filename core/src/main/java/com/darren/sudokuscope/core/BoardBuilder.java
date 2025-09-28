package com.darren.sudokuscope.core;

import java.util.List;
import java.util.Objects;

/** Fluent helper to create {@link SudokuBoard} instances from various inputs. */
public final class BoardBuilder {
  private final byte[] values = new byte[SudokuBoard.CELL_COUNT];

  public BoardBuilder() {}

  public BoardBuilder withRow(int row, int... digits) {
    Objects.requireNonNull(digits, "digits");
    if (row < 0 || row >= SudokuBoard.SIZE) {
      throw new IllegalArgumentException("Row index must be between 0 and 8 but was " + row);
    }
    if (digits.length != SudokuBoard.SIZE) {
      throw new IllegalArgumentException("Row requires exactly 9 digits but was " + digits.length);
    }
    for (int column = 0; column < digits.length; column++) {
      setInternal(new CellPosition(row, column), digits[column]);
    }
    return this;
  }

  public BoardBuilder withLinearValues(int... digits) {
    Objects.requireNonNull(digits, "digits");
    if (digits.length != SudokuBoard.CELL_COUNT) {
      throw new IllegalArgumentException("Expected 81 digits but got " + digits.length);
    }
    for (int i = 0; i < digits.length; i++) {
      setInternal(new CellPosition(i / SudokuBoard.SIZE, i % SudokuBoard.SIZE), digits[i]);
    }
    return this;
  }

  public BoardBuilder withStringRows(List<String> rows) {
    Objects.requireNonNull(rows, "rows");
    if (rows.size() != SudokuBoard.SIZE) {
      throw new IllegalArgumentException("Exactly 9 rows are required but was " + rows.size());
    }
    for (int row = 0; row < rows.size(); row++) {
      withRow(row, parseRow(rows.get(row)));
    }
    return this;
  }

  public BoardBuilder withCanonicalString(String canonical) {
    Objects.requireNonNull(canonical, "canonical");
    if (canonical.length() != SudokuBoard.CELL_COUNT) {
      throw new IllegalArgumentException("Canonical string must be 81 characters long");
    }
    for (int i = 0; i < canonical.length(); i++) {
      char c = canonical.charAt(i);
      if (c < '0' || c > '9') {
        throw new IllegalArgumentException(
            "Canonical string must only contain digits but found '" + c + "'");
      }
      values[i] = (byte) (c - '0');
    }
    return this;
  }

  public BoardBuilder withEntry(CellPosition position, int value) {
    setInternal(position, value);
    return this;
  }

  public SudokuBoard build() {
    return SudokuBoard.fromBytes(values.clone());
  }

  private void setInternal(CellPosition position, int value) {
    Objects.requireNonNull(position, "position");
    requireValueWithinRange(value);
    values[position.toIndex()] = (byte) value;
  }

  private static int[] parseRow(String row) {
    if (row.length() != SudokuBoard.SIZE) {
      throw new IllegalArgumentException("Each row must contain exactly 9 characters");
    }
    int[] digits = new int[SudokuBoard.SIZE];
    for (int i = 0; i < row.length(); i++) {
      char c = row.charAt(i);
      if (c == '.' || c == '0') {
        digits[i] = 0;
      } else if (c >= '1' && c <= '9') {
        digits[i] = c - '0';
      } else {
        throw new IllegalArgumentException("Unexpected character '" + c + "' at position " + i);
      }
    }
    return digits;
  }

  private static void requireValueWithinRange(int value) {
    if (value < 0 || value > 9) {
      throw new IllegalArgumentException("Value must be between 0 and 9 but was " + value);
    }
  }
}
