package com.darren.sudokuscope.core.solver;

import com.darren.sudokuscope.core.SudokuBoard;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Random;

/** Searches for a puzzle whose solution count is closest to a target. */
public final class TargetPuzzleSearch {
  private static final int BASE = 3;
  private static final int SIDE = BASE * BASE;
  private static final int MIN_CLUES = 10;

  private final SudokuSolver solver = SudokuSolver.createDefault();

  public record SearchResult(
      SudokuBoard board,
      BigInteger solutionCount,
      boolean approximate,
      long iterations,
      long elapsedMillis,
      BigInteger delta) {}

  public SearchResult findClosest(
      BigInteger target, long timeLimitMillis, int maxSolutions, long seed) {
    SudokuBoard solved = generateRandomSolved(timeLimitMillis, seed);
    if (solved == null) {
      return new SearchResult(SudokuBoard.empty(), BigInteger.ZERO, true, 0L, 0L, target);
    }
    return findClosestFromSolved(solved, target, timeLimitMillis, maxSolutions, seed);
  }

  public SudokuBoard generateRandomSolved(long timeLimitMillis, long seed) {
    long deadline =
        timeLimitMillis <= 0 ? 0L : System.nanoTime() + timeLimitMillis * 1_000_000L;
    Random random = new Random(seed);
    return generateSolvedBoard(random, deadline);
  }

  public SearchResult findClosestFromSolved(
      SudokuBoard solved, BigInteger target, long timeLimitMillis, int maxSolutions, long seed) {
    Objects.requireNonNull(solved, "solved");
    Objects.requireNonNull(target, "target");
    if (target.signum() <= 0) {
      throw new IllegalArgumentException("target must be positive");
    }
    if (maxSolutions == 0) {
      throw new IllegalArgumentException("maxSolutions must be non-zero");
    }
    long boundedLimit = Math.max(500L, timeLimitMillis);
    long start = System.nanoTime();
    long deadline = start + boundedLimit * 1_000_000L;
    int solverLimit = resolveMaxSolutions(target, maxSolutions);
    boolean limitImpliesOverTarget = limitImpliesOverTarget(target, solverLimit);
    SolverOptions options = new SolverOptions(solverLimit, false, true, 0L);
    Random random = new Random(seed);
    long iterations = 0;

    byte[] solvedBytes = solved.toByteArray();
    Candidate best = evaluate(solvedBytes, target, options, deadline, limitImpliesOverTarget);
    iterations++;
    if (!best.approximate && best.delta.equals(BigInteger.ZERO)) {
      long elapsedMillis = (System.nanoTime() - start) / 1_000_000L;
      return new SearchResult(
          SudokuBoard.fromBytes(best.puzzle),
          best.solutionCount,
          best.approximate,
          iterations,
          elapsedMillis,
          best.delta);
    }
    if (target.equals(BigInteger.ONE)) {
      long elapsedMillis = (System.nanoTime() - start) / 1_000_000L;
      return new SearchResult(
          SudokuBoard.fromBytes(best.puzzle),
          best.solutionCount,
          best.approximate,
          iterations,
          elapsedMillis,
          best.delta);
    }

    while (System.nanoTime() < deadline && !Thread.currentThread().isInterrupted()) {
      byte[] puzzle = solvedBytes.clone();
      int[] removalOrder = shuffledCells(random);
      int clues = SudokuBoard.CELL_COUNT;

      for (int cell : removalOrder) {
        if (System.nanoTime() >= deadline || Thread.currentThread().isInterrupted()) {
          break;
        }
        if (clues <= MIN_CLUES) {
          break;
        }
        if (puzzle[cell] == 0) {
          continue;
        }
        byte previous = puzzle[cell];
        puzzle[cell] = 0;
        clues--;

        Candidate candidate =
            evaluate(puzzle, target, options, deadline, limitImpliesOverTarget);
        iterations++;
        if (candidate.approximate || candidate.overTarget) {
          puzzle[cell] = previous;
          clues++;
          continue;
        }
        if (isBetter(candidate, best)) {
          best = candidate;
        }
        if (candidate.delta.equals(BigInteger.ZERO)) {
          long elapsedMillis = (System.nanoTime() - start) / 1_000_000L;
          return new SearchResult(
              SudokuBoard.fromBytes(best.puzzle),
              best.solutionCount,
              best.approximate,
              iterations,
              elapsedMillis,
              best.delta);
        }
      }
    }

    long elapsedMillis = (System.nanoTime() - start) / 1_000_000L;
    return new SearchResult(
        SudokuBoard.fromBytes(best.puzzle),
        best.solutionCount,
        best.approximate,
        iterations,
        elapsedMillis,
        best.delta);
  }

  private Candidate evaluate(
      byte[] puzzle,
      BigInteger target,
      SolverOptions options,
      long deadlineNanos,
      boolean limitImpliesOverTarget) {
    byte[] snapshot = puzzle.clone();
    SudokuBoard board = SudokuBoard.fromBytes(snapshot);
    SolverOptions timedOptions = options.withDeadlineNanos(deadlineNanos);
    SudokuAnalysis analysis = solver.analyze(board, timedOptions);
    BigInteger count = analysis.solutionCount();
    boolean approximate = analysis.limitReached();
    boolean maxSolutionsHit =
        !options.isUnlimited()
            && approximate
            && count.compareTo(BigInteger.valueOf(options.maxSolutions())) >= 0;
    boolean overTarget =
        (!approximate && count.compareTo(target) > 0)
            || (maxSolutionsHit && limitImpliesOverTarget);
    BigInteger delta = count.subtract(target).abs();
    return new Candidate(snapshot, count, approximate, overTarget, delta, countClues(snapshot));
  }

  private int resolveMaxSolutions(BigInteger target, int maxSolutions) {
    if (maxSolutions < 0) {
      return maxSolutions;
    }
    if (target.compareTo(BigInteger.valueOf(Integer.MAX_VALUE - 1L)) <= 0) {
      int targetInt = target.intValue();
      int desired = Math.max(2, targetInt + 1);
      if (desired < maxSolutions) {
        return desired;
      }
    }
    return maxSolutions;
  }

  private boolean isBetter(Candidate candidate, Candidate baseline) {
    if (baseline == null) {
      return true;
    }
    if (baseline.approximate && !candidate.approximate) {
      return true;
    }
    if (!baseline.approximate && candidate.approximate) {
      return false;
    }
    int deltaCompare = candidate.delta.compareTo(baseline.delta);
    if (deltaCompare != 0) {
      return deltaCompare < 0;
    }
    return candidate.clueCount > baseline.clueCount;
  }

  private SudokuBoard generateSolvedBoard(Random random, long deadlineNanos) {
    if (deadlineNanos > 0 && System.nanoTime() >= deadlineNanos) {
      return null;
    }
    if (Thread.currentThread().isInterrupted()) {
      return null;
    }
    int[] bands = shuffledRange(BASE, random);
    int[] stacks = shuffledRange(BASE, random);
    int[] rows = expandGroups(bands, random);
    int[] cols = expandGroups(stacks, random);
    int[] digits = shuffledDigits(random);

    byte[] cells = new byte[SudokuBoard.CELL_COUNT];
    int index = 0;
    for (int r = 0; r < SIDE; r++) {
      for (int c = 0; c < SIDE; c++) {
        int value = pattern(rows[r], cols[c]) + 1;
        cells[index++] = (byte) digits[value - 1];
      }
    }
    return SudokuBoard.fromBytes(cells);
  }

  private int[] shuffledCells(Random random) {
    int[] order = new int[SudokuBoard.CELL_COUNT];
    for (int i = 0; i < order.length; i++) {
      order[i] = i;
    }
    shuffle(order, random);
    return order;
  }

  private boolean limitImpliesOverTarget(BigInteger target, int solverLimit) {
    if (solverLimit <= 0) {
      return false;
    }
    if (target.compareTo(BigInteger.valueOf(Integer.MAX_VALUE - 1L)) > 0) {
      return false;
    }
    int targetPlusOne = target.intValue() + 1;
    return solverLimit == targetPlusOne;
  }

  private int countClues(byte[] puzzle) {
    int count = 0;
    for (byte value : puzzle) {
      if (value != 0) {
        count++;
      }
    }
    return count;
  }

  private void shuffle(int[] array, Random random) {
    for (int i = array.length - 1; i > 0; i--) {
      int j = random.nextInt(i + 1);
      int tmp = array[i];
      array[i] = array[j];
      array[j] = tmp;
    }
  }

  private int[] shuffledDigits(Random random) {
    int[] digits = new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9};
    shuffle(digits, random);
    return digits;
  }

  private int pattern(int row, int col) {
    return (BASE * (row % BASE) + (row / BASE) + col) % SIDE;
  }

  private int[] shuffledRange(int size, Random random) {
    int[] values = new int[size];
    for (int i = 0; i < size; i++) {
      values[i] = i;
    }
    shuffle(values, random);
    return values;
  }

  private int[] expandGroups(int[] groups, Random random) {
    int[] result = new int[SIDE];
    int index = 0;
    for (int group : groups) {
      int[] inner = shuffledRange(BASE, random);
      for (int offset : inner) {
        result[index++] = group * BASE + offset;
      }
    }
    return result;
  }

  private static final class Candidate {
    private final byte[] puzzle;
    private final BigInteger solutionCount;
    private final boolean approximate;
    private final boolean overTarget;
    private final BigInteger delta;
    private final int clueCount;

    private Candidate(
        byte[] puzzle,
        BigInteger solutionCount,
        boolean approximate,
        boolean overTarget,
        BigInteger delta,
        int clueCount) {
      this.puzzle = puzzle;
      this.solutionCount = solutionCount;
      this.approximate = approximate;
      this.overTarget = overTarget;
      this.delta = delta;
      this.clueCount = clueCount;
    }
  }
}
