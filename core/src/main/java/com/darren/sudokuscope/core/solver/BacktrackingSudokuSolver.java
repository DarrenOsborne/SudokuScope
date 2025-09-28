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
    if (state.solutionCount == 0) {
      status = SolverStatus.NO_SOLUTION;
    } else if (state.limitReached) {
      status = SolverStatus.LIMIT_REACHED;
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
    private final byte[] working;
    private byte[] firstSolution;
    private final int[] rowMasks = new int[SudokuBoard.SIZE];
    private final int[] columnMasks = new int[SudokuBoard.SIZE];
    private final int[] boxMasks = new int[SudokuBoard.SIZE];
    private final int[] emptyPositions;
    private final List<CellPosition> emptyOrder;
    private long solutionCount;
    private boolean limitReached;
    private long visitedNodes;
    private String message = "Search completed";

    private SearchState(SudokuBoard board, SolverOptions options) {
      this.options = options;
      this.limit = options.isUnlimited() ? Integer.MAX_VALUE : options.maxSolutions();
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
    }

    private void search() {
      backtrack(0);
      if (limitReached) {
        message = "Stopped after reaching maxSolutions=" + limit;
      } else if (solutionCount == 0) {
        message = "No solutions found";
      } else {
        message = "Enumerated " + solutionCount + " solution(s)";
      }
    }

    private void backtrack(int depth) {
      if (limitReached) {
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

      int pivotIndex = selectPivot(depth);
      if (pivotIndex < 0) {
        return; // dead end
      }
      swap(emptyPositions, depth, pivotIndex);
      int cell = emptyPositions[depth];
      int row = cell / SudokuBoard.SIZE;
      int col = cell % SudokuBoard.SIZE;
      int box = boxIndex(row, col);
      int usedMask = rowMasks[row] | columnMasks[col] | boxMasks[box];
      int candidates = (~usedMask) & ALL_DIGITS_MASK;
      if (candidates == 0) {
        swap(emptyPositions, depth, pivotIndex);
        return;
      }

      while (candidates != 0) {
        int bit = candidates & -candidates;
        candidates &= candidates - 1;
        int digit = Integer.numberOfTrailingZeros(bit) + 1;
        place(row, col, box, cell, digit, bit);
        visitedNodes++;
        backtrack(depth + 1);
        remove(row, col, box, cell, digit, bit);
        if (limitReached && !options.isUnlimited()) {
          break;
        }
      }
      swap(emptyPositions, depth, pivotIndex);
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
