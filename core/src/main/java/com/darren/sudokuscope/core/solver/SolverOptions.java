package com.darren.sudokuscope.core.solver;

/** Options to tune solver behaviour. */
public record SolverOptions(
    int maxSolutions, boolean captureFirstSolution, boolean treatEmptyBoardAsKnown) {

  public SolverOptions {
    if (maxSolutions == 0) {
      throw new IllegalArgumentException("maxSolutions must be non-zero. Use -1 for unlimited.");
    }
  }

  public static SolverOptions defaultOptions() {
    return new SolverOptions(100_000, true, true);
  }

  public static SolverOptions findFirstSolution() {
    return new SolverOptions(1, true, true);
  }

  public static SolverOptions uniquenessProbe() {
    return new SolverOptions(2, true, true);
  }

  public boolean isUnlimited() {
    return maxSolutions < 0;
  }

  public SolverOptions withMaxSolutions(int newMax) {
    return new SolverOptions(newMax, captureFirstSolution, treatEmptyBoardAsKnown);
  }

  public SolverOptions withoutEmptyBoardShortcut() {
    return new SolverOptions(maxSolutions, captureFirstSolution, false);
  }
}
