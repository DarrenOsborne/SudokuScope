import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins { alias(libs.plugins.spotless) }

val libsCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

group = "com.darren.sudokuscope"

version = "0.1.0-SNAPSHOT"

spotless {
  java {
    googleJavaFormat()
    target("**/*.java")
  }
  kotlinGradle {
    target("**/*.gradle.kts")
    ktfmt()
  }
}

subprojects {
  pluginManager.apply("java")

  extensions.configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
    withJavadocJar()
  }

  tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
  }

  val libs = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")

  dependencies {
    add("testImplementation", platform(libs.findLibrary("junit-bom").get()))
    add("testImplementation", libs.findLibrary("junit-jupiter").get())
    add("testImplementation", libs.findLibrary("assertj-core").get())
    add("testImplementation", libs.findLibrary("mockito-core").get())
    add("testRuntimeOnly", libs.findLibrary("junit-platform-launcher").get())
  }

  tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
      events = setOf(TestLogEvent.FAILED)
      showStandardStreams = false
    }
  }
}

tasks.register("checkAll") {
  group = "verification"
  description = "Runs Spotless and all project checks"
  dependsOn(tasks.named("spotlessCheck"))
  dependsOn(subprojects.mapNotNull { it.tasks.findByName("check") })
}
