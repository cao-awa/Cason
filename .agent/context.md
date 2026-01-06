```markdown
# Context: Kotlin Gradle Project

## Project Summary
Standard single-module Kotlin JVM project managed by Gradle. Uses conventional directory layout for main sources (`src/main/kotlin`) and tests (`src/test/kotlin`). Suitable as a foundation for libraries, CLI apps, or modules; extend via `build.gradle`.

## Tech Stack
- **Language**: Kotlin (JVM target)
- **Build**: Gradle (Groovy DSL) + Wrapper
- **Sources**: `src/main/kotlin`, `src/test/kotlin`
- **Testing**: JUnit5 / `kotlin.test` (standard)

## Key Files (Read First)
- `build.gradle`: Plugins, deps, tasks (e.g., `kotlin("jvm")`, `application`).
- `settings.gradle`: Root project name, modules.
- `gradle.properties`: Kotlin/ JVM versions, JVM args.
- `gradle/wrapper/gradle-wrapper.properties`: Gradle version.
- `src/main/kotlin`: Entry point (e.g., `Main.kt`).

## Architecture
- **Layout**: Gradle convention (`src/{set}/{lang}`).
- **Components**: Single module; main logic in root package.
- **Entry**: `main` function or Gradle `application` task.
- **No Layers**: Flat; add packages for domain/UI as needed.

## Patterns & Conventions
- **Naming**: CamelCase (classes/funs), snake_case (props/files).
- **Structure**: Packages match dirs (e.g., `com.example.app`).
- **Kotlin**: Data/sealed classes, extension fns, null-safety.
- **Gradle**: `plugins { ... }`, `dependencies { implementation(...) }`.
- **Formatting**: ktlint / spotless (if configured).

## Common Tasks
- **Build/Run**: `./gradlew build` or `./gradlew run`.
- **Clean**: `./gradlew clean`.
- **Add Feature**:
  1. Add `.kt` to `src/main/kotlin`.
  2. Update deps/tasks in `build.gradle`.
  3. `./gradlew build`.
- **Fix Bug**: `./gradlew test`, edit, `./gradlew build`.
- **New Dep**: `dependencies { implementation("group:artifact:1.0") }`; sync.

## Testing
- **Run**: `./gradlew test` (reports in `build/reports/tests`).
- **Write**: Add `.kt` to `src/test/kotlin`; use `@Test`, `assertEquals`.
- **Debug**: `./gradlew test --tests "*Class" --debug-jvm`.
- **Add Coverage**: Apply `jacoco` plugin; `./gradlew jacocoTestReport`.

## Important Notes
- **Wrapper Only**: Use `./gradlew` (Unix) / `gradlew.bat` (Win) for version consistency.
- **Versions**: Pinned in `gradle.properties` (kotlin.version) & wrapper props.
- **No Multiplatform**: JVM-only unless `kotlin("multiplatform")` added.
- **Licensing**: Check `LICENSE` for terms.
- **Gotchas**: Sync IDE after `build.gradle` changes; watch JVM compat.
```

*(~70 lines; optimized for quick AI parsing)*