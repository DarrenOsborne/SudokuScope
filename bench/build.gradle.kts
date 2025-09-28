plugins { alias(libs.plugins.jmh) }

dependencies {
  implementation(project(":core"))
  jmh(project(":core"))
  jmh(libs.slf4j.api)
  jmh(libs.jmh.core)
  jmhAnnotationProcessor(libs.jmh.generator.annprocess)
}
