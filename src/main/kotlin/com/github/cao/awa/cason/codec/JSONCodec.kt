package com.github.cao.awa.cason.codec

import com.github.cao.awa.cason.codec.decoder.JSONDecoder
import com.github.cao.awa.cason.obj.JSONObject

object JSONCodec {
    inline fun <reified T> decode(data: JSONObject): T {
        return JSONDecoder.decode(data)
    }
}