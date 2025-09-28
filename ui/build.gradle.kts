plugins {
  id("application")
  alias(libs.plugins.javafx)
}

dependencies {
  implementation(project(":core"))
  implementation(libs.slf4j.api)
  implementation(libs.javafx.controls)
  implementation(libs.javafx.fxml)
  runtimeOnly(libs.logback.classic)
}

application { mainClass.set("com.darren.sudokuscope.ui.app.SudokuScopeApp") }

javafx {
  version = libs.versions.javafx.get()
  modules("javafx.controls", "javafx.fxml")
}
