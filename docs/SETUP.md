# Setup Guide for Kotlin Gradle Project

This document provides step-by-step instructions to set up, build, run, and test the Kotlin Gradle project. The project uses Kotlin and Gradle as the primary tech stack and build system.

## Prerequisites

Before getting started, ensure the following software is installed with the specified minimum versions:

| Software       | Minimum Version | Notes |
|----------------|-----------------|-------|
| JDK            | 17              | OpenJDK or Oracle JDK recommended. Verify with `java -version`. |
| Gradle         | 8.5             | The project includes a Gradle Wrapper (`gradlew`), so manual installation is optional. Verify with `./gradlew --version`. |
| Git            | 2.30            | Required for cloning the repository. |

No additional runtime dependencies (e.g., databases) are required for this project.

## Quick Start

Get the project running in under 5 minutes:

1. Clone the repository:  
   ```
   git clone <repository-url>
   cd kotlin-gradle-project
   ```

2. Build the project:  
   ```
   ./gradlew build
   ```

3. Run the application:  
   ```
   ./gradlew run
   ```

The application will start and output logs to the console.

## Detailed Installation

### Clone Repository
```
git clone <repository-url>
cd kotlin-gradle-project
```

### Install Dependencies
The Gradle Wrapper (`gradlew` on Unix/Mac or `gradlew.bat` on Windows) automatically downloads and manages dependencies. No manual installation is needed.

Run `./gradlew build` to fetch all dependencies and compile the project.

### Environment Setup
This project does not use environment variables or a `.env` file. Configuration is handled via `build.gradle.kts` and `gradle.properties`.

### Database Setup
Not applicable. This project does not require a database.

## Running the Application

### Development Mode
Use the Gradle task for hot-reloading and development:
```
./gradlew run
```
The application runs on the default JVM process and outputs to the console. Stop with `Ctrl+C`.

### Production Mode
1. Build the executable JAR:  
   ```
   ./gradlew build
   ```
   The JAR is generated at `build/libs/kotlin-gradle-project-<version>.jar`.

2. Run the JAR:  
   ```
   java -jar build/libs/kotlin-gradle-project-<version>.jar
   ```

### With Docker
Not applicable. No Docker configuration is present in the project.

## Running Tests

The project uses Kotlin test framework (via `kotlin("test")` plugin). Run the full test suite:

```
./gradlew test
```

- View test reports: Open `build/reports/tests/test/index.html` in a browser.
- Run specific tests: `./gradlew test --tests "com.example.MyTest"`.
- Continuous testing: `./gradlew test --continuous` (watches for changes).

All tests must pass for the build to succeed.

## Troubleshooting

| Issue | Possible Cause | Solution |
|-------|----------------|----------|
| `Gradle version mismatch` | Incompatible Gradle/JDK | Update JDK to 17+ and use `./gradlew wrapper --gradle-version=8.5`. |
| `Could not resolve dependencies` | Network/proxy issues | Run `./gradlew build --refresh-dependencies` or configure proxy in `gradle.properties`. |
| `Kotlin compiler errors` | Syntax issues or version mismatch | Ensure Kotlin version in `build.gradle.kts` matches plugin (e.g., 1.9.20). Run `./gradlew clean build`. |
| `Daemon out of memory` | Low heap size | Add to `gradle.properties`: `org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=512m`. |
| `Permission denied on gradlew` (Unix/Mac) | File permissions | Run `chmod +x gradlew`. |
| Tests fail intermittently | JVM forking issues | Add `--no-daemon` flag: `./gradlew test --no-daemon`. |

If issues persist, check `build/gradle.log` or run with `--stacktrace --info`.

## IDE Setup

### IntelliJ IDEA (Recommended)
1. Open the project directory in IntelliJ.
2. Gradle plugin auto-imports `build.gradle.kts`.
3. **Recommended Plugins**:
   - Kotlin (bundled)
   - Gradle (bundled)

### Visual Studio Code
1. Install extensions:
   - **Kotlin** (by fwcd)
   - **Extension Pack for Java** (includes Gradle support)
   - **Gradle for Java** (by vscjava)
2. Open the project folder and reload.

Import the Gradle project in your IDE for full syntax highlighting, auto-completion, and task running.