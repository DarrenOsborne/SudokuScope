# SudokuScope

SudokuScope is a multi-module Gradle workspace for exploring, analysing, and eventually publishing Sudoku boards. It provides a fast core solver, a JavaFX desktop client, a Spring Boot REST API, JMH benchmarks, and an end-to-end test harness that demonstrate a production-friendly architecture ready for future expansion (including a JS-hosted frontend).

## Modules

| Module | Description |
| --- | --- |
| core | Pure Java domain + solver engine with backtracking, command history, validators, and asynchronous solver service. |
| ui | JavaFX MVVM client that renders the board, debounces inputs, and streams live solution counts using the core module. |
| web | Spring Boot API exposing the solver for remote or browser usage (POST /api/analyze). |
| ench | JMH micro-benchmarks for profiling solver strategies. |
| e2e-tests | High-level tests wiring the solver service as an end-to-end sanity check. |

## Getting Started

### Prerequisites

* JDK 21+
* Gradle Wrapper (already included) or Gradle 8.12.1+

### Build & Verify

`ash
./gradlew check
`

### Run the JavaFX desktop client

`ash
./gradlew :ui:run
`

The client starts with an empty board, shows the known number of valid completions for the blank grid, and continuously recomputes counts after each edit. Invalid boards surface validation feedback immediately.

### Run the REST API

`ash
./gradlew :web:bootRun
`

Then POST an 81-length integer array to http://localhost:8080/api/analyze:

`json
{
  "cells": [5,3,0,0,7,0,0,0,0,6,0,0,1,9,5,0,0,0,0,9,8,0,0,0,0,6,0,8,0,0,0,6,0,0,0,3,4,0,0,8,0,3,0,0,1,7,0,0,0,2,0,0,0,6,0,0,0,4,1,9,0,0,5,0,0,0,0,8,0,0,7,9]
}
`

### Run benchmarks

`ash
./gradlew :bench:jmh
`

## Architecture Highlights

* **Solver strategy** – core uses a bit-mask driven backtracking engine with MRV heuristics. It counts solutions up to configurable limits and shortcuts the empty-board case using the known Sudoku constant (6.670903752021072936960e21).
* **Command + Undo** – GameState executes BoardCommand instances (e.g. SetValueCommand) to support undo/redo and event observation.
* **MVVM UI** – BoardViewModel exposes observable properties for the JavaFX view, debounces edits via PauseTransition, and offloads solving to a single-threaded SolverService with cancellation.
* **Ports & adapters** – core remains framework-free; ui and web depend on it but not vice versa.
* **Async service** – SolverService wraps the solver with an executor and CompletableFuture pipeline for reuse across modules.

## Testing Strategy

* Unit tests in core exercise board validation, undo stack mechanics, and solver correctness.
* web module includes MockMvc tests for the REST surface.
* e2e-tests module drives the asynchronous solver end-to-end.
* Spotless enforces formatting across all modules.

## Next Steps

1. Persist board states and user sessions (e.g. via database) to support multi-device play.
2. Expose streaming updates from the solver to the UI/web (Server-Sent Events or WebSocket).
3. Build the planned JS frontend on top of the REST API and host it publicly.
4. Expand solver strategies (e.g. DLX/Exact Cover) and compare via the benchmark suite.
5. Add richer invalid-state diagnostics to highlight specific conflicting cells.

## License

This project is released under the MIT License. See [LICENSE](LICENSE).
