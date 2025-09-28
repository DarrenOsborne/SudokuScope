package com.darren.sudokuscope.core.command;

import com.darren.sudokuscope.core.BoardEvent;
import com.darren.sudokuscope.core.CellPosition;
import com.darren.sudokuscope.core.SudokuBoard;
import java.util.Objects;

/** Command to set a particular cell to a specific value (0 clears the cell). */
public final class SetValueCommand implements BoardCommand {
  private final CellPosition position;
  private final int value;

  public SetValueCommand(CellPosition position, int value) {
    this.position = Objects.requireNonNull(position, "position");
    if (value < 0 || value > 9) {
      throw new IllegalArgumentException("Value must be between 0 and 9 but was " + value);
    }
    this.value = value;
  }

  public CellPosition position() {
    return position;
  }

  public int value() {
    return value;
  }

  @Override
  public MutationResult apply(SudokuBoard board) {
    int previousValue = board.valueAt(position);
    SudokuBoard updated = board.withValue(position, value);
    BoardEvent event = new BoardEvent(board, updated, position, previousValue, value);
    BoardCommand undo = new SetValueCommand(position, previousValue);
    return new MutationResult(board, updated, undo, event);
  }
}
