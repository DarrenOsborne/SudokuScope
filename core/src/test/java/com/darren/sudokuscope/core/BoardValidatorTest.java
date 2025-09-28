package com.darren.sudokuscope.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BoardValidatorTest {
  @Test
  void detectsRowConflict() {
    SudokuBoard board = new BoardBuilder().withRow(0, 1, 1, 0, 0, 0, 0, 0, 0, 0).build();

    BoardValidator.ValidationResult result = BoardValidator.validate(board);
    assertThat(result.valid()).isFalse();
    assertThat(result.message()).contains("row");
  }

  @Test
  void validatesCorrectBoard() {
    SudokuBoard board =
        new BoardBuilder()
            .withStringRows(
                java.util.List.of(
                    "530070000",
                    "600195000",
                    "098000060",
                    "800060003",
                    "400803001",
                    "700020006",
                    "060000280",
                    "000419005",
                    "000080079"))
            .build();

    assertThat(BoardValidator.isValid(board)).isTrue();
  }
}
