package com.github.cao.awa.caoson.setting

import com.github.cao.awa.caoson.serialize.JSONSerializeVersion

object JSONSettings {
    var prettyFormat: Boolean = false
    var preferSingleQuote: Boolean = false
    var serializeVersion: JSONSerializeVersion = JSONSerializeVersion.JSON
}