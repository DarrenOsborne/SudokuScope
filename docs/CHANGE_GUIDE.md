# Change Guide

This guide is a quick map of where to make changes and how to verify them.

## Core changes

* Solver behavior: `core/src/main/java/com/darren/sudokuscope/core/solver/BacktrackingSudokuSolver.java`
* Solver options and timeouts: `core/src/main/java/com/darren/sudokuscope/core/solver/SolverOptions.java`
* Target count search and base solution generation: `core/src/main/java/com/darren/sudokuscope/core/solver/TargetPuzzleSearch.java`

## UI changes

* Solve tab layout: `ui/src/main/java/com/darren/sudokuscope/ui/view/BoardView.java`
* Target Count tab layout: `ui/src/main/java/com/darren/sudokuscope/ui/view/TargetCountView.java`
* UI logic (Solve): `ui/src/main/java/com/darren/sudokuscope/ui/viewmodel/BoardViewModel.java`
* UI logic (Target Count): `ui/src/main/java/com/darren/sudokuscope/ui/viewmodel/TargetCountViewModel.java`
* Global styles: `ui/src/main/resources/styles/main.css`

## Target Count tuning

* Base solution generation logic: `TargetPuzzleSearch.generateRandomSolved`
* Pruning loop: `TargetPuzzleSearch.findClosestFromSolved`
* Solver limits and timeouts: `TargetCountViewModel` constants and `SolverOptions`

## Build and verify

```bash
./gradlew check
```

UI only:

```bash
./gradlew :ui:run
```

Benchmarks:

```bash
./gradlew :bench:jmh
```
