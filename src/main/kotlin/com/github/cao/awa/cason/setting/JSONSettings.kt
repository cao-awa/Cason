package com.github.cao.awa.cason.setting

import com.github.cao.awa.cason.serialize.JSONSerializeVersion

object JSONSettings {
    var prettyFormat: Boolean = false
    var preferSingleQuote: Boolean = false
    var serializeVersion: JSONSerializeVersion = JSONSerializeVersion.JSON
}