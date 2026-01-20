package com.darren.sudokuscope.core.solver;

import com.darren.sudokuscope.core.BoardValidator;
import com.darren.sudokuscope.core.CellPosition;
import com.darren.sudokuscope.core.SudokuBoard;
import com.darren.sudokuscope.core.SudokuFacts;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class BacktrackingSudokuSolver implements SudokuSolver {
  private static final int ALL_DIGITS_MASK = 0x1FF; // 9 bits set

  @Override
  public SudokuAnalysis analyze(SudokuBoard board, SolverOptions options) {
    Objects.requireNonNull(board, "board");
    Objects.requireNonNull(options, "options");

    BoardValidator.ValidationResult validation = BoardValidator.validate(board);
    if (!validation.valid()) {
      return SudokuAnalysis.invalid(board, validation.message());
    }

    if (board.isEmptyBoard() && options.treatEmptyBoardAsKnown()) {
      return SudokuAnalysis.emptyBoard(board, SudokuFacts.TOTAL_COMPLETED_GRIDS);
    }

    if (board.isComplete()) {
      return new SudokuAnalysis(
          board,
          true,
          SolverStatus.UNIQUE_SOLUTION,
          BigInteger.ONE,
          Optional.of(board),
          false,
          0L,
          "Board already solved");
    }

    SearchState state = new SearchState(board, options);
    state.search();

    SolverStatus status;
    if (state.limitReached) {
      status = SolverStatus.LIMIT_REACHED;
    } else if (state.solutionCount == 0) {
      status = SolverStatus.NO_SOLUTION;
    } else if (state.solutionCount == 1) {
      status = SolverStatus.UNIQUE_SOLUTION;
    } else {
      status = SolverStatus.MULTIPLE_SOLUTIONS;
    }

    Optional<SudokuBoard> solution =
        state.firstSolution == null
            ? Optional.empty()
            : Optional.of(SudokuBoard.fromBytes(state.firstSolution));

    return new SudokuAnalysis(
        board,
        true,
        status,
        BigInteger.valueOf(state.solutionCount),
        solution,
        state.limitReached,
        state.visitedNodes,
        state.message);
  }

  private static final class SearchState {
    private final SolverOptions options;
    private final int limit;
    private final long deadlineNanos;
    private final byte[] working;
    private byte[] firstSolution;
    private final int[] rowMasks = new int[SudokuBoard.SIZE];
    private final int[] columnMasks = new int[SudokuBoard.SIZE];
    private final int[] boxMasks = new int[SudokuBoard.SIZE];
    private final int[] emptyPositions;
    private final List<CellPosition> emptyOrder;
    private final int[] forcedCells;
    private final int[] forcedBits;
    private final int[] forcedDigits;
    private final int[] forcedSwap;
    private int forcedTop;
    private long solutionCount;
    private boolean limitReached;
    private boolean timeLimitReached;
    private boolean interrupted;
    private long visitedNodes;
    private String message = "Search completed";

    private SearchState(SudokuBoard board, SolverOptions options) {
      this.options = options;
      this.limit = options.isUnlimited() ? Integer.MAX_VALUE : options.maxSolutions();
      this.deadlineNanos = options.deadlineNanos();
      this.working = board.toByteArray();
      this.emptyOrder = new ArrayList<>();
      List<Integer> empties = new ArrayList<>();

      for (int row = 0; row < SudokuBoard.SIZE; row++) {
        for (int col = 0; col < SudokuBoard.SIZE; col++) {
          int value = board.valueAt(row, col);
          if (value == 0) {
            empties.add(row * SudokuBoard.SIZE + col);
            emptyOrder.add(new CellPosition(row, col));
          } else {
            int bit = 1 << (value - 1);
            rowMasks[row] |= bit;
            columnMasks[col] |= bit;
            boxMasks[boxIndex(row, col)] |= bit;
          }
        }
      }
      this.emptyPositions = empties.stream().mapToInt(Integer::intValue).toArray();
      this.forcedCells = new int[emptyPositions.length];
      this.forcedBits = new int[emptyPositions.length];
      this.forcedDigits = new int[emptyPositions.length];
      this.forcedSwap = new int[emptyPositions.length];
    }

    private void search() {
      backtrack(0);
      if (interrupted) {
        message = "Stopped due to interruption";
      } else if (timeLimitReached) {
        message = "Stopped after reaching time limit";
      } else if (limitReached) {
        message = "Stopped after reaching maxSolutions=" + limit;
      } else if (solutionCount == 0) {
        message = "No solutions found";
      } else {
        message = "Enumerated " + solutionCount + " solution(s)";
      }
    }

    private void backtrack(int depth) {
      if (shouldStop()) {
        return;
      }
      if (depth == emptyPositions.length) {
        solutionCount++;
        if (firstSolution == null && options.captureFirstSolution()) {
          firstSolution = working.clone();
        }
        if (!options.isUnlimited() && solutionCount >= limit) {
          limitReached = true;
        }
        return;
      }

      int forcedStart = forcedTop;
      int forcedCount = propagateSingles(depth);
      if (forcedCount < 0) {
        return;
      }
      if (shouldStop()) {
        undoForced(depth, forcedStart);
        return;
      }
      int nextDepth = depth + forcedCount;
      if (nextDepth == emptyPositions.length) {
        solutionCount++;
        if (firstSolution == null && options.captureFirstSolution()) {
          firstSolution = working.clone();
        }
        if (!options.isUnlimited() && solutionCount >= limit) {
          limitReached = true;
        }
        undoForced(depth, forcedStart);
        return;
      }

      int pivotIndex = selectPivot(nextDepth);
      if (pivotIndex < 0) {
        undoForced(depth, forcedStart);
        return; // dead end
      }
      swap(emptyPositions, nextDepth, pivotIndex);
      int cell = emptyPositions[nextDepth];
      int row = cell / SudokuBoard.SIZE;
      int col = cell % SudokuBoard.SIZE;
      int box = boxIndex(row, col);
      int usedMask = rowMasks[row] | columnMasks[col] | boxMasks[box];
      int candidates = (~usedMask) & ALL_DIGITS_MASK;
      if (candidates == 0) {
        swap(emptyPositions, nextDepth, pivotIndex);
        undoForced(depth, forcedStart);
        return;
      }

      while (candidates != 0) {
        int bit = candidates & -candidates;
        candidates &= candidates - 1;
        int digit = Integer.numberOfTrailingZeros(bit) + 1;
        place(row, col, box, cell, digit, bit);
        visitedNodes++;
        backtrack(nextDepth + 1);
        remove(row, col, box, cell, digit, bit);
        if (shouldStop()) {
          break;
        }
      }
      swap(emptyPositions, nextDepth, pivotIndex);
      undoForced(depth, forcedStart);
    }

    private boolean shouldStop() {
      if (limitReached) {
        return true;
      }
      if (deadlineNanos > 0 && System.nanoTime() >= deadlineNanos) {
        timeLimitReached = true;
        limitReached = true;
        return true;
      }
      if (Thread.currentThread().isInterrupted()) {
        interrupted = true;
        limitReached = true;
        return true;
      }
      return false;
    }

    private int propagateSingles(int depth) {
      int start = forcedTop;
      boolean progress;
      do {
        progress = false;
        int filled = forcedTop - start;
        for (int i = depth + filled; i < emptyPositions.length; i++) {
          int cell = emptyPositions[i];
          int row = cell / SudokuBoard.SIZE;
          int col = cell % SudokuBoard.SIZE;
          int box = boxIndex(row, col);
          int candidates = (~(rowMasks[row] | columnMasks[col] | boxMasks[box])) & ALL_DIGITS_MASK;
          if (candidates == 0) {
            undoForced(depth, start);
            return -1;
          }
          if ((candidates & (candidates - 1)) == 0) {
            int bit = candidates;
            int digit = Integer.numberOfTrailingZeros(bit) + 1;
            int target = depth + filled;
            swap(emptyPositions, target, i);
            forcedSwap[forcedTop] = i;
            forcedCells[forcedTop] = cell;
            forcedBits[forcedTop] = bit;
            forcedDigits[forcedTop] = digit;
            forcedTop++;
            place(row, col, box, cell, digit, bit);
            progress = true;
            break;
          }
        }
      } while (progress);
      return forcedTop - start;
    }

    private void undoForced(int depth, int start) {
      for (int i = forcedTop - 1; i >= start; i--) {
        int cell = forcedCells[i];
        int row = cell / SudokuBoard.SIZE;
        int col = cell % SudokuBoard.SIZE;
        int box = boxIndex(row, col);
        remove(row, col, box, cell, forcedDigits[i], forcedBits[i]);
        int target = depth + (i - start);
        swap(emptyPositions, target, forcedSwap[i]);
      }
      forcedTop = start;
    }

    private int selectPivot(int depth) {
      int bestIndex = -1;
      int bestCount = Integer.MAX_VALUE;
      for (int i = depth; i < emptyPositions.length; i++) {
        int cell = emptyPositions[i];
        int row = cell / SudokuBoard.SIZE;
        int col = cell % SudokuBoard.SIZE;
        int box = boxIndex(row, col);
        int candidates = (~(rowMasks[row] | columnMasks[col] | boxMasks[box])) & ALL_DIGITS_MASK;
        if (candidates == 0) {
          swap(emptyPositions, depth, i);
          return depth;
        }
        int count = Integer.bitCount(candidates);
        if (count < bestCount) {
          bestCount = count;
          bestIndex = i;
          if (count == 1) {
            break;
          }
        }
      }
      return bestIndex;
    }

    private void place(int row, int col, int box, int cell, int digit, int bit) {
      working[cell] = (byte) digit;
      rowMasks[row] |= bit;
      columnMasks[col] |= bit;
      boxMasks[box] |= bit;
    }

    private void remove(int row, int col, int box, int cell, int digit, int bit) {
      working[cell] = 0;
      rowMasks[row] &= ~bit;
      columnMasks[col] &= ~bit;
      boxMasks[box] &= ~bit;
    }

    private static void swap(int[] array, int i, int j) {
      if (i == j) {
        return;
      }
      int tmp = array[i];
      array[i] = array[j];
      array[j] = tmp;
    }

    private static int boxIndex(int row, int column) {
      return (row / SudokuBoard.REGION_SIZE) * SudokuBoard.REGION_SIZE
          + (column / SudokuBoard.REGION_SIZE);
    }
  }
}
