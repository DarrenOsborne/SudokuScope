package com.darren.sudokuscope.ui.app;

import com.darren.sudokuscope.ui.view.BoardView;
import com.darren.sudokuscope.ui.view.TargetCountView;
import com.darren.sudokuscope.ui.viewmodel.BoardViewModel;
import com.darren.sudokuscope.ui.viewmodel.TargetCountViewModel;
import java.util.Objects;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

public class SudokuScopeApp extends Application {
  @Override
  public void start(Stage primaryStage) {
    BoardViewModel solveViewModel = new BoardViewModel();
    BoardView boardView = new BoardView(solveViewModel);

    TargetCountViewModel targetViewModel = new TargetCountViewModel();
    TargetCountView targetView = new TargetCountView(targetViewModel);

    Tab solveTab = new Tab("Solve", boardView.getRoot());
    solveTab.setClosable(false);
    Tab targetTab = new Tab("Target Count", targetView.getRoot());
    targetTab.setClosable(false);

    TabPane tabs = new TabPane(solveTab, targetTab);

    Scene scene = new Scene(tabs);
    scene
        .getStylesheets()
        .add(Objects.requireNonNull(getClass().getResource("/styles/main.css")).toExternalForm());

    primaryStage.setTitle("SudokuScope");
    primaryStage.setScene(scene);
    primaryStage.setResizable(false);
    primaryStage.show();

    primaryStage.setOnCloseRequest(
        event -> {
          solveViewModel.shutdown();
          targetViewModel.shutdown();
        });
  }

  public static void main(String[] args) {
    launch(args);
  }
}
