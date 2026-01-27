# Cason

**Cason** is a lightweight, high-performance, type safe JSON/JSON5 parsing library for Kotlin with expressive DSL.

![](https://count.getloli.com/@@cao-awa.cason?name=%40cao-awa.cason&padding=7&offset=0&align=top&scale=1&pixelated=1&darkmode=auto)

## Features

- **Dual Format Support**: Parse both standard JSON and JSON5 (extended JSON with comments, trailing commas, etc.)
- **Kotlin-Native**: Written in pure Kotlin with idiomatic Kotlin APIs
- **Performance Optimized**: Standard JSON parsing outperforms `org.json` while maintaining readability
- **Gradle Ready**: Seamlessly integrates with Gradle-based Kotlin/Java projects
- **Lightweight**: Minimal dependencies and footprint
- **Type-Safe**: Leverages Kotlin's type system for safer parsing

## Performance

Cason parser based on implicit state machine, optimized for real-world usage scenarios:

- **vs org.json**: 5x-6x faster for standard JSON parsing
- **vs fastjson2**: Slightly slower for pure throughput but more memory efficient
- **JSON5**: Competitive parsing speed with excellent error recovery

<details>

<summary>Benchmark case</summary>

### Test data (small strict JSON)

```json
{
  "schema_style": "conium",
  "identifier": "conium:sample",
  "templates": {
    "armor": {
      "slot": "helmet",
      "defense": 2,
      "toughness": 2,
      "knockback_resistance": 2,
      "enchantable": 2
    },
    "netherite_axe": {},
    "can_destroy_in_creative": true,
    "clear_ignite": true,
    "consumable": {
      "convert_to": "minecraft:apple",
      "apply_effect": "minecraft:regeneration"
    },
    "consume_on_used": {
      "used_on_block": "minecraft:bedrock",
      "used_on_entity": "minecraft:pig",
      "consume": true
    },
    "entity_placer": {
      "entity": "minecraft:pig",
      "allowed_block": "minecraft:bedrock"
    },
    "food": {
      "nutrition": 10,
      "saturation": 10,
      "can_always_eat": true
    },
    "force_mining_speed": 8,
    "fuel": 100,
    "glint": true,
    "ignite": true,
    "rarity": "epic",
    "spawn_egg": "minecraft:creeper",
    "max_count": 64,
    "use_action": "eat"
  }
}
```

### Test result (small strict JSON)

Small object benchmarks parsing a small elements object, repeat 10000 times to widen the performance gap. For most
applications,
Cason provides an excellent balance of speed, features, and maintainability.

* **Cason**: 65359 parses/s
* **Cason (Strict mode)**: 74627 parses/s
* **fastjson2**: 83333 parses/s
* **org.json**: 12610 parses/s

### Test data (big JSON5)

```json5
[
  {
    "id": 0,
    "name": "User_0",
    "bio": "This is a random bio with some // comments inside strings",
    // This is a line comment for item 0
    "values": [
      0.12843502127637108,
      70,
      0,
      /* Trailing comma test */
    ],
    "meta": {
      "active": true,
      "score": 11.066215791114075
      /* Block 
                 Comment */
    },
  },
  // ...... Repeat the data like that.
]
```

### Test result (big JSON5)

Big object benchmarks parsing a big elements array, repeat 248000 elements in array to widen the performance gap.

* **Cason**: 3 parses/s
* **Cason (Strict mode)**: Not parsable
* **fastjson2**: 2.56 parses/s
* **org.json**: Not parsable

</details>

## Installation

Cason can be installed from JitPack by adding the following repository:

```groovy
repositories {
    maven {
        url 'https://jitpack.io'
    }
}
```

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("com.github.cao.awa:cason:{CASON_VERSION}")
}
```

### Gradle (Groovy DSL)

```groovy
dependencies {
    implementation 'com.github.cao.awa:cason:{CASON_VERSION}'
}
```

# Quick Start

## Basic JSON Parsing

```kotlin
import com.github.cao.awa.cason.serialize.parser.JSONParser

fun main() {
    val jsonString = """
        {
            "name": "Cason",
            "version": "1.0.0",
            "features": ["json", "json5", "kotlin"]
        }
    """

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
import com.github.cao.awa.cason.serialize.parser.JSONParser

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

## Serialization

```kotlin
import com.github.cao.awa.cason.obj.JSONObject
import com.github.cao.awa.cason.serialize.writer.JSONWriter

fun main() {
    val data = JSONObject {
        "name" set "Cason"
        "version" set "1.0.0"
        array("keywords") {
            +"kotlin"
            +"json"
            +"json5"
            +"parser"
        }
    }

    val jsonString = JSONWriter.stringify(data, pretty = true)
    println(jsonString)
}
```

## Class construction

```kotlin
import com.github.cao.awa.cason.obj.JSONObject
import com.github.cao.awa.cason.codec.JSONCodec

fun main() {
    val json = JSONObject {
        "value" set "awa"
        "test_id" set 1234

        json("inner_data") {
            "inner_string" set "inner-awa"
        }
    }

    println(
        JSONCodec.decode<Struct>(
            json
        ).also {
            println(JSONCodec.encodeAny(it))
        }
    )
}

data class Struct(
    val value: String,
    @Field("test_id")
    val testId: Int,
    @Nested
    @Field("inner_data")
    val innerData: TestInnerData
)

data class TestInnerData(
    @Field("inner_string")
    val innerString: String
)
```

The serialization test will get these output:

```json
{
  "inner_data": {
    "inner_string": "inner-awa"
  },
  "test_id": 1234,
  "value": "awa"
}
```

```kotlin
Empty(value = awa, testId = 1234, innerData = TestInnerData(innerString = inner - awa))
```

## Flatten

```kotlin
import com.github.cao.awa.cason.obj.JSONObject
import com.github.cao.awa.cason.codec.JSONCodec

fun main() {
    val json: JSONObject = JSONCodec.encode<Struct>(
        Struct(
            "value-awa",
            123,
            TestNested(
                "value-qaq",
                456
            )
        )
    )

    println(
        JSONCodec.decode<Struct>(
            json
        ).also {
            println(JSONCodec.encodeAny(it))
        }
    )
}

data class Struct(
    val value: String,
    @Field("test_id")
    val testId: Int,
    @Flattened
    val innerData: TestNested
)

data class TestNested(
    @Field("inner_string")
    val innerString: String,
    @Field("inner_int")
    val innerInt: Int
)
```

The flattened test will get these output:

```json
{
  "inner_int": 456,
  "inner_string": "value-qaq",
  "test_id": 123,
  "value": "value-awa"
}
```

```kotlin
Struct(value = value - awa, testId = 123, innerData = TestNested(innerString = value - qaq, innerInt = 456))
```

## Notice

It's best not to get the data that just recent set to in DSL:

```kotlin
import com.github.cao.awa.cason.obj.JSONObject

fun test(data: JSONObject) {
    data.instruct {
        "key" set "value"

        // Value will not be null but data stream will be completed,
        // validate or other intermediate operations will complete here.
        getString("key")
    }
}
```

Using ```instruct``` method instead of ```apply``` to into the DSL phase:

```kotlin
import com.github.cao.awa.cason.obj.JSONObject

fun test(data: JSONObject) {
    data.instruct {
        "key" set "value"
    }

    // Don't do this, otherwise calls 'completePending' manually.
    data.apply {
        "key" set "value"
    }.completePending()
}
```

Using ```nested``` to put a data class into JSONObject and get with ```getNested```:

```kotlin
fun test() {
    JSONObject {
        "test_value" set "Test"
        "test_id" set 1234

        // Use 'nested' to auto encode to JSONObject.
        "test_nested" nested TestNested("Awa")
    }.also { data: JSONObject ->
        // Use 'getNested' to auto decode from JSONObject.
        val nested = data.getNested<TestNested>("test_nested")
        println(nested)
    }
}

// Never put element that not data class into a JSONObject!
data class TestNested(val name: String)
```

The nested is a data, DSL body cannot insert other element into it:

```kotlin
fun test() {
    JSONObject {
        // Use 'nested' to auto encode to JSONObject.
        nested<TestNested>("test_nested") {
            // Nested object should always const, no dynamic element insertions.
            // So this is an unexpected behaviors, cannot put other element into nested body.
            // This set call will insert "value" to the parent JSONObject instead of nested object.
            "key" set "value"

            // Getter style to construct the nested object is 
            // design to something intermediate operations like fetch to databases.
            // Not for constructing a dynamic object.
            TestNested("QwQ")
        }
    }
}

data class TestNested(val name: String)
```

好，这里我给你一段**可以直接放进 README 的「Why Cason?」**，语气偏工程理性、不是营销吹捧，也和你现在整篇文档风格是统一的。
（如果你想更偏宣传 / 更偏技术白皮书 / 更偏 Kotlin 社区口吻，我也可以再拆版本。）

---

## Why Cason?

Most JSON libraries focus on **parsing correctness** or **raw throughput**.
Cason focuses on **real-world usability**.

In practice, JSON data is often **written and maintained by humans**, not just generated by machines.
Configuration files, mod data, scripts, and DSLs frequently rely on comments, trailing commas, relaxed syntax, and incremental edits — features that strict JSON parsers simply do not handle well.

Cason is designed specifically for these scenarios.

### Designed for JSON *and* JSON5

Cason natively supports both **standard JSON** and **JSON5** in a single, unified parser.
JSON5 is treated as a first-class format rather than an optional extension, making Cason suitable for configuration-heavy and developer-authored data.
For projects that require strict compliance, a dedicated strict mode is also provided.

### Kotlin-first, not Java-centric

Cason is written in pure Kotlin and designed around **idiomatic Kotlin APIs**:

* Type-safe JSON objects and arrays
* Expressive Kotlin DSL for building JSON structures
* Minimal reflection and clear annotation semantics

Rather than adapting a Java-style API to Kotlin, Cason embraces Kotlin’s language features directly.

### Balanced performance for real projects

Cason’s parser is built on an optimized implicit state machine and delivers:

* Significantly better performance than `org.json`
* Competitive throughput compared to high-performance parsers
* Lower memory pressure in practical usage

The goal is not extreme micro-benchmark dominance, but **predictable, production-ready performance**.

### Strong type safety with flexible data flow

Cason provides a complete data pipeline:

* Parse JSON / JSON5 into mutable, type-safe structures
* Decode JSON into Kotlin data classes
* Encode Kotlin objects back into JSON
* Support for nested, flattened, and field-mapped structures

This makes Cason suitable not only for parsing, but also for **data transformation, intermediate processing, and configuration systems**.

### Explicit design boundaries

Cason intentionally defines clear behavioral boundaries for its DSL and data model.\
Unsafe or ambiguous operations are documented and constrained to prevent subtle runtime errors, helping developers avoid hard-to-debug data inconsistencies.

**In short:** Cason is built for developers who want a **lightweight, Kotlin-native, JSON/JSON5 parser** that works well in real-world projects — without sacrificing performance, safety, or clarity.

# API Overview

Core Classes

* JSONParser: Main entry point for JSON parsing
* JSONWriter: Main entry point for JSON serialization
* JSONObject: Type-safe JSON object representation
* JSONArray: Mutable JSON array implementation
* JSONElement: Base class for all JSON elements
* JSONSettings: Configuration for parsing and serializing
* JSONCodec: Decode a JSON data to a refiled object or Encode a refiled object to a JSON data
* JSONDecoder: Decode a JSON data to a refiled object
* JSONEncoder: Encode a refiled object to a JSON data

## Configuration

```kotlin
fun main() {
    JSONSettings.preferSingleQuote = true
    JSONSettings.prettyFormat = true
}
```

## More samples

See [Test codec](/src/test/kotlin/Main.kt).

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

