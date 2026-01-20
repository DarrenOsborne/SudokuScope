package com.darren.sudokuscope.core.solver;

import com.darren.sudokuscope.core.SudokuBoard;
import com.darren.sudokuscope.core.SudokuFacts;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Random;

/** Searches for a puzzle whose solution count is closest to a target. */
public final class TargetPuzzleSearch {
  private static final int ALL_DIGITS_MASK = 0x1FF;
  private static final int MIN_CLUES = 10;
  private static final MathContext ESTIMATE_CONTEXT = new MathContext(40, RoundingMode.HALF_UP);

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
    Objects.requireNonNull(target, "target");
    if (target.signum() <= 0) {
      throw new IllegalArgumentException("target must be positive");
    }
    if (maxSolutions == 0) {
      throw new IllegalArgumentException("maxSolutions must be non-zero");
    }
    long boundedLimit = Math.max(500L, timeLimitMillis);
    Random random = new Random(seed);
    int solverLimit = resolveMaxSolutions(target, maxSolutions);
    SolverOptions options = new SolverOptions(solverLimit, false, true);
    long iterations = 0;
    long start = System.nanoTime();
    long deadline = start + boundedLimit * 1_000_000L;
    Candidate best = null;

    while (System.nanoTime() < deadline && !Thread.currentThread().isInterrupted()) {
      SudokuBoard solved = generateSolvedBoard(random);
      byte[] puzzle = solved.toByteArray();
      int[] removalOrder = shuffledCells(random);
      int clues = SudokuBoard.CELL_COUNT;

      Candidate current = evaluate(puzzle, target, options);
      iterations++;
      if (isBetter(current, best)) {
        best = current;
      }
      if (current.delta.equals(BigInteger.ZERO)) {
        break;
      }

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

        Candidate candidate = evaluate(puzzle, target, options);
        iterations++;
        if (isBetter(candidate, best)) {
          best = candidate;
        }
        if (candidate.delta.equals(BigInteger.ZERO)) {
          best = candidate;
          break;
        }
        if (isBetter(candidate, current)) {
          current = candidate;
          if (candidate.solutionCount.compareTo(target) > 0) {
            break;
          }
        } else {
          puzzle[cell] = previous;
          clues++;
        }
      }

      if (best != null && best.delta.equals(BigInteger.ZERO)) {
        break;
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

  private Candidate evaluate(byte[] puzzle, BigInteger target, SolverOptions options) {
    byte[] snapshot = puzzle.clone();
    SudokuBoard board = SudokuBoard.fromBytes(snapshot);
    SudokuAnalysis analysis = solver.analyze(board, options);
    BigInteger count = analysis.solutionCount();
    boolean approximate = analysis.limitReached();
    if (approximate) {
      BigInteger estimate = estimateSolutions(board);
      if (estimate.signum() > 0) {
        count = estimate;
      }
    }
    BigInteger delta = count.subtract(target).abs();
    return new Candidate(snapshot, count, approximate, delta, countClues(snapshot));
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
    int deltaCompare = candidate.delta.compareTo(baseline.delta);
    if (deltaCompare != 0) {
      return deltaCompare < 0;
    }
    if (candidate.approximate != baseline.approximate) {
      return !candidate.approximate;
    }
    return candidate.clueCount > baseline.clueCount;
  }

  private SudokuBoard generateSolvedBoard(Random random) {
    byte[] cells = new byte[SudokuBoard.CELL_COUNT];
    int[] rowMasks = new int[SudokuBoard.SIZE];
    int[] columnMasks = new int[SudokuBoard.SIZE];
    int[] boxMasks = new int[SudokuBoard.SIZE];
    int[] order = new int[SudokuBoard.CELL_COUNT];
    for (int i = 0; i < order.length; i++) {
      order[i] = i;
    }
    shuffle(order, random);
    if (!fillBoard(0, order, cells, rowMasks, columnMasks, boxMasks, random)) {
      throw new IllegalStateException("Failed to generate solved board");
    }
    return SudokuBoard.fromBytes(cells);
  }

  private boolean fillBoard(
      int index,
      int[] order,
      byte[] cells,
      int[] rowMasks,
      int[] columnMasks,
      int[] boxMasks,
      Random random) {
    if (index == order.length) {
      return true;
    }
    int cell = order[index];
    int row = cell / SudokuBoard.SIZE;
    int col = cell % SudokuBoard.SIZE;
    int box = boxIndex(row, col);
    int used = rowMasks[row] | columnMasks[col] | boxMasks[box];
    int candidates = (~used) & ALL_DIGITS_MASK;
    int[] digits = shuffledDigits(random);
    for (int digit : digits) {
      int bit = 1 << (digit - 1);
      if ((candidates & bit) == 0) {
        continue;
      }
      cells[cell] = (byte) digit;
      rowMasks[row] |= bit;
      columnMasks[col] |= bit;
      boxMasks[box] |= bit;
      if (fillBoard(index + 1, order, cells, rowMasks, columnMasks, boxMasks, random)) {
        return true;
      }
      cells[cell] = 0;
      rowMasks[row] &= ~bit;
      columnMasks[col] &= ~bit;
      boxMasks[box] &= ~bit;
    }
    return false;
  }

  private int[] shuffledCells(Random random) {
    int[] order = new int[SudokuBoard.CELL_COUNT];
    for (int i = 0; i < order.length; i++) {
      order[i] = i;
    }
    shuffle(order, random);
    return order;
  }

  private BigInteger estimateSolutions(SudokuBoard board) {
    BigDecimal estimate = new BigDecimal(SudokuFacts.TOTAL_COMPLETED_GRIDS);
    int[] rowMasks = new int[SudokuBoard.SIZE];
    int[] columnMasks = new int[SudokuBoard.SIZE];
    int[] boxMasks = new int[SudokuBoard.SIZE];
    boolean anyFilled = false;

    for (int row = 0; row < SudokuBoard.SIZE; row++) {
      for (int col = 0; col < SudokuBoard.SIZE; col++) {
        int value = board.valueAt(row, col);
        if (value == 0) {
          continue;
        }
        anyFilled = true;
        int box = boxIndex(row, col);
        int used = rowMasks[row] | columnMasks[col] | boxMasks[box];
        int candidates = (~used) & ALL_DIGITS_MASK;
        int candidateCount = Integer.bitCount(candidates);
        if (candidateCount == 0) {
          return BigInteger.ZERO;
        }
        estimate = estimate.divide(BigDecimal.valueOf(candidateCount), ESTIMATE_CONTEXT);
        int bit = 1 << (value - 1);
        rowMasks[row] |= bit;
        columnMasks[col] |= bit;
        boxMasks[box] |= bit;
      }
    }
    if (!anyFilled) {
      return SudokuFacts.TOTAL_COMPLETED_GRIDS;
    }
    return estimate.setScale(0, RoundingMode.HALF_UP).toBigInteger();
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

  private int boxIndex(int row, int column) {
    return (row / SudokuBoard.REGION_SIZE) * SudokuBoard.REGION_SIZE
        + (column / SudokuBoard.REGION_SIZE);
  }

  private static final class Candidate {
    private final byte[] puzzle;
    private final BigInteger solutionCount;
    private final boolean approximate;
    private final BigInteger delta;
    private final int clueCount;

    private Candidate(
        byte[] puzzle,
        BigInteger solutionCount,
        boolean approximate,
        BigInteger delta,
        int clueCount) {
      this.puzzle = puzzle;
      this.solutionCount = solutionCount;
      this.approximate = approximate;
      this.delta = delta;
      this.clueCount = clueCount;
    }
  }
}
