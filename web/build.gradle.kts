plugins {
  alias(libs.plugins.springboot)
  alias(libs.plugins.springdependency)
}

dependencies {
  implementation(project(":core"))
  implementation(libs.spring.boot.starter.web)
  implementation(libs.spring.boot.starter.actuator)
  implementation(libs.jackson.databind)
  testImplementation(libs.spring.boot.starter.test)
}

springBoot { mainClass.set("com.darren.sudokuscope.web.SudokuScopeWebApplication") }
