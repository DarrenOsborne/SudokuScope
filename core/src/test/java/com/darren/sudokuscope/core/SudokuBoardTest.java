package com.darren.sudokuscope.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SudokuBoardTest {
  @Test
  void withValueReturnsNewInstance() {
    SudokuBoard board = SudokuBoard.empty();
    SudokuBoard updated = board.withValue(new CellPosition(0, 0), 5);

    assertThat(board).isNotSameAs(updated);
    assertThat(board.valueAt(0, 0)).isZero();
    assertThat(updated.valueAt(0, 0)).isEqualTo(5);
  }

  @Test
  void builderCreatesExpectedBoard() {
    SudokuBoard board =
        new BoardBuilder()
            .withStringRows(
                java.util.List.of(
                    "400000805",
                    "030000000",
                    "000700000",
                    "020000060",
                    "000080400",
                    "000010000",
                    "000603070",
                    "500200000",
                    "104000000"))
            .build();

    assertThat(board.valueAt(0, 0)).isEqualTo(4);
    assertThat(board.valueAt(8, 0)).isEqualTo(1);
    assertThat(board.valueAt(0, 1)).isZero();
  }
}
