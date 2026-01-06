fun main() {
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
        com.github.cao.awa.caoson.parser.JSONParser.parse(data)
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
    for (i in 0 until 5000) {
        action()
    }

    val start = System.currentTimeMillis()
    for (i in 0 until count) {
        action()
    }
    println("Sample $sampleName done $count times test in ${System.currentTimeMillis() - start} ms")
}