package com.darren.sudokuscope.web.api;

import com.darren.sudokuscope.core.SudokuBoard;
import com.darren.sudokuscope.core.solver.TargetPuzzleSearch;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public record TargetCountResponse(
    List<Integer> board,
    String solutionCount,
    boolean approximate,
    String delta,
    long elapsedMillis,
    long iterations,
    String message) {

  private static final DecimalFormat NUMBER_FORMAT =
      (DecimalFormat) NumberFormat.getIntegerInstance(Locale.US);

  static {
    NUMBER_FORMAT.setGroupingUsed(true);
    NUMBER_FORMAT.setMaximumFractionDigits(0);
    NUMBER_FORMAT.setMinimumFractionDigits(0);
  }

  public static TargetCountResponse from(TargetPuzzleSearch.SearchResult result) {
    String formattedCount = format(result.solutionCount());
    String formattedDelta = format(result.delta());
    String message =
        "Closest count " + formattedCount + " (delta " + formattedDelta + ") in "
            + result.elapsedMillis() + "ms";
    return new TargetCountResponse(
        toList(result.board()),
        formattedCount,
        result.approximate(),
        formattedDelta,
        result.elapsedMillis(),
        result.iterations(),
        message);
  }

  private static List<Integer> toList(SudokuBoard board) {
    return java.util.Arrays.stream(board.toIntArray()).boxed().toList();
  }

  private static String format(BigInteger value) {
    return NUMBER_FORMAT.format(value);
  }
}
