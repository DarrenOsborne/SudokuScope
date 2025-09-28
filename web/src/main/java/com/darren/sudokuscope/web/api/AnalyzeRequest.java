package com.darren.sudokuscope.web.api;

import com.darren.sudokuscope.core.SudokuBoard;
import java.util.List;

public record AnalyzeRequest(List<Integer> cells) {
  public SudokuBoard toBoard() {
    if (cells == null || cells.size() != SudokuBoard.CELL_COUNT) {
      throw new IllegalArgumentException("Request must contain exactly 81 cells");
    }
    int[] values = new int[SudokuBoard.CELL_COUNT];
    for (int i = 0; i < cells.size(); i++) {
      Integer value = cells.get(i);
      values[i] = value == null ? 0 : value;
    }
    return SudokuBoard.fromArray(values);
  }
}
