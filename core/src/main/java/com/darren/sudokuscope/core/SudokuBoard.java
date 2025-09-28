package com.darren.sudokuscope.core;

import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.BiConsumer;

/** Immutable snapshot of a 9x9 Sudoku board. Values are 0 (empty) or 1-9. */
public final class SudokuBoard {
  public static final int SIZE = 9;
  public static final int REGION_SIZE = 3;
  public static final int CELL_COUNT = SIZE * SIZE;

  private final byte[] cells;

  private SudokuBoard(byte[] cells, boolean clone) {
    this.cells = clone ? cells.clone() : cells;
  }

  public static SudokuBoard empty() {
    return new SudokuBoard(new byte[CELL_COUNT], false);
  }

  public static SudokuBoard fromArray(int[] values) {
    Objects.requireNonNull(values, "values");
    if (values.length != CELL_COUNT) {
      throw new IllegalArgumentException("Expected 81 values but got " + values.length);
    }
    byte[] data = new byte[CELL_COUNT];
    for (int i = 0; i < values.length; i++) {
      int value = values[i];
      requireWithinRange(value);
      data[i] = (byte) value;
    }
    return new SudokuBoard(data, false);
  }

  public static SudokuBoard fromBytes(byte[] values) {
    Objects.requireNonNull(values, "values");
    if (values.length != CELL_COUNT) {
      throw new IllegalArgumentException("Expected 81 values but got " + values.length);
    }
    byte[] copy = values.clone();
    for (byte value : copy) {
      requireWithinRange(Byte.toUnsignedInt(value));
    }
    return new SudokuBoard(copy, false);
  }

  public byte[] toByteArray() {
    return cells.clone();
  }

  public int[] toIntArray() {
    int[] copy = new int[CELL_COUNT];
    for (int i = 0; i < cells.length; i++) {
      copy[i] = Byte.toUnsignedInt(cells[i]);
    }
    return copy;
  }

  public SudokuBoard withValue(CellPosition position, int value) {
    Objects.requireNonNull(position, "position");
    requireWithinRange(value);
    int index = position.toIndex();
    int current = Byte.toUnsignedInt(cells[index]);
    if (current == value) {
      return this;
    }
    byte[] updated = cells.clone();
    updated[index] = (byte) value;
    return new SudokuBoard(updated, false);
  }

  public SudokuBoard clear(CellPosition position) {
    return withValue(position, 0);
  }

  public int valueAt(CellPosition position) {
    Objects.requireNonNull(position, "position");
    return valueAt(position.row(), position.column());
  }

  public int valueAt(int row, int column) {
    return Byte.toUnsignedInt(cells[row * SIZE + column]);
  }

  public boolean isEmpty(CellPosition position) {
    return valueAt(position) == 0;
  }

  public boolean isEmptyBoard() {
    for (byte cell : cells) {
      if (cell != 0) {
        return false;
      }
    }
    return true;
  }

  public boolean isComplete() {
    for (byte cell : cells) {
      if (cell == 0) {
        return false;
      }
    }
    return true;
  }

  public void forEachCell(BiConsumer<CellPosition, Integer> consumer) {
    Objects.requireNonNull(consumer, "consumer");
    for (int row = 0; row < SIZE; row++) {
      for (int col = 0; col < SIZE; col++) {
        consumer.accept(new CellPosition(row, col), valueAt(row, col));
      }
    }
  }

  public String toCanonicalString() {
    StringBuilder builder = new StringBuilder(CELL_COUNT);
    for (byte cell : cells) {
      builder.append((char) ('0' + Byte.toUnsignedInt(cell)));
    }
    return builder.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    return Arrays.equals(cells, ((SudokuBoard) obj).cells);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(cells);
  }

  @Override
  public String toString() {
    StringJoiner joiner = new StringJoiner(System.lineSeparator());
    for (int row = 0; row < SIZE; row++) {
      StringBuilder builder = new StringBuilder();
      for (int col = 0; col < SIZE; col++) {
        if (col > 0) {
          builder.append(' ');
        }
        int value = valueAt(row, col);
        builder.append(value == 0 ? '.' : value);
      }
      joiner.add(builder.toString());
    }
    return joiner.toString();
  }

  private static void requireWithinRange(int value) {
    if (value < 0 || value > 9) {
      throw new IllegalArgumentException("Cell values must be between 0 and 9 but was " + value);
    }
  }
}
