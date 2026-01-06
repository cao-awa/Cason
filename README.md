# Cason

**Cason** is a lightweight, high-performance JSON/JSON5 parsing library for Kotlin, built with Gradle and distributed under the GPLv3 license.

## Features

- **Dual Format Support**: Parse both standard JSON and JSON5 (extended JSON with comments, trailing commas, etc.)
- **Kotlin-Native**: Written in pure Kotlin with idiomatic Kotlin APIs
- **Performance Optimized**: Standard JSON parsing outperforms `org.json` while maintaining readability
- **Gradle Ready**: Seamlessly integrates with Gradle-based Kotlin/Java projects
- **Lightweight**: Minimal dependencies and footprint
- **Type-Safe**: Leverages Kotlin's type system for safer parsing

## Performance

Cason is optimized for real-world usage scenarios:

- **vs org.json**: 1.5x-2x faster for standard JSON parsing
- **vs fastjson2**: Slightly slower for pure throughput but more memory efficient
- **JSON5**: Competitive parsing speed with excellent error recovery

Benchmarks parsing a 15000 elements object, repeat 10000 times to widen the performance gap. For most applications, Cason provides an excellent balance of speed, features, and maintainability.

## Installation
Cason can be installed from GitHub Packages by adding the following repository:
```groovy
repositories {
    maven {
        name = "GitHubPackages"
        url = "https://maven.pkg.github.com/cao-awa/Cason"
        credentials {
            username = "<your-username>"
            password = "<your-github-token>"
        }
    }
}
```

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("com.github.cao.awa:cason:1.0.0")
}
```

### Gradle (Groovy DSL)

```groovy
dependencies {
    implementation 'com.github.cao.awa:cason:1.0.0'
}
```

# Quick Start

## Basic JSON Parsing

```kotlin
import com.github.cao.awa.cason.parser.JSONParser

fun main() {
    val jsonString = """
        {
            "name": "Cason",
            "version": "1.0.0",
            "features": ["json", "json5", "kotlin"]
        }
    """.trimIndent()

    val jsonObject = JSONParser.parseObject(jsonString)
    
    val name = jsonObject.getString("name")
    val version = jsonObject.getString("version")
    val features = jsonObject.getArray("features")
    
    println("Library: $name v$version")
    println("Features: ${features.toList()}")
}
```

## JSON5 Support

```kotlin
import com.github.cao.awa.cason.parser.JSONParser

fun main() {
    val json5String = """
        {
            // JSON5 supports comments!
            name: "Cason",
            version: "1.0.0",
            features: [
                "json",
                "json5", // trailing commas allowed
            ],
            "extra-config": true,
        }
    """.trimIndent()

    val jsonObject = JSONParser.parseObject(json5String)
    
    // Use the parsed data...
}
```

# Serialization

```kotlin
import com.github.cao.awa.cason.obj.JSONObject
import com.github.cao.awa.cason.writer.JSONWriter

fun main() {
    val data = JSONObject {
        put("name", "Cason")
        put("version", "1.0.0")
        array("keywords") {
            add("kotlin")
            add("json")
            add("json5")
            add("parser")
        }
    }
    
    val jsonString = JSONWriter.stringify(data, pretty = true)
    println(jsonString)
}
```

# API Overview

Core Classes

* JSONParser: Main entry point for parsing and serialization
* JSONObject: Type-safe JSON object representation
* JSONArray: Mutable JSON array implementation
* JSONElement: Base class for all JSON elements
* JSONSettings: Configuration for parsing and serializing

## Configuration

```kotlin
JSONSettings.preferSingleQuote = true
JSONSettings.prettyFormat = true
```

# Build
## Building from Source

```bash
# Clone the repository
git clone https://github.com/cao-awa/cason.git
cd cason

# Build the project
./gradlew build
```

## Requirements

* Kotlin 2.2.21+
* Java 21+ JVM
* Gradle 8.14+ (for building)

# Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (git checkout -b feature/amazing-feature)
3. Commit your changes (git commit -m 'Add some amazing feature')
4. Push to the branch (git push origin feature/amazing-feature)
5. Open a Pull Request

 # Support

* [Issue Tracker](https://github.com/cao-awa/Cason/issues)
* [Documentation](https://github.com/cao-awa/Cason/tree/main/document)

