package com.darren.sudokuscope.core;

import com.darren.sudokuscope.core.command.BoardCommand;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/** Mutable game state that tracks board history and allows undo/redo operations. */
public final class GameState {
  private SudokuBoard board;
  private final Deque<BoardCommand> undoStack = new ArrayDeque<>();
  private final Deque<BoardCommand> redoStack = new ArrayDeque<>();
  private final List<BoardObserver> observers = new CopyOnWriteArrayList<>();

  public GameState() {
    this(SudokuBoard.empty());
  }

  public GameState(SudokuBoard initialBoard) {
    this.board = Objects.requireNonNull(initialBoard, "initialBoard");
  }

  public SudokuBoard board() {
    return board;
  }

  public void addObserver(BoardObserver observer) {
    observers.add(Objects.requireNonNull(observer, "observer"));
  }

  public void removeObserver(BoardObserver observer) {
    observers.remove(observer);
  }

  public BoardEvent apply(BoardCommand command) {
    Objects.requireNonNull(command, "command");
    BoardCommand.MutationResult result = command.apply(board);
    board = result.newBoard();
    undoStack.push(result.undoCommand());
    redoStack.clear();
    notifyObservers(result.event());
    return result.event();
  }

  public boolean canUndo() {
    return !undoStack.isEmpty();
  }

  public boolean canRedo() {
    return !redoStack.isEmpty();
  }

  public BoardEvent undo() {
    if (!canUndo()) {
      return null;
    }
    BoardCommand command = undoStack.pop();
    BoardCommand.MutationResult result = command.apply(board);
    board = result.newBoard();
    redoStack.push(result.undoCommand());
    notifyObservers(result.event());
    return result.event();
  }

  public BoardEvent redo() {
    if (!canRedo()) {
      return null;
    }
    BoardCommand command = redoStack.pop();
    BoardCommand.MutationResult result = command.apply(board);
    board = result.newBoard();
    undoStack.push(result.undoCommand());
    notifyObservers(result.event());
    return result.event();
  }

  private void notifyObservers(BoardEvent event) {
    if (event == null || event.isNoOp()) {
      return;
    }
    for (BoardObserver observer : observers) {
      observer.onBoardChanged(event);
    }
  }
}
