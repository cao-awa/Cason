```markdown
# Kotlin Gradle Project - Codebase Overview

## File Index
- **LICENSE**: Project license (e.g., MIT/Apache).
- **build.gradle**: Gradle build script (plugins, deps, tasks).
- **gradle/wrapper/gradle-wrapper.jar**: Gradle wrapper executable JAR.
- **gradle/wrapper/gradle-wrapper.properties**: Wrapper config (Gradle version, dist URL).
- **gradle.properties**: JVM/daemon properties (e.g., `kotlin.code.style=official`).
- **gradlew**: Unix Gradle wrapper launcher script.
- **gradlew.bat**: Windows Gradle wrapper launcher script.
- **settings.gradle**: Project settings (single module: `rootProject.name = 'project'`).

## Directory Map
- **gradle/wrapper/**: Reproducible Gradle version files.
- **src/main/kotlin/**: Production Kotlin sources (add `Main.kt` here).
- **src/test/kotlin/**: Kotlin tests (JUnit/KotlinTest).

## Entry Points
- `./gradlew tasks`: List tasks.
- `./gradlew build`: Compile/test/package.
- `./gradlew run`: Run app (needs `kotlin("application")` plugin + `mainClass`).
- Main: `fun main()` in `src/main/kotlin/Main.kt`.

## Key Functions/Classes
- None yet (skeleton project).
- Add app logic/entry: `src/main/kotlin/Main.kt`.
- Add tests: `src/test/kotlin/*Test.kt` (e.g., `MyClassTest`).

## Dependencies
- In `build.gradle`: Kotlin stdlib (`kotlin("jvm")`), Gradle plugin (`kotlin("gradle-plugin")`).
- Common adds: JUnit (`testImplementation("junit:junit")`), Kotest/KotlinTest.
- Update via `./gradlew dependencies`.
```