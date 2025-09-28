package com.darren.sudokuscope.core.command;

import com.darren.sudokuscope.core.BoardEvent;
import com.darren.sudokuscope.core.SudokuBoard;
import java.util.Objects;

/** Command abstraction for applying mutations to a {@link SudokuBoard} with undo support. */
public interface BoardCommand {
  MutationResult apply(SudokuBoard board);

  static BoardCommand noop() {
    return board -> MutationResult.identity(board);
  }

  record MutationResult(
      SudokuBoard previousBoard, SudokuBoard newBoard, BoardCommand undoCommand, BoardEvent event) {
    public MutationResult {
      Objects.requireNonNull(previousBoard, "previousBoard");
      Objects.requireNonNull(newBoard, "newBoard");
      Objects.requireNonNull(undoCommand, "undoCommand");
      Objects.requireNonNull(event, "event");
    }

    public static MutationResult identity(SudokuBoard board) {
      return new MutationResult(
          board, board, BoardCommand.noop(), new BoardEvent(board, board, null, 0, 0));
    }
  }
}
