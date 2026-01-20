package com.darren.sudokuscope.core.solver;

/** Options to tune solver behaviour. */
public record SolverOptions(
    int maxSolutions,
    boolean captureFirstSolution,
    boolean treatEmptyBoardAsKnown,
    long deadlineNanos) {

  public SolverOptions {
    if (maxSolutions == 0) {
      throw new IllegalArgumentException("maxSolutions must be non-zero. Use -1 for unlimited.");
    }
    if (deadlineNanos < 0) {
      throw new IllegalArgumentException("deadlineNanos must be >= 0");
    }
  }

  public static SolverOptions defaultOptions() {
    return new SolverOptions(100_000, true, true, 0L);
  }

  public static SolverOptions findFirstSolution() {
    return new SolverOptions(1, true, true, 0L);
  }

  public static SolverOptions uniquenessProbe() {
    return new SolverOptions(2, true, true, 0L);
  }

  public boolean isUnlimited() {
    return maxSolutions < 0;
  }

  public SolverOptions withMaxSolutions(int newMax) {
    return new SolverOptions(newMax, captureFirstSolution, treatEmptyBoardAsKnown, deadlineNanos);
  }

  public SolverOptions withoutEmptyBoardShortcut() {
    return new SolverOptions(maxSolutions, captureFirstSolution, false, deadlineNanos);
  }

  public SolverOptions withDeadlineNanos(long newDeadlineNanos) {
    return new SolverOptions(maxSolutions, captureFirstSolution, treatEmptyBoardAsKnown, newDeadlineNanos);
  }

  public SolverOptions withTimeLimitMillis(long millis) {
    if (millis <= 0) {
      return withDeadlineNanos(0L);
    }
    long deadline = System.nanoTime() + millis * 1_000_000L;
    return withDeadlineNanos(deadline);
  }
}
