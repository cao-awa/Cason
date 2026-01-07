import com.github.cao.awa.cason.obj.JSONObject
import com.github.cao.awa.cason.serialize.parser.JSONParser
import com.github.cao.awa.cason.serialize.JSONSerializeVersion
import com.github.cao.awa.cason.setting.JSONSettings
import com.github.cao.awa.cason.serialize.writer.JSONWriter

fun main() {
    parsing()

    dsl()
}

fun dsl() {
    JSONSettings.preferSingleQuote = true
    JSONSettings.serializeVersion = JSONSerializeVersion.JSON5
    val json = JSONObject {
        "name" set "Cason"
        "version" set "1.0.0"
        array("keywords") {
            +"kotlin"
            +"json"
            +"json5"
            +"parser"
        }
        json("awa") {
            "test-awa" set "awa"
            "test-number" set 1234567
        }
        computeInt("test-compute") {
            123 + (it ?:0)
        }
    }

    println(JSONWriter.stringify(json, pretty = true).also {
        println((JSONParser.parse(it) as JSONObject).getString("name"))
    })
}

fun parsing() {
    val data = """
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
    """.trimIndent()

    val testCount = 100000

    benchmark(testCount, "Cason") {
        JSONParser.parse(data)
    }

    benchmark(testCount, "fastjson") {
        com.alibaba.fastjson2.JSON.parseObject(data)
    }

    benchmark(testCount, "org.json") {
        org.json.JSONObject(data)
    }
}

fun benchmark(count: Int, sampleName: String, action: () -> Unit) {
    // Pre test.
    for (i in 0 until 10000) {
        action()
    }

    val start = System.currentTimeMillis()
    for (i in 0 until count) {
        action()
    }
    println("Sample $sampleName done $count times test in ${System.currentTimeMillis() - start} ms")
}