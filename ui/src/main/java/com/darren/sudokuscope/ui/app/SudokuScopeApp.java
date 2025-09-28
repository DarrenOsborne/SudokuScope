package com.darren.sudokuscope.ui.app;

import com.darren.sudokuscope.ui.view.BoardView;
import com.darren.sudokuscope.ui.viewmodel.BoardViewModel;
import java.util.Objects;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SudokuScopeApp extends Application {
  @Override
  public void start(Stage primaryStage) {
    BoardViewModel viewModel = new BoardViewModel();
    BoardView boardView = new BoardView(viewModel);

    Scene scene = new Scene(boardView.getRoot());
    scene
        .getStylesheets()
        .add(Objects.requireNonNull(getClass().getResource("/styles/main.css")).toExternalForm());

    primaryStage.setTitle("SudokuScope");
    primaryStage.setScene(scene);
    primaryStage.setResizable(false);
    primaryStage.show();

    primaryStage.setOnCloseRequest(event -> viewModel.shutdown());
  }

  public static void main(String[] args) {
    launch(args);
  }
}
