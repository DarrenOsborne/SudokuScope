package com.darren.sudokuscope.core;

/** Observer for board mutations. Useful for wiring UI or logging reactions to moves. */
@FunctionalInterface
public interface BoardObserver {
  void onBoardChanged(BoardEvent event);
}
