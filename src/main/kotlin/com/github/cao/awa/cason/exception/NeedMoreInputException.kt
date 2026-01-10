package com.github.cao.awa.cason.exception

import com.github.cao.awa.cason.serialize.parser.JSONParser

class NeedMoreInputException(val parser: JSONParser) : RuntimeException()