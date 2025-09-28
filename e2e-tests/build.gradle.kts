dependencies {
  testImplementation(project(":ui"))
  testImplementation(project(":core"))
}

tasks.withType<Test>().configureEach { systemProperty("javafx.headless", "true") }
