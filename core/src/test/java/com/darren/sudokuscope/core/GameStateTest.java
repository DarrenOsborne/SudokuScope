package com.darren.sudokuscope.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.darren.sudokuscope.core.command.SetValueCommand;
import org.junit.jupiter.api.Test;

class GameStateTest {
  @Test
  void undoAndRedoWorkAsExpected() {
    GameState state = new GameState();
    state.apply(new SetValueCommand(new CellPosition(0, 0), 5));
    state.apply(new SetValueCommand(new CellPosition(1, 1), 9));

    assertThat(state.board().valueAt(0, 0)).isEqualTo(5);
    assertThat(state.board().valueAt(1, 1)).isEqualTo(9);

    state.undo();
    assertThat(state.board().valueAt(1, 1)).isZero();

    state.redo();
    assertThat(state.board().valueAt(1, 1)).isEqualTo(9);
  }

  @Test
  void noOpCommandsDoNotModifyHistory() {
    GameState state = new GameState();
    CellPosition position = new CellPosition(0, 0);

    BoardEvent initialEvent = state.apply(new SetValueCommand(position, 0));
    assertThat(initialEvent.isNoOp()).isTrue();
    assertThat(state.canUndo()).isFalse();

    state.apply(new SetValueCommand(position, 5));
    assertThat(state.board().valueAt(0, 0)).isEqualTo(5);
    assertThat(state.canUndo()).isTrue();

    state.apply(new SetValueCommand(position, 5));
    assertThat(state.canUndo()).isTrue();

    state.undo();
    assertThat(state.board().valueAt(0, 0)).isZero();
    assertThat(state.canUndo()).isFalse();
    assertThat(state.canRedo()).isTrue();

    state.apply(new SetValueCommand(position, 0));
    assertThat(state.canUndo()).isFalse();
    assertThat(state.canRedo()).isTrue();
  }
}
